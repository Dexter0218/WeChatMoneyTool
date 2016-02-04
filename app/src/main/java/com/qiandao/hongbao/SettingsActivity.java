package com.qiandao.hongbao;

import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;

public class SettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_layout);
        addPreferencesFromResource(R.xml.setting_preferences);
        setstatusBar();
        setPrefListeners();
    }

    private void setstatusBar() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;

        Window window = this.getWindow();

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        window.setStatusBarColor(0xffd84e43);
    }

    private void setPrefListeners() {
        Preference exculdePreference = findPreference("pref_watch_exclude_words");
        String oriSummary = getResources().getString(R.string.pref_watch_exclude_words_summary);
        String value = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_watch_exclude_words", "");
        if (value.length() > 0) {
            exculdePreference.setSummary(oriSummary + ":" + value);
        }

        exculdePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                String summary = getResources().getString(R.string.pref_watch_exclude_words_summary);
                if (o != null && o.toString().length() > 0)
                    preference.setSummary(summary + ":" + o.toString());
                return true;
            }
        });

    }


}
