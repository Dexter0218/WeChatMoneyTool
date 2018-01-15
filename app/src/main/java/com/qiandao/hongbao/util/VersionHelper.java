package com.qiandao.hongbao.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import com.qiandao.hongbao.R;

/**
 * Created by Dexter0218 on 2016/10/27.
 */


public  class VersionHelper {
    public static final String WechatPackageName = "com.tencent.mm";
    public static final String WechatClassName = "com.tencent.mm.ui.LauncherUI";


    public static void handleMaterialStatusBar(Activity activity) {
        // Not supported in APK level lower than 21
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        Window window = activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(activity.getResources().getColor(R.color.backgroud_red));
    }

    /**
     * 获取指定包名的版本号
     *
     * @param context
     *            本应用程序上下文
     * @param packageName
     *            你想知道版本信息的应用程序的包名
     * @return
     * @throws Exception
     */
    public static String getVersionName(Context context, String packageName) throws Exception {
        // 获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packInfo = packageManager.getPackageInfo(packageName, 0);
        String version = packInfo.versionName;
        return version;
    }
}
