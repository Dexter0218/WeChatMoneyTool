package com.qiandao.hongbao.IListerner;

import com.qiandao.hongbao.bean.User;

/**
 * Created by Dexter0218 on 2016/10/27.
 */
public interface OnLoginListener {
    void OnSuccess(User user);
    void OnFailed();
}
