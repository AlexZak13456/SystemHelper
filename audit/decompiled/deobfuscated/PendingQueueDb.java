package com.mobiletools.systemhelper.deob;

import android.content.ContentValues;
import android.content.Context;
import android.database.AbstractWindowedCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

public class PendingQueueDb extends SQLiteOpenHelper {

    public static final int DB_VERSION = 1;

    public static final String DB_NAME = "systemhelper";

    public static final String TABLE_NAME = "history";

    private static final int MAX_QUEUE_SIZE = 100;

    private static final AtomicInteger openCount = new AtomicInteger();
    private static PendingQueueDb instance = null;

    private PendingQueueDb(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public static synchronized PendingQueueDb open(Context context) {
        openCount.incrementAndGet();
        if (instance == null) {
            instance = new PendingQueueDb(context, DB_NAME, null, DB_VERSION);
        }
        return instance;
    }

    public synchronized void clear() {
        getWritableDatabase().delete(TABLE_NAME, null, null);
    }

    public synchronized void enqueue(String json) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{"jsonString"},
                null, null, null, null, null);
        if (cursor.getCount() >= MAX_QUEUE_SIZE) {
            cursor.moveToFirst();
            db.delete(TABLE_NAME, "jsonString=?",
                    new String[]{cursor.getString(cursor.getColumnIndex("jsonString"))});
        }
        ContentValues values = new ContentValues();
        values.put("jsonString", json);
        db.insert(TABLE_NAME, null, values);
    }

    public synchronized JSONArray read() {
        Cursor cursor = getWritableDatabase().query(TABLE_NAME,
                new String[]{"jsonString"}, null, null, null, null, null);
        if (cursor.getCount() <= 0) {
            return null;
        }
        JSONArray array = new JSONArray();
        while (cursor.moveToNext()) {
            try {
                array.put(new JSONObject(cursor.getString(cursor.getColumnIndex("jsonString"))));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return array;
    }

    public synchronized JSONArray readLarge() {
        AbstractWindowedCursor cursor = (AbstractWindowedCursor) getWritableDatabase()
                .query(TABLE_NAME, new String[]{"jsonString"}, null, null, null, null, null);
        cursor.setWindow(new CursorWindow("queryWindow", 20971520L));
        if (cursor.getCount() <= 0) {
            return null;
        }
        JSONArray array = new JSONArray();
        while (cursor.moveToNext()) {
            try {
                array.put(new JSONObject(cursor.getString(cursor.getColumnIndex("jsonString"))));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return array;
    }

    @Override
    public synchronized void close() {
        super.close();
        if (openCount.decrementAndGet() == 0) {
            instance = null;
        }
    }

    @Override
    public synchronized void onCreate(SQLiteDatabase db) {
        db.execSQL("create table history(id integer primary key autoincrement, jsonString text)");
    }

    @Override
    public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
