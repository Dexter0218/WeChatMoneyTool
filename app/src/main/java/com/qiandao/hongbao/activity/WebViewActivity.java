package com.qiandao.hongbao.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.qiandao.hongbao.R;
import com.qiandao.hongbao.util.DownloadUtil;
import com.qiandao.hongbao.util.VersionHelper;

/**
 * Created by Dexter0218 on 2016/9/6.
 */
public class WebViewActivity extends BaseActivity {
    private WebView webView;
    private String webViewUrl, webViewTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        VersionHelper.handleMaterialStatusBar(this);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && !bundle.isEmpty()) {
            webViewTitle = bundle.getString("title");
            webViewUrl = bundle.getString("url");

            TextView webViewBar = (TextView) findViewById(R.id.webview_bar);
            webViewBar.setText(webViewTitle);

            webView = (WebView) findViewById(R.id.webView);
            webView.getSettings().setBuiltInZoomControls(false);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url.indexOf("apk") > 0) {
                        Toast.makeText(getApplicationContext(), "正在准备下载", Toast.LENGTH_SHORT).show();
                        (new DownloadUtil()).enqueue(url, getApplicationContext());
                        return true;
                    } else {
                        view.loadUrl(url);
                        return false;
                    }
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    CookieSyncManager.getInstance().sync();
                }
            });
            webView.loadUrl(webViewUrl);
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
    }

    public void performBack(View view) {
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    public void openLink(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(this.webViewUrl));
        startActivity(intent);
    }
}
