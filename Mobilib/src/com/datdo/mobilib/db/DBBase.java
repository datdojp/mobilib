package com.datdo.mobilib.db;

import com.datdo.mobilib.util.MblUtils;

import android.database.sqlite.SQLiteDatabase;

public abstract class DBBase {
    private static SQLiteDatabase sDb;
    protected static SQLiteDatabase getDatabase() {
        if (sDb == null) {
            sDb = DBHelper.getInstance(MblUtils.getCurrentContext()).getWritableDatabase();
        }
        return sDb;
    }
}
