package com.qiandao.hongbao.IListerner;

import android.content.Context;
import android.view.View;

/**
 * Created by Dexter0218 on 2016/10/27.
 */
public interface IUserBiz {
    void login(String userName, String password, OnLoginListener loginListener, Context context);
    void register(String userName, String password, String email, OnRegisterListener registerListener, Context context);
}
