package com.qiandao.hongbao.view;

import android.content.Context;

import com.qiandao.hongbao.bean.User;

/**
 * Created by Dexter0218 on 2016/10/27.
 */
public interface IUserLoginView {
    String getUserName();
    String getPassword();
    void showLoading();
    void hideLoading();
    void toHomeActivity(User user);
    void showFailedError();
    Context getContext();
    void ErrorOfUsnAndPsd();
}
