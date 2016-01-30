package com.qiandao.hongbao;

import java.util.Date;


/**
 * Created by dexter0218 on 16-1-19.
 */
public class Util {
    private static String Tag = "HongbaoUtil";
    private static boolean isUseable = false;

    public static boolean isUseable() {
        return isUseable;
    }


    public static void checkPassCode(int code) {
        long time = System.currentTimeMillis();
        Date date = new Date(time);
        int res = (date.getYear() + (date.getDay() * 32 - date.getMonth() * 5) / 3);
        if (code == res) {
            isUseable = true;
        }else{
            isUseable = false;
        }
    }
}
