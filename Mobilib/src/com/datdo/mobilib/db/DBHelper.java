package com.datdo.mobilib.db;

import com.datdo.mobilib.cache.MblDatabaseCache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * データベースのヘルパークラス。
 */
class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "mobilib.db";
    private static final int DB_VERSION = 2;
    private static DBHelper instance;

    public static DBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DBHelper(context);
        }
        return instance;
    }

    private DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropTables(db);
        createTables(db);
    }

    public static void createTables(SQLiteDatabase db) {
        MblDatabaseCache.createTable(db);
    }

    public static void dropTables(SQLiteDatabase db) {
        MblDatabaseCache.dropTable(db);
    }
}
