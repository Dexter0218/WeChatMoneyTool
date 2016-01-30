package com.qiandao.hongbao;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static String Tag ="HongbaoMainActivity";
    private final Intent mAccessibleIntent =
            new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);

    private Button switchPlugin;
    private EditText codeEditor;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        switchPlugin = (Button) findViewById(R.id.button_accessible);
        codeEditor = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView4);
        handleMIUIStatusBar();
        updateServiceStatus();


    }

    private void handleMIUIStatusBar() {
        Window window = getWindow();

        Class clazz = window.getClass();
        try {
            int tranceFlag = 0;
            int darkModeFlag = 0;
            Class layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");

            Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_TRANSPARENT");
            tranceFlag = field.getInt(layoutParams);

            Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
            extraFlagField.invoke(window, tranceFlag, tranceFlag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
    }

    @Override
    protected void onDestroy() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroy();
    }

    private void updateServiceStatus() {
        boolean serviceEnabled = false;

        AccessibilityManager accessibilityManager =
                (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> accessibilityServices =
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : accessibilityServices) {
            if (info.getId().equals(getPackageName() + "/.HongbaoService")) {
                serviceEnabled = true;
            }
        }

        if (serviceEnabled) {
            textView.setVisibility(View.INVISIBLE);
            codeEditor.setVisibility(View.INVISIBLE);
            switchPlugin.setText("关闭插件");
            // Prevent screen from dimming
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            textView.setVisibility(View.VISIBLE);
            codeEditor.setVisibility(View.VISIBLE);
            switchPlugin.setText("开启插件");
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

    }

    public void onButtonClicked(View view) {

        if (switchPlugin.getText().equals("开启插件")) {
            Log.e(Tag, "点击开启插件");
            String str = "000";
            int code = 0;
            String txt = codeEditor.getText().toString();
            Pattern p = Pattern.compile("[0-9]*");
            Matcher m = p.matcher(txt);
            if (m.matches()) {
                if (txt != null) {
                    str = String.valueOf(codeEditor.getText());
                }
                if (str != null && !str.equals("")) {
                    code = Integer.parseInt(str);
                }
                Util.checkPassCode(code);
                if (Util.isUseable()) {
                    Toast.makeText(this, "验证成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "验证失败", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else if (codeEditor.getEditableText().toString().equals("签到钱就到")) {
                long time = System.currentTimeMillis();
                Date date = new Date(time);
                int res = (date.getYear() + (date.getDay() * 32 - date.getMonth() * 5) / 3);
                Toast.makeText(this, res + "", Toast.LENGTH_SHORT).show();
                return;
            } else {
                Toast.makeText(this, "验证失败", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        startActivity(mAccessibleIntent);
    }


    public void openGithub(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/geeeeeeeeek/WeChatLuckyMoney"));
        startActivity(browserIntent);
    }
}
