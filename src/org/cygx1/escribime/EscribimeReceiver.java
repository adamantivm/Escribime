package org.cygx1.escribime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class EscribimeReceiver extends BroadcastReceiver {
    static final String ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
	public void onReceive(Context context, Intent intent) {
    	SharedPreferences settings = context.getSharedPreferences( Escribime.PREFS_NAME, 0);
    	boolean autoStart = settings.getBoolean("autoStart", true);
        if (intent.getAction().equals(ACTION) && autoStart) {
        	context.startService(new Intent(context, EscribimeService.class));
        }
    }
}
