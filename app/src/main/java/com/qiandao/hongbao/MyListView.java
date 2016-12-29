package com.qiandao.hongbao;

import android.app.Activity;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.qiandao.hongbao.util.HongbaoLogger;
import com.qiandao.hongbao.util.LoggerDataBaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MyListView extends Activity {

    private ListView listView;
    private HongbaoLogger logger;

    //private List<String> data = new ArrayList<String>();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listView = new ListView(this);
        logger = HongbaoLogger.getInstance(this);
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getData()));
        setContentView(listView);


        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(0xffd84e43);
    }

    @Override
    protected void onResume() {
        super.onResume();
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getData()));
    }

    private List<String> getData() {

        List<String> data = new ArrayList<String>();
        if (logger != null) {
            data = logger.getAllLogs();
            Log.e("wupeng", "data:" + data.size());
        }
        if (data == null) {
            data.add("test1");
            data.add("test2");
        }
        return data;
    }
}