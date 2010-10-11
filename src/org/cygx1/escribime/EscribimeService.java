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
import android.util.Base64;
import android.util.Log;

public class EscribimeService extends Service {
	private static final int NOTIFICATION_ID = 120198237;
	
    static String userid;
    static String password;
    static String label;
    static int updateInterval;
    
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
        SharedPreferences settings = getSharedPreferences( Escribime.PREFS_NAME, 0);
        userid = settings.getString("userid", "");
        password = settings.getString("password", "");
        label = settings.getString("label", "");
        updateInterval = settings.getInt("updateInterval", 60);
   }

   private void updateNotification( int unread) {
		int icon = R.drawable.happyness;
		CharSequence tickerText = "unread = " + unread;
		long when = System.currentTimeMillis();
		notification = new Notification(icon, tickerText, when);
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		notification.ledARGB = 0xFFFF0000;
		notification.ledOnMS = 300;
		notification.ledOffMS = 1000;
		
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

		loadPreferences();
	
		Log.d(EscribimeService.class.getCanonicalName(), "Service started. Using updateInterval = " + updateInterval);
		timer.scheduleAtFixedRate( new TimerTask() {
			public void run() {
				final int unread = EscribimeService.this.fetchUnread( userid, password, label);
				Log.d(EscribimeService.class.getCanonicalName(), "Got unread = " + unread);
				if( unread > 0) {
					updateNotification( unread);
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
