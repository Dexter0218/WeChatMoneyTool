package com.qiandao.hongbao.util;

import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;

/**
 * Created by dexter0218 on 2018/1/15.
 */

public class ScreenUtil {
    private static final String TAG = "ScreenUtil";

    private static PowerManager pm;
    //唤醒锁
    private static PowerManager.WakeLock lock = null;

    private static KeyguardManager kManager;
    //安全锁
    private static KeyguardManager.KeyguardLock kLock = null;

    /**
     * 判断屏幕是否亮
     *
     * @param context the context
     * @return true when (at least one) screen is on
     */
    public static boolean isScreenOn(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            DisplayManager dm = (DisplayManager) context
                    .getSystemService(Context.DISPLAY_SERVICE);
            boolean screenOn = false;
            for (Display display : dm.getDisplays()) {
                if (display.getState() != Display.STATE_OFF) {
                    screenOn = true;
                }
            }
            return screenOn;
        } else {
            PowerManager pm = (PowerManager) context
                    .getSystemService(Context.POWER_SERVICE);
            // noinspection deprecation
            return pm.isScreenOn();
        }
    }


    /**
     * 判断是否加了安全锁
     *
     * @return
     */
    public static boolean isLockOn(Context context) {
        KeyguardManager kM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (kM != null) {
            if (kM.isKeyguardLocked()) { // && kM.isKeyguardSecure()) {
                return true;
            }
        }

        return false;
    }


    /**
     * 点亮屏幕
     */
    public static void lightScreen(Context context) {
        if (pm == null) {
            pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        }
        Log.e(TAG, "lightScreen()");
        lock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
        lock.acquire();
    }

    /**
     * 解锁
     */
    public static void unLock(Context context) {
        if (kManager == null) {
            kManager = ((KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE));
        }
        Log.e(TAG, "unLock()");
        kLock = kManager.newKeyguardLock(TAG);
        kLock.disableKeyguard();
    }

    /**
     * 清理环境
     */
    public static void clean() {

        Log.e(TAG, "clean()");
        if (kLock != null) {
            kLock.reenableKeyguard();
            kLock = null;
        }
        if (lock != null) {
            lock.release();
            lock = null;
        }
    }

    public static void sendClick(final int x,final int y) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"sendClick:"+x+"*"+y);
                Instrumentation mInst = new Instrumentation();
                mInst.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, x, y, 0));    //x,y 即是事件的坐标
                mInst.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0));
            }
        }).start();

    }
}
