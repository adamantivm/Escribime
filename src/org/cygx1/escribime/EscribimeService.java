package org.cygx1.escribime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.http.AndroidHttpClient;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

public class EscribimeService extends Service {
	private static final int NOTIFICATION_ID = 120198237;
	
	static SharedPreferences prefs;
    static String userid;
    static String password;
    static String label;
    static int updateInterval;
    static boolean vibrate;
    
    static boolean running = false;

    final AndroidHttpClient http = AndroidHttpClient.newInstance("CygX1 browser");
    private Timer timer = new Timer();
    
    static Activity activity;
    
    Notification notification;
    NotificationManager mNotificationManager;
    
    int lastUnread = 0;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	public static void initialize(Activity a) {
		activity = a;
	}
	
   protected void loadPreferences() {
       userid = prefs.getString(getString(R.string.emailPref), null);
       password = prefs.getString(getString(R.string.passwordPref), null);
       label = prefs.getString(getString(R.string.labelPref), null);
       updateInterval = Integer.parseInt(prefs.getString(getString(R.string.intervalPref), null));
       vibrate = prefs.getBoolean(getString(R.string.vibratePref), false);
   }

   private void updateNotification(int unread, int status) {
	    int icon;
	    switch (status) {
	    case 1:
	    	icon = R.drawable.happyness1;
	    	break;
	    case 2:
	    	icon = R.drawable.happyness2;
	    	break;
	    case 3:
	    	icon = R.drawable.happyness3;
	    	break;
	    case 4:
	    	icon = R.drawable.happyness4;
	    	break;
	    case 5:
	    	icon = R.drawable.happyness5;
	    	break;
	    case 6:
	    	icon = R.drawable.happyness6;
	    	break;
	    case 7:
	    	icon = R.drawable.happyness7;
	    	break;
	    default:
	    	icon = R.drawable.happyness;
	    }
		CharSequence tickerText = "unread = " + unread;
		long when = System.currentTimeMillis();
		notification = new Notification(icon, tickerText, when);
		// TL: only show light (and/or vibrate) if there are unread messages
		if (unread > 0) {
			notification.flags |= Notification.FLAG_SHOW_LIGHTS;
			notification.ledARGB = 0xFFFF0000;
			notification.ledOnMS = 300;
			notification.ledOffMS = 1000;
			
			if (vibrate) {
				long[] pattern = {0, 150, 250, 250, 250, 150};
				notification.vibrate = pattern;
			}
		}
		
		Context context = getApplicationContext();
		CharSequence contentTitle = "Escribime";
		CharSequence contentText = "unread = " + unread;
		Intent notificationIntent = new Intent( context, Escribime.class);

		PendingIntent contentIntent = PendingIntent.getActivity( context, 0, notificationIntent, 0);
		notification.setLatestEventInfo( context, contentTitle, contentText, contentIntent);
		mNotificationManager.notify( NOTIFICATION_ID, notification);
	}
	
	private void clearNotification() {
		mNotificationManager.cancel( NOTIFICATION_ID);
	}

	@Override
	public void onCreate() {
        System.setProperty("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver");
        
        String ns = Context.NOTIFICATION_SERVICE;
		mNotificationManager = (NotificationManager) getSystemService(ns);

		// Save a pointer to our preferences for use later
	    prefs = PreferenceManager.getDefaultSharedPreferences(this);
		loadPreferences();
	
		Log.d(EscribimeService.class.getCanonicalName(), "Service started. Using updateInterval = " + updateInterval);
		timer.scheduleAtFixedRate( new TimerTask() {
			public void run() {
				int status = getStatus();
				final int unread = EscribimeService.this.fetchUnread( userid, password, label);
				Log.d(EscribimeService.class.getCanonicalName(), "Got unread = " + unread);
				if( unread > 0) {
					status += 4;
				}
				if (status > 0) {
					// TL: need better logic here to determine when to update the notification
					// It's more complicated because changes to the lime shouldn't trigger a vibrate
					if (unread > lastUnread) {
						updateNotification(unread, status);
					}
				} else {
					clearNotification();
				}
				lastUnread = unread;				
			}
		}, 0, updateInterval * 1000);
		Log.d("EscribimeService","Monotoring label " + label + " every " + updateInterval + " seconds");
		
		EscribimeService.running = true;
	}
	
	private int fetchUnread(String userid, String password, String label) {
		int ret = -1;
		synchronized (this) {
			try {
			    HttpUriRequest request = new HttpGet("https://mail.google.com/mail/feed/atom/"+ label);
		        request.addHeader("Authorization", "Basic " + new String(Base64.encode((userid + ":" + password).getBytes(),Base64.NO_WRAP)));
				HttpResponse response = http.execute(request);
				InputStream is = response.getEntity().getContent();
				
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(new InputSource(is));
				doc.getDocumentElement().normalize();
				NodeList nodeList = doc.getElementsByTagName("fullcount");
				Node node = nodeList.item(0);
				ret = Integer.parseInt(node.getTextContent());

				is.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ret;
	}
	
	// -------------------------------------------------------------------
	// For online status
	
	/**
	 * Get our statuses from the online server
	 * @return 0 if neither is online
	 *         1 if I am online
	 *         2 if you are online
	 *         3 if we are both online
	 */
	int getStatus() {
		// TL: can I assume that userid has already been set at this point, or do I
		// need to retrieve it from settings each time?
        SharedPreferences settings = getSharedPreferences(Escribime.PREFS_NAME, 0);
        userid = settings.getString("userid", "");
	    HttpUriRequest request = new HttpGet("http://ofb.net:3300/status/q");
	    Log.d("Escribime", "Fetching status...");
	    
	    boolean my_avail = false, you_avail = false;
	    try {
	    	HttpResponse response = http.execute(request);
			InputStream is = response.getEntity().getContent();
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(is));
			doc.getDocumentElement().normalize();
			NodeList nodeList = doc.getElementsByTagName("record");
			for (int i=0; i<nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				NodeList children = node.getChildNodes();
				
				boolean availability = false;
				String email = null;
				
				for (int j=0; j<children.getLength(); j++) {
					Node child = children.item(j);
					if (child.getNodeName().toLowerCase().equals("available")) {
						availability = (child.getTextContent().equals("true"));
					} else if (child.getNodeName().toLowerCase().equals("email")) {
						email = child.getTextContent();
					}
				}
				
				// Got one status field
				if (email != null) {
					if (userid.equals(email)) {
						// it's me
						my_avail = availability;
					} else {
						// it's someone else; I sure hope it's you
						you_avail = availability;
					}
				}
			}

			is.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.d("Escribime", "Error updating status:" + e);
			return 0;
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (!my_avail && !you_avail) return 0;
		else if (!my_avail && you_avail) return 1;
		else if (my_avail && !you_avail) return 2;
		else if (my_avail && you_avail) return 3;
		else return -1;
	}
	
	// ----------------------------------------------------------------

	@Override
	public void onDestroy() {
		if (timer != null) timer.cancel();
		clearNotification();
		//	Block here until any ongoing http request is finished
		synchronized (this) {}
		try {
			http.close();
		} catch( RuntimeException e) {
			Log.e( EscribimeService.class.getCanonicalName(), "Exception trying to close HttpClient: " + e);
		}
	}
}
