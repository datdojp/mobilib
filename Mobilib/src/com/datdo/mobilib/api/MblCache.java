package com.datdo.mobilib.api;

import com.datdo.mobilib.util.MblUtils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

class MblCache extends DBBase {
    private static final String TABLE = "cache";
    private String mKey;
    private String mFileName;
    private long mDate;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + "("
                + "key      TEXT NOT NULL,"
                + "date     LONG)");
        db.execSQL("CREATE INDEX " + TABLE + "_index ON " + TABLE + "(key)");
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
    }

    public static void deleteAll() {
        getDatabase().delete(TABLE, null, null);
    }

    public static boolean insert(MblCache cache) {
        ContentValues values = new ContentValues();
        values.put("key", cache.getKey());
        values.put("date", cache.getDate());
        return -1 != getDatabase().insert(TABLE, null, values);
    }

    public static boolean update(MblCache cache) {
        ContentValues values = new ContentValues();
        values.put("date", cache.getDate());
        return 0 != getDatabase().update(TABLE, values, "key = ?", new String[] { cache.getKey() });
    }

    public static MblCache get(String key) {
        Cursor cur = getDatabase().query(
                TABLE,
                new String[] {"date"}, 
                "key = ?",
                new String[] { key },
                null, null, null);
        MblCache ret = null;

        if (cur.moveToNext()) {
            ret = new MblCache();
            ret.setKey(key);
            ret.setDate(cur.getLong(0));
        }
        cur.close();
        return ret;
    }


    // GENERATED

    public String getFileName() {
        return mFileName;
    }

    public String getKey() {
        return mKey;
    }
    public void setKey(String mKey) {
        this.mKey = mKey;
        this.mFileName = MblUtils.md5(mKey);
    }
    public long getDate() {
        return mDate;
    }
    public void setDate(long mDate) {
        this.mDate = mDate;
    }
}
