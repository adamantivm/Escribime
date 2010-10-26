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
    
    static Escribime activity;
    
    static Notification notification;
    NotificationManager mNotificationManager;
    
    int lastUnread = 0;
	int lastStatus = 0;
	
	//	ORable flags for the status
	static final int STATUS_YOU = 1;	//	You are online
	static final int STATUS_ME = 2;		//	I am online

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	public static void initialize(Escribime a) {
		activity = a;
	}
	
   protected void loadPreferences() {
       userid = prefs.getString(getString(R.string.emailPref), null);
       password = prefs.getString(getString(R.string.passwordPref), null);
       label = prefs.getString(getString(R.string.labelPref), null);
       updateInterval = Integer.parseInt(prefs.getString(getString(R.string.intervalPref), null));
       vibrate = prefs.getBoolean(getString(R.string.vibratePref), false);
   }

   /**
    * 
    * @param unread - how many unread messages there are
    * @param stautsOnline - YOU and ME online status
    * @param iconStatus - which icon to draw (1-7)
    * @param activeNotify - set to true when something new happened on this notification
    *        (e.g. we should vibrate)
    */
   private void updateNotification(int unread, int statusOnline, int iconStatus, boolean activeNotify) {
	    int icon;
	    switch (iconStatus) {
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

	    notification.icon = icon;
	    notification.tickerText = tickerText;
	    notification.when = when;
	    if( statusOnline != 0) {
	    	notification.flags |= Notification.FLAG_NO_CLEAR;
	    }
		notification.number = unread;

		// TL: only show light (and/or vibrate) if there are unread messages
		if (unread > 0) {
			notification.flags |= Notification.FLAG_SHOW_LIGHTS;

			//	Blink differently when YOU are online
			if((statusOnline & STATUS_YOU) == STATUS_YOU) {
				notification.ledARGB = 0xFFFFFF00;
				notification.ledOffMS = 300;
				notification.ledOnMS = 1000;
			} else {
				notification.ledARGB = 0xFFFF0000;
				notification.ledOffMS = 1000;
				notification.ledOnMS = 300;
			}
			
			if (vibrate && activeNotify) {
				// Vibrate only if there are new messages
				long[] pattern = {0, 150, 250, 250, 250, 150};
				notification.vibrate = pattern;
			} else {
				notification.vibrate = null;
			}
		}

		Context context = getApplicationContext();
		CharSequence contentTitle = "Escribime";
		CharSequence contentText = "unread = " + unread;
		Intent notificationIntent = new Intent( context, Escribime.class);
		PendingIntent contentIntent = PendingIntent.getActivity( context, 0, notificationIntent, 0);
		notification.setLatestEventInfo( context, contentTitle, contentText, contentIntent);

		// Actually send the notification
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
				
		// Initialize the Notification object that will be used throughout the service lifetime
		CharSequence contentTitle = "Escribime";
		CharSequence contentText = "";
		Context context = getApplicationContext();
		Intent notificationIntent = new Intent( context, Escribime.class);

		PendingIntent contentIntent = PendingIntent.getActivity( context, 0, notificationIntent, 0);
		notification = new Notification(R.drawable.happyness, "", System.currentTimeMillis());
		notification.number = 1;
		notification.setLatestEventInfo( context, contentTitle, contentText, contentIntent);		
		
	
		Log.d(EscribimeService.class.getCanonicalName(), "Service started. Using updateInterval = " + updateInterval);
		timer.scheduleAtFixedRate( new TimerTask() {
			public void run() {
				int status = getStatus();
				// Which icon to use
				int icon = status;
				Log.d(EscribimeService.class.getCanonicalName(), "Got online status: " + status);
				final int unread = EscribimeService.this.fetchUnread( userid, password, label);
				if( unread == -1 ) {
					Log.d(EscribimeService.class.getCanonicalName(), "There was an error fetching unread. Discarding");
					return;
				}
				Log.d(EscribimeService.class.getCanonicalName(), "Got unread = " + unread);
				if( unread > 0) {
					icon += 4;
				}
				if (status > 0 || unread > 0) {
					// TL: need to verify the logic here to determine how to update the notification
					// It's more complicated because changes to the lime shouldn't trigger a vibrate
					// JAC: I added logic to also vibrate when YOU become online
					updateNotification(unread, status, icon, 
							(unread > lastUnread) || ((status & ~lastStatus & STATUS_YOU) == STATUS_YOU));
				} else {
					// Nothing to notify; clear the notification
					clearNotification();
				}
				lastUnread = unread;
				lastStatus = status;
			}
		}, 0, updateInterval * 1000);
		Log.d("EscribimeService","Monitoring label " + label + " every " + updateInterval + " seconds");
		
		EscribimeService.running = true;
	}
	
	private int fetchUnread(String userid, String password, String label) {
		int ret = -1;
		synchronized (this) {
			try {
			    HttpUriRequest request = new HttpGet("https://mail.google.com/mail/feed/atom/"+ label);
		        request.addHeader("Authorization", "Basic " + new String(Base64.encode((userid + ":" + password).getBytes(),Base64.NO_WRAP)));
				HttpResponse response = http.execute(request);
				if(response.getStatusLine().getStatusCode() != 200) {
					throw new Exception(response.getStatusLine().getReasonPhrase());
				}
				InputStream is = response.getEntity().getContent();
				
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(new InputSource(is));
				doc.getDocumentElement().normalize();
				NodeList nodeList = doc.getElementsByTagName("fullcount");
				Node node = nodeList.item(0);
				ret = Integer.parseInt(node.getTextContent());

				is.close();
			} catch (final Exception e) {
                e.printStackTrace();
                if( e.toString().contains("Unauthorized")) {
	                activity.runOnUiThread(new Runnable() {
	                	public void run() {
	                		activity.dealWithError( "Can't log in to GMail. Please check your settings.");
	                	}
	                });
                }
			}
		}
		return ret;
	}
	
	// -------------------------------------------------------------------
	// For online status
	
	/**
	 * Get our statuses from the online server
	 * @return 0 if neither is online
	 *         1 if you are online
	 *         2 if I am online
	 *         3 if we are both online
	 */
	int getStatus() {
	    HttpUriRequest request = new HttpGet("http://ofb.net:3300/status/q");
	    
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
		
		int status = 0;
		if( my_avail) {
			status |= STATUS_ME;
		}
		if( you_avail) {
			status |= STATUS_YOU;
		}
		
		return status;
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
