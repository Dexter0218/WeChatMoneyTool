package com.qiandao.hongbao.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.qiandao.hongbao.R;
import com.qiandao.hongbao.presenter.RegisterPresenter;
import com.qiandao.hongbao.view.IUserRegisterView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Dexter0218 on 2016/10/27.
 */
public class RegisterActivity extends BaseActivity implements IUserRegisterView, View.OnClickListener {
    @BindView(R.id.et_register_username)
    EditText mUsername;
    @BindView(R.id.et_register_password)
    EditText mPsd;
    @BindView(R.id.et_register_con_password)
    EditText mConfigPsd;
    @BindView(R.id.et_register_email)
    EditText mEmail;
    @BindView(R.id.btn_register)
    Button mSubmit;
    @BindView(R.id.pg_register)
    ProgressBar mRegisterProgress;

    private RegisterPresenter mRegisterPresenter = new RegisterPresenter(RegisterActivity.this, this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        mSubmit.setOnClickListener(this);
    }

    @Override
    public void Success() {
        Toast.makeText(getApplicationContext(),"注册成功",Toast.LENGTH_SHORT).show();
        mRegisterPresenter.toLoginActivity();
    }

    @Override
    public void Failed() {
        Toast.makeText(getApplicationContext(),"注册失败",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showLoading() {
        mRegisterProgress.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoading() {
        mRegisterProgress.setVisibility(View.INVISIBLE);
    }

    @Override
    public void FinishAty() {
        RegisterActivity.this.finish();
    }

    @Override
    public String getUserName() {
        return mUsername.getText().toString();
    }


    @Override
    public String getPsd() {
        return mPsd.getText().toString();
    }

    @Override
    public String getEmail() {
        return mEmail.getText().toString();
    }

    @Override
    public String getConPsd() {
        return mConfigPsd.getText().toString();
    }

    @Override
    public Context getContext() {
        return RegisterActivity.this;
    }

    @Override
    public void ErrorOfUsnorPsdorEmail() {
        Toast.makeText(getApplicationContext(),"用户名,密码或者邮箱错误",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void ErrorOfConfingerPsd() {
        Toast.makeText(getApplicationContext(),"两次密码不一致",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_register:
                mRegisterPresenter.register();
        }
    }
}
