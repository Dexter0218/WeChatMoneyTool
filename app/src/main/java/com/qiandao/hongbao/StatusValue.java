package com.qiandao.hongbao;

/**
 * Created by dexter0218 on 16-2-4.
 */
public class StatusValue {
    /**
     * 单例设计
     */
    private static StatusValue statusInstance;
    private static boolean isSupportNotification = true;
    private static boolean isSupportAutoRob = true;
    private static boolean isSupportBlackSreen = true;
    private static String[] exculdeWords = {};

    private StatusValue() {
    }

    /**
     * 单例设计，惰性实例化
     *
     * @return 返回唯一的实例
     */
    public static StatusValue getInstance() {
        if (statusInstance == null) statusInstance = new StatusValue();
        return statusInstance;
    }

    public boolean isSupportNotification() {
        return isSupportNotification;
    }

    public void setIsSupportNotification(boolean isSupportNotification) {
        this.isSupportNotification = isSupportNotification;
    }

    public boolean isSupportBlackSreen() {
        return isSupportBlackSreen;
    }

    public void setIsSupportBlackSreen(boolean isSupportBlackSreen) {
        this.isSupportBlackSreen = isSupportBlackSreen;
    }

    public String[] getExculdeWords() {
        return exculdeWords;
    }

    public void setExculdeWords(String[] exculdeWords) {
        this.exculdeWords = exculdeWords;
    }

    public static boolean isSupportAutoRob() {
        return isSupportAutoRob;
    }

    public static void setIsSupportAutoRob(boolean isSupportAutoRob) {
        StatusValue.isSupportAutoRob = isSupportAutoRob;
    }

}
