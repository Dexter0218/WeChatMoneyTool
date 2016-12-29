package com.qiandao.hongbao.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by dexter0218 on 16-2-22.
 */
public class LoggerDataBaseHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "WeChatLuckyMoney.db";
    public static final String TABLE_NAME = "HongbaoLog";
    private static final String createDatabaseSQL = "CREATE TABLE IF NOT EXISTS HongbaoLog (id INTEGER PRIMARY KEY AUTOINCREMENT, sender TEXT, content TEXT, time TEXT, amount TEXT);";

    public LoggerDataBaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createDatabaseSQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
