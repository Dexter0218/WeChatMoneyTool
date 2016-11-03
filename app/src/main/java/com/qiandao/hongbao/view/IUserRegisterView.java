package com.qiandao.hongbao.view;

import android.content.Context;

/**
 * Created by Dexter0218 on 2016/10/27.
 */
public interface IUserRegisterView {
    void Success();
    void Failed();
    void showLoading();
    void hideLoading();
    void FinishAty();
    String getUserName();
    String getPsd();
    String getEmail();
    String getConPsd();
    Context getContext();
    void ErrorOfUsnorPsdorEmail();
    void ErrorOfConfingerPsd();
}
