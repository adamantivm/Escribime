package org.cygx1.escribime;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;

/***********************
 * @author adamantivm
 *
 * TODO
 * - Take credentials from Google account in the phone
 * - Hook up to synchronization service and update via that channel (to avoid more connections to the web)
 * - Unread count as icon
 * - Concealed shortcut from Activity to GMail label  
 * - Better UI/Configuration interaction
 * -- Start service automatically after opening if already configured
 * -- Allow stopping service without closing to configure service
 * - Error management, particularly:
 * -- Wrong Google userid / password
 * -- Wrong label
 * - Notification sound
 * - Customizable notification
 * - Start service on phone reboot
 */

public class Escribime extends Activity {
	public static final String PREFS_NAME = "EscribimePrefs";
	
	String userid, password, label;
	int updateInterval;
	boolean autoStart;
	EditText etUserid, etPassword, etLabel, etUpdateInterval;
	SeekBar bar;
	CheckBox cbAutoStart;
	
	ComponentName serviceName = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		final Intent svc = new Intent( this, EscribimeService.class);
		etUserid = (EditText)findViewById(R.id.ETUserid);
		etPassword = (EditText)findViewById(R.id.ETPassword);
		etLabel = (EditText)findViewById(R.id.ETLabel);
		etUpdateInterval = (EditText)findViewById(R.id.ETUpdateInterval);
		bar = (SeekBar)findViewById(R.id.SeekBar01);
		cbAutoStart = (CheckBox)findViewById(R.id.CheckBoxStartup);
		loadPreferences();
		
		//	Tie SeekBar to EditText for Update Interval
		bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				etUpdateInterval.setText(Integer.toString(progress));
			}
		});
		etLabel.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				try {
					bar.setProgress( Integer.parseInt(etLabel.getText().toString().trim()));
				} catch( Exception e) {}
			}
		});

		//	Start service
        final Button startButton = (Button) findViewById(R.id.Button01);
        startButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				savePreferences();
				setAllEnabled( false);
				EscribimeService.initialize( Escribime.this);
				serviceName = Escribime.this.startService( svc);
			}
		});

        //	Stop service and close
        final Button stopButton = (Button) findViewById(R.id.Button02);
        stopButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Escribime.this.stopService(svc);
				Escribime.this.finish();
			}
		});

        if( EscribimeService.running) {
        	Log.d("Escribime", "service was already running");
        	setAllEnabled( false);
        }
    }
    
    protected void setAllEnabled( boolean enabled) {
    	etUserid.setEnabled( enabled);
    	etPassword.setEnabled( enabled);
    	etLabel.setEnabled( enabled);
    	etUpdateInterval.setEnabled( enabled);
    	bar.setEnabled( enabled);
    }
    
    protected void loadPreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        userid = settings.getString("userid", "");
        password = settings.getString("password", "");
        label = settings.getString("label", "");
        updateInterval = settings.getInt("updateInterval", 60);
        autoStart = settings.getBoolean("autoStart", true);
        if(!"".equals(userid)) { etUserid.setText( userid); }
        if(!"".equals(password)) { etPassword.setText( password); }
        if(!"".equals(label)) { etLabel.setText( label); }
        etUpdateInterval.setText( Integer.toString(updateInterval));
        bar.setProgress( updateInterval);
        cbAutoStart.setChecked( autoStart);
    }
    
    protected void savePreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        userid = etUserid.getText().toString().trim();
        password = etPassword.getText().toString().trim();
        label = etLabel.getText().toString().trim();
        updateInterval = Integer.parseInt(etUpdateInterval.getText().toString().trim());
        autoStart = cbAutoStart.isChecked();
        editor.putString("userid", userid);
        editor.putString("password", password);
        editor.putString("label", label);
        editor.putInt("updateInterval", updateInterval);
        editor.putBoolean("autoStart", autoStart);
        editor.commit();
    }

	@Override
	protected void onStop() {
		savePreferences();
		super.onStop();
	}
}