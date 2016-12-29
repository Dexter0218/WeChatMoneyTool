package com.qiandao.hongbao;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.qiandao.hongbao.util.ConnectivityUtil;
import com.qiandao.hongbao.util.HongbaoLogger;
import com.qiandao.hongbao.util.UpdateTask;
import com.tencent.bugly.crashreport.CrashReport;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements AccessibilityManager.AccessibilityStateChangeListener {

    private static String TAG = "HongbaoMainActivity";
    private Button switchPlugin;
    private AccessibilityManager accessibilityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        CrashReport.initCrashReport(getApplicationContext(), "900019366", false);
        accessibilityManager =
                (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        accessibilityManager.addAccessibilityStateChangeListener(this);
        switchPlugin = (Button) findViewById(R.id.button_accessible);
        handleMaterialStatusBar();
        updateServiceStatus();


    }

    private void initPreferenceValue() {
        String excludeWordses = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_watch_exclude_words", "");
        StatusValue.getInstance().setExculdeWords(excludeWordses);

        boolean issupportBlackSceen = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_watch_black_screen_notification", true);
        StatusValue.getInstance().setIsSupportBlackSreen(issupportBlackSceen);

        boolean isSupportAutoRob = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_click_open_hongbao", true);
        StatusValue.getInstance().setIsSupportAutoRob(isSupportAutoRob);

    }

    private void handleMaterialStatusBar() {
        // Not supported in APK level lower than 21
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(0xffd84e43);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        updateServiceStatus();
        initPreferenceValue();
        // Check for update when WIFI is connected or on first time.
        if (ConnectivityUtil.isWifi(this) || UpdateTask.count == 0)
            new UpdateTask(this, false).update();
    }

    @Override
    protected void onDestroy() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    private void updateServiceStatus() {
        boolean serviceEnabled = false;
        if (accessibilityManager == null) return;
        List<AccessibilityServiceInfo> accessibilityServices =
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : accessibilityServices) {
            if (info != null && info.getId() != null && info.getId().equals(getPackageName() + "/.HongbaoService")) {
                serviceEnabled = true;
            }
        }

        if (serviceEnabled) {
            switchPlugin.setText("关闭插件");
            // Prevent screen from dimming
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            switchPlugin.setText("开启插件");
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
//        Toast.makeText(this, "版本号：" + getVersionName(), Toast.LENGTH_SHORT).show();
    }

    private String getVersionName() {
        String version = "";
        int versioncode = 0;
        try {
            PackageManager packageManager = getPackageManager();
            // getPackageName()是你当前类的包名，0代表是获取版本信息
            PackageInfo packInfo = packageManager.getPackageInfo(getPackageName(), 0);
            version = packInfo.versionName;
            versioncode = packInfo.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return version + "." + versioncode;
    }

    public void onButtonClicked(View view) {
        switch (view.getId()) {
            case R.id.button_accessible:
                try {
                    Intent mAccessibleIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(mAccessibleIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "遇到一些问题,请手动打开系统“设置”->找到“无障碍”或者“辅助服务”->“签到钱就到”", Toast.LENGTH_LONG).show();
                }

                break;
            case R.id.button_seting:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
        }
    }


    public void openGithub(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/geeeeeeeeek/WeChatLuckyMoney"));
        startActivity(browserIntent);
    }

    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
        updateServiceStatus();
    }
}
