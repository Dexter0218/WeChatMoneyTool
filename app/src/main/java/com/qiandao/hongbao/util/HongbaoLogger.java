package com.qiandao.hongbao.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by Zhongyi on 1/22/16.
 */
public class HongbaoLogger {
    private Context context;
    private SQLiteDatabase database;

    private static final String SENDER = "sender";
    private static final String CONTENT = "content";
    private static final String TIME = "time";
    private static final String AMOUNT = "amount";
    private LoggerDataBaseHelper dbHelper;

    public HongbaoLogger(final Context context) {
        this.context = context;
        this.initSchemaAndDatabase();
    }

    private void initSchemaAndDatabase() {
        dbHelper = new LoggerDataBaseHelper(context, LoggerDataBaseHelper.DATABASE_NAME, null, LoggerDataBaseHelper.DATABASE_VERSION);
    }

    public void writeHongbaoLog(String sender, String content, String amount) {
//        database.execSQL("INSERT INTO " + TABLE_NAME + " (sender,content,amount) VALUES( '" + sender + "', '" + content + "', '" + amount + "');");
        database = dbHelper.getWritableDatabase();
        ContentValues mValues = new ContentValues();
        mValues.put(SENDER, sender);
        mValues.put(CONTENT, content);
        mValues.put(AMOUNT, amount);
        long re = database.insert(LoggerDataBaseHelper.TABLE_NAME, "id", mValues);
        Log.e("wupeng", "re:" + re);
        database.close();
    }

    public void getAllLogs() {
//        Cursor cursor = database.rawQuery("select * from " + TABLE_NAME + " order by id", null);
        database = dbHelper.getReadableDatabase();
        Cursor cursor = database.query(LoggerDataBaseHelper.TABLE_NAME, new String[]{
                SENDER, CONTENT, AMOUNT}, null, null, null, null, null);
        cursor.moveToFirst();
        Log.e("wupeng", "Cursoe Count():" + cursor.getColumnCount());
        Log.e("wupeng", " cursor.getString(0):" + cursor.getString(0));
        Log.e("wupeng", " cursor.getString(1)" + cursor.getString(1));
        database.close();
    }

    public void closeDatebase() {
        database.close();
    }
}