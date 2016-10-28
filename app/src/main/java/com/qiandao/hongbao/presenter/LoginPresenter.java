package com.qiandao.hongbao.presenter;

import android.content.Context;
import android.content.Intent;

import com.qiandao.hongbao.IListerner.IUserBiz;
import com.qiandao.hongbao.IListerner.OnLoginListener;
import com.qiandao.hongbao.IListerner.biz.UserBiz;
import com.qiandao.hongbao.activity.MainActivity;
import com.qiandao.hongbao.activity.RegisterActivity;
import com.qiandao.hongbao.bean.User;
import com.qiandao.hongbao.view.IUserLoginView;

/**
 * Created by Dexter0218 on 2016/10/27.
 */
public class LoginPresenter {
    private Context mContext;
    private IUserBiz mIUserBiz;
    private IUserLoginView mUserLoginView;

    public LoginPresenter(Context context, IUserLoginView userLoginView) {
        mContext = context;
        mIUserBiz = new UserBiz();
        mUserLoginView = userLoginView;
    }

    public void login(){
        mUserLoginView.showLoading();
        mIUserBiz.login(mUserLoginView.getUserName(), mUserLoginView.getPassword(), new OnLoginListener() {
            @Override
            public void OnSuccess(User user) {
                mUserLoginView.hideLoading();
                mUserLoginView.toHomeActivity(user);
            }

            @Override
            public void OnFailed() {

            }
        },mUserLoginView.getContext());
    }

    /**
     * 跳转到注册页面
     */
    public void toRegisterActivity() {
        Intent intent = new Intent(mContext
                , RegisterActivity.class);
        mContext.startActivity(intent);
    }

    /**
     * 跳转到注册页面
     */
    public void toHomeActivity() {
        Intent intent = new Intent(mContext
                , MainActivity.class);
        mContext.startActivity(intent);
    }
}
