package com.qiandao.hongbao.activity;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.qiandao.hongbao.R;
import com.qiandao.hongbao.StatusValue;
import com.qiandao.hongbao.util.ConnectivityUtil;
import com.qiandao.hongbao.util.VersionHelper;
import com.qiandao.hongbao.util.UpdateTask;
import com.tencent.bugly.crashreport.CrashReport;

import java.util.List;

public class MainActivity extends BaseActivity implements AccessibilityManager.AccessibilityStateChangeListener {

    private static String TAG = "HongbaoMainActivity";
    private TextView switchTextview;
    private ImageView switchImageview;
    private TextView versionTextView;
    private AccessibilityManager accessibilityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        VersionHelper.handleMaterialStatusBar(this);
        try {
            String weChatVersion =VersionHelper.getVersionName(this,VersionHelper.WechatPackageName);
            Log.d(TAG,"WeChatVersion:"+weChatVersion);
            if(weChatVersion.compareToIgnoreCase("6.6.0")>=0){
                Log.d(TAG,"大于6.6.0");
                StatusValue.getInstance().setIsSupportDelete(false);
            }else {
                Log.d(TAG,"小于6.6.0");
                StatusValue.getInstance().setIsSupportDelete(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        CrashReport.initCrashReport(getApplicationContext(), "900019366", false);
        accessibilityManager =
                (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        accessibilityManager.addAccessibilityStateChangeListener(this);
        switchTextview = (TextView) findViewById(R.id.tv_accessible);
        switchImageview = (ImageView) findViewById(R.id.im_accessible);
        versionTextView = (TextView) findViewById(R.id.tx_version);
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


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        updateServiceStatus();
        updateVersion();
        initPreferenceValue();
        // Check for update when WIFI is connected or on first time.
        if (ConnectivityUtil.isWifi(this) || UpdateTask.count == 0)
            new UpdateTask(this, false).update();
    }

    private void updateVersion() {
        versionTextView.setText("V"+getVersionName());
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
            switchTextview.setText("关闭插件");
            switchImageview.setImageResource(R.drawable.logo_stop);
            // Prevent screen from dimming
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            switchTextview.setText("开启插件");
            switchImageview.setImageResource(R.drawable.logo_start);
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
            case R.id.ly_accessible:
                try {
                    Intent mAccessibleIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(mAccessibleIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "遇到一些问题,请手动打开系统“设置”->找到“无障碍”或者“辅助服务”->“签到钱就到”", Toast.LENGTH_LONG).show();
                }

                break;
            case R.id.ly_setting:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
        }
    }

    public void copyCode(View view){
        // 从API11开始android推荐使用android.content.ClipboardManager
        // 为了兼容低版本我们这里使用旧版的android.text.ClipboardManager，虽然提示deprecated，但不影响使用。
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        // 将文本内容放到系统剪贴板里。
        cm.setText("人人可领，领完就能用。祝大家领取的红包金额大大大！#吱口令#长按复制此消息，打开支付宝就能领取！fbQfV839B2 ");
//        Toast.makeText(this, "复制成功，打开支付宝就可以领。", Toast.LENGTH_LONG).show();
        try{
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);//重点是加这个
            intent.setComponent(new ComponentName("com.eg.android.AlipayGphone","com.eg.android.AlipayGphone.AlipayLogin"));
            startActivity(intent);
        }catch (Exception e){
            Log.w(TAG,"Activity not found");
        }

    }



    public void openWeb(View view) {
        Intent webViewIntent = new Intent(this, WebViewActivity.class);
        webViewIntent.putExtra("title", "Blade A2 Plus");
        webViewIntent.putExtra("url", "https://item.m.jd.com/product/3370431.html");
        startActivity(webViewIntent);
    }

    public void openWeChat(View view) {

    }

    public void openLoginActivity(View view) {
        Toast.makeText(this, "签到成功", Toast.LENGTH_LONG).show();
//        Intent intent = new Intent(MainActivity.this,LoginActivity.class);
//        startActivity(intent);
    }

    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
        updateServiceStatus();
    }
}
