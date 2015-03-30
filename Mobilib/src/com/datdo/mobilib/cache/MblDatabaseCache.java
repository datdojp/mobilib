package com.datdo.mobilib.cache;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.datdo.mobilib.db.DBBase;
import com.datdo.mobilib.util.MblUtils;

public class MblDatabaseCache extends DBBase {

    private static final String TABLE       = "cache";
    private static final String COL_KEY     = "key";
    private static final String COL_DATE    = "date";

    private String      mKey;
    private long        mDate;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + "("
                + COL_KEY   + " TEXT NOT NULL,"
                + COL_DATE  + " LONG)");
        db.execSQL("CREATE INDEX " + TABLE + "_index ON " + TABLE + "(" + COL_KEY + ")");
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
    }

    public MblDatabaseCache() {
        super();
    }

    public MblDatabaseCache(String key, long date) {
        super();
        mKey    = key;
        mDate   = date;
    }

    public static void deleteAll() {
        getDatabase().delete(TABLE, null, null);
    }

    private static MblDatabaseCache fromCursor(Cursor cur) {
        MblDatabaseCache c = new MblDatabaseCache();
        c.setKey(cur.getString(0));
        c.setDate(cur.getLong(1));
        return c;
    }

    private static ContentValues toContentValues(MblDatabaseCache c) {
        ContentValues values = new ContentValues();
        values.put(COL_KEY, c.getKey());
        values.put(COL_DATE, c.getDate());
        return values;
    }

    public static void upsert(MblDatabaseCache c) {
        getDatabase().insertWithOnConflict(
                TABLE,
                null,
                toContentValues(c),
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void upsert(List<MblDatabaseCache> caches) {
        getDatabase().beginTransaction();
        for (MblDatabaseCache c : caches) {
            getDatabase().insertWithOnConflict(
                    TABLE,
                    null,
                    toContentValues(c),
                    SQLiteDatabase.CONFLICT_REPLACE);
        }
        getDatabase().setTransactionSuccessful();
        getDatabase().endTransaction();
    }

    public static MblDatabaseCache get(String key) {
        Cursor cur = getDatabase().query(
                TABLE,
                null, 
                COL_KEY + " = ?",
                new String[] { key },
                null, null, null);
        MblDatabaseCache c = null;
        if (cur.moveToNext()) {
            c = fromCursor(cur);
        }
        cur.close();
        return c;
    }

    public static List<MblDatabaseCache> getAll() {
        Cursor cur = getDatabase().query(TABLE, null, null, null, null, null, null);
        List<MblDatabaseCache> ret = new ArrayList<MblDatabaseCache>();
        while (cur.moveToNext()) {
            ret.add(fromCursor(cur));
        }
        cur.close();
        return ret;
    }

    public static List<MblDatabaseCache> get(List<String> keys, long duration) {

        if (MblUtils.isEmpty(keys)) {
            return new ArrayList<MblDatabaseCache>();
        }

        String[] placeholder = new String[keys.size()];
        String[] selectionArgs = new String[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            placeholder[i]      = "?";
            selectionArgs[i]    = keys.get(i);
        }

        Cursor cur = getDatabase().query(
                TABLE,
                null,
                COL_KEY + " IN (" + TextUtils.join(",", placeholder) + ") AND " + COL_DATE + " + " + duration + " > " + System.currentTimeMillis(),
                selectionArgs,
                null, null, null);
        List<MblDatabaseCache> ret = new ArrayList<MblDatabaseCache>();
        while (cur.moveToNext()) {
            ret.add(fromCursor(cur));
        }
        cur.close();
        return ret;
    }

    public String getKey() {
        return mKey;
    }

    public void setKey(String key) {
        mKey = key;
    }

    public long getDate() {
        return mDate;
    }

    public void setDate(long date) {
        mDate = date;
    }
}
