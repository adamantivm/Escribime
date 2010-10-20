package org.cygx1.escribime;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * @author tlau
 *
 * Activity to handle setting the preferences
 */
public class EscribimePreferences extends PreferenceActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.escribimeprefs);
    }
}


