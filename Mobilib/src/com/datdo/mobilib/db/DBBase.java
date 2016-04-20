package com.datdo.mobilib.db;

import com.datdo.mobilib.util.MblUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public abstract class DBBase {
    private static SQLiteDatabase sDb;
    protected static SQLiteDatabase getDatabase() {
        if (sDb == null) {
            Context context = MblUtils.getCurrentContext().getApplicationContext();
            sDb = DBHelper.getInstance(context).getWritableDatabase();
        }
        return sDb;
    }
}
