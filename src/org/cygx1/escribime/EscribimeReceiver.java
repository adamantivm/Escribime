package org.cygx1.escribime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class EscribimeReceiver extends BroadcastReceiver {
    static final String ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
	public void onReceive(Context context, Intent intent) {
    	// TL: retrieve preferences from global shared prefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	boolean autoStart = prefs.getBoolean("autoStart", true);
        if (intent.getAction().equals(ACTION) && autoStart) {
        	context.startService(new Intent(context, EscribimeService.class));
        }
    }
}
