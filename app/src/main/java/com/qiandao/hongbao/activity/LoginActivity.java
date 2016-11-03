package com.qiandao.hongbao.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.qiandao.hongbao.R;
import com.qiandao.hongbao.bean.User;
import com.qiandao.hongbao.presenter.LoginPresenter;
import com.qiandao.hongbao.util.Helper;
import com.qiandao.hongbao.view.IUserLoginView;

import org.w3c.dom.Text;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;

/**
 * Created by Dexter0218 on 2016/10/27.
 */
public class LoginActivity extends BaseActivity implements IUserLoginView, View.OnClickListener {
    @BindView(R.id.et_username)
    EditText userNameEt;
    @BindView(R.id.et_password)
    EditText passwordEt;
    @BindView(R.id.btn_login)
    Button loginBtn;
    @BindView(R.id.tv_register)
    TextView registerTv;
    @BindView(R.id.pg_login)
    ProgressBar loginPg;

    private LoginPresenter mLoginPresenter = new LoginPresenter(this, this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        Helper.handleMaterialStatusBar(this);
        loginBtn.setOnClickListener(this);
        registerTv.setOnClickListener(this);
    }

    @Override
    public String getUserName() {
        if (userNameEt.getText() != null) {
            return userNameEt.getText().toString();
        } else {
            return null;
        }
    }

    @Override
    public void Success() {
        Toast.makeText(LoginActivity.this
                , "登陆成功", Toast.LENGTH_LONG).show();
    }

    @Override
    public String getPassword() {
        if (passwordEt.getText() != null) {
            return passwordEt.getText().toString();
        } else {
            return null;
        }
    }

    @Override
    public void showLoading() {
        loginPg.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoading() {
        loginPg.setVisibility(View.INVISIBLE);
    }

    @Override
    public void toHomeActivity(User user) {
        mLoginPresenter.toHomeActivity();
    }

    @Override
    public void showFailedError() {
        Toast.makeText(LoginActivity.this
                , "用户名或者密码错误", Toast.LENGTH_LONG).show();
    }

    @Override
    public Context getContext() {
        return LoginActivity.this;
    }

    @Override
    public void ErrorOfUsnAndPsd() {
        if (TextUtils.isEmpty(userNameEt.getText().toString()) || TextUtils.isEmpty(passwordEt.getText().toString())) {
            showFailedError();
        }
        ;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_login:
                mLoginPresenter.login();
                break;
            case R.id.tv_register:
                mLoginPresenter.toRegisterActivity();
                break;

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLoginPresenter = null;
    }
}
