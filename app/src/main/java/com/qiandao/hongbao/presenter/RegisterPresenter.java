package com.qiandao.hongbao.presenter;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.qiandao.hongbao.IListerner.OnRegisterListener;
import com.qiandao.hongbao.IListerner.biz.UserBiz;
import com.qiandao.hongbao.activity.LoginActivity;
import com.qiandao.hongbao.view.IUserRegisterView;

/**
 * Created by Dexter0218 on 2016/10/27.
 */
public class RegisterPresenter {
    private Context mContext;
    private UserBiz mUserBiz;
    private IUserRegisterView mUserRegisterView;

    public RegisterPresenter(Context context, IUserRegisterView userRegisterView) {
        mContext = context;
        mUserRegisterView = userRegisterView;
        this.mUserBiz = new UserBiz();
    }

    public void register() {
        if (TextUtils.isEmpty(mUserRegisterView.getPsd()) || TextUtils.isEmpty(mUserRegisterView.getUserName()) || TextUtils.isEmpty(mUserRegisterView.getConPsd()) || TextUtils.isEmpty(mUserRegisterView.getEmail())) {
            mUserRegisterView.ErrorOfUsnorPsdorEmail();
            return;
        }
        if (!mUserRegisterView.getConPsd().equals(mUserRegisterView.getPsd())) {
            mUserRegisterView.ErrorOfConfingerPsd();
            return;
        }
        mUserRegisterView.showLoading();
        mUserBiz.register(mUserRegisterView.getUserName(), mUserRegisterView.getPsd(), mUserRegisterView.getEmail(), new OnRegisterListener() {
            @Override
            public void OnSuccess() {
                mUserRegisterView.hideLoading();
                mUserRegisterView.Success();
                mUserRegisterView.FinishAty();
            }

            @Override
            public void OnError() {
                mUserRegisterView.hideLoading();
                mUserRegisterView.Failed();
            }
        }, mUserRegisterView.getContext());

    }


    //跳转到注册的页面
    public void toLoginActivity() {
        Intent intent = new Intent(mContext, LoginActivity.class);
        mContext.startActivity(intent);
    }

}
