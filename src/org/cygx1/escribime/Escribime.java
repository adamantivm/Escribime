package org.cygx1.escribime;

import java.io.IOException;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

/***********************
 * @author adamantivm
 * 
 *  TODO 
 *  - Take credentials from Google account in the phone 
 *  - Hook up to synchronization service and update via that channel (to avoid more
 *    connections to the web)
 *  - Unread count as icon
 *  - Concealed shortcut from Activity to GMail label
 *  - Better UI/Configuration interaction
 *  -- Start service automatically after opening if already configured
 *  -- Allow stopping service without closing to configure service
 *  - Error management, particularly:
 *  -- Wrong Google userid / password
 *  -- Wrong label
 *  - Notification sound
 *  - Customizable notification
 *  - Start service on phone reboot
 * 
 *  - Hide email/password settings behind the menu button
 */

public class Escribime extends Activity {
	public static final String PREFS_NAME = "EscribimePrefs";
	private static final int PREFS_ID = 0;

	ComponentName serviceName = null;
	
	Button startButton;
	Button stopButton;
	Intent svc;

	// TL: sorry, we should probably reuse a single browser rather than creating
	// one
	// here and one in the EscribimeService class
	final AndroidHttpClient http = AndroidHttpClient
			.newInstance("CygX1 browser");
	
	
	/**
	 * Enable/disable start/stop buttons, to indicate whether the service
	 * is running or not
	 */
	private void setRunning( boolean running) {
		startButton.setEnabled( !running);
		stopButton.setEnabled( running);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		svc = new Intent(this, EscribimeService.class);

		// Start service
		startButton = (Button) findViewById(R.id.Button01);
		startButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EscribimeService.initialize(Escribime.this);
				serviceName = Escribime.this.startService(svc);
				setRunning( true);
			}
		});

		// Stop service and close
		stopButton = (Button) findViewById(R.id.Button02);
		stopButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Escribime.this.stopService(svc);
				EscribimeService.running = false;
				setRunning( false);
				Escribime.this.finish();
			}
		});

		// TL: Handle the online toggle
		final ToggleButton onlineButton = (ToggleButton) findViewById(R.id.ToggleButton01);
		onlineButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				Log.d("Escribime", "Toggle button toggled to: " + isChecked);
				setMyStatus(isChecked);
			}
		});

		setRunning( EscribimeService.running);
		
		// If we haven't set our preferences yet, start with that view
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String email = prefs.getString(getString(R.string.emailPref), null);
        if (email == null) {
        	startActivity(new Intent(this, EscribimePreferences.class));
        }
	}

	public void dealWithError( String theError) {
		Toast.makeText( this, theError, Toast.LENGTH_LONG).show();
		stopService( svc);
		setRunning( false);
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	// ----------------------------------------------------------------------------
	// Online status

	/**
	 * Sends an HTTP request to the status server to update my status
	 */
	class StatusUpdate extends Thread implements Runnable {
		boolean availability;
		String email;
		
		@Override
		public void run() {
			Log.d("Escribime", "Setting availablity for " + email + " to "
					+ availability);
			HttpUriRequest request = new HttpGet(
					"http://ofb.net:3300/status/update?email=" + email
							+ "&available=" + (availability ? "true" : "false"));

			try {
				http.execute(request);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.d("Escribime", "Error updating status:" + e);
				return;
			}
		}

		// Uh, how am I supposed to pass data into a thread?
		void setData(boolean avail, String email) {
			this.availability = avail;
			this.email = email;
		}
	};

	void setMyStatus(boolean availability) {		
		// Get my email address out of preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String email = prefs.getString(getString(R.string.emailPref), null);

		// Need to create a new thread to do HTTP requests
		StatusUpdate t = new StatusUpdate();
		t.setData(availability, email);
		t.start();
	}

	/**
	 * Handle the preferences menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, PREFS_ID, Menu.NONE, "Prefs")
				.setIcon(android.R.drawable.ic_menu_preferences)
				.setAlphabeticShortcut('p');
		return (super.onCreateOptionsMenu(menu));
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case PREFS_ID:
			startActivity(new Intent(this, EscribimePreferences.class));
			return (true);
		}
		return (super.onOptionsItemSelected(item));
	}

}