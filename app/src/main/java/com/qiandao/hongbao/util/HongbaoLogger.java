package com.qiandao.hongbao.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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
    static HongbaoLogger instance;

    HongbaoLogger(final Context context) {
        this.context = context;
        this.initSchemaAndDatabase();
    }

    public static HongbaoLogger getInstance(Context context) {
        if (instance == null) {
            instance = new HongbaoLogger(context);
        }
        return instance;
    }

    private void initSchemaAndDatabase() {
        dbHelper = new LoggerDataBaseHelper(context, LoggerDataBaseHelper.DATABASE_NAME, null, LoggerDataBaseHelper.DATABASE_VERSION);
    }

    public void writeHongbaoLog(String sender, String content, String amount) {
//        database.execSQL("INSERT INTO " + TABLE_NAME + " (sender,content,amount) VALUES( '" + sender + "', '" + content + "', '" + amount + "');");
        try {
            database = dbHelper.getWritableDatabase();
            ContentValues mValues = new ContentValues();
            mValues.put(SENDER, sender);
            mValues.put(CONTENT, content);
            mValues.put(AMOUNT, amount);
            long re = database.insert(LoggerDataBaseHelper.TABLE_NAME, "id", mValues);
            Log.e("wupeng", "re:" + re);
            database.close();
        } catch (Exception e) {

        } finally {
            if (database != null) {
                database.close();
            }
        }

    }

    public List<String> getAllLogs() {
//        Cursor cursor = database.rawQuery("select * from " + TABLE_NAME + " order by id", null);
        List<String> list = new ArrayList<String>();
        Cursor cursor = null;
        try {
            database = dbHelper.getReadableDatabase();
            cursor = database.query(LoggerDataBaseHelper.TABLE_NAME, new String[]{
                    SENDER, CONTENT, AMOUNT}, null, null, null, null, null);
            if (cursor.moveToFirst() == false) {
                //为空的Cursor
                return null;
            }
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
            }
            while (cursor.moveToNext()) {
                list.add("从" + cursor.getString(0) + "抢了" + cursor.getString(2));
            }
        } catch (Exception e) {

        } finally {
            if (database != null) {
                database.close();
            }
            if (cursor != null) {
                cursor.close();
            }
        }


        return list;
    }

    public void closeDatebase() {
        database.close();
    }
}