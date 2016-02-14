package com.qiandao.hongbao;

import android.content.SharedPreferences;
import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.qiandao.hongbao.util.UpdateTask;

public class SettingsActivity extends PreferenceActivity {
    private static final String TAG = "SettingsActivity";
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_layout);
        addPreferencesFromResource(R.xml.setting_preferences);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setstatusBar();
        updateExcludeWordsPreference();
    }

    private void setstatusBar() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;

        Window window = this.getWindow();

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        window.setStatusBarColor(0xffd84e43);
    }

    private void updateExcludeWordsPreference() {
        Preference exculdePreference = findPreference("pref_watch_exclude_words");
        String oriSummary = getResources().getString(R.string.pref_watch_exclude_words_summary);
        String value = sharedPreferences.getString("pref_watch_exclude_words", "");
        if (value.length() > 0) {
            exculdePreference.setSummary(oriSummary + ":" + value);
        }
        exculdePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                String summary = getResources().getString(R.string.pref_watch_exclude_words_summary);
                if (obj != null && obj.toString().length() > 0) {
                    preference.setSummary(summary + ":" + obj.toString());
                    StatusValue.getInstance().setExculdeWords(obj.toString());
                } else {
                    preference.setSummary(summary);
                    StatusValue.getInstance().setExculdeWords("");
                }

                return true;
            }
        });
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        Log.e(TAG, "key:" + key);
        switch (key) {
            //黑屏抢
            case "pref_watch_black_screen_notification":
                StatusValue.getInstance().setIsSupportBlackSreen(sharedPreferences.getBoolean(key, true));
                break;
            //自动拆红包
            case "pref_click_open_hongbao":
                StatusValue.getInstance().setIsSupportAutoRob(sharedPreferences.getBoolean(key, true));
                break;
            case "pref_check_update":
                new UpdateTask(this, true).update();
                break;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);

    }
}
