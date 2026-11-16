/*
 * 反编译产物 —— 未反混淆（原始）
 * 来源：SystemHelper_1.2-20221110.apk / classes.dex
 * 类：b.c.a.b.d  —— SQLite 待上报队列（库 systemhelper / 表 history）
 * 工具：JADX（MCP）
 * 说明：本文件为 JADX 原始输出，未作任何改写。
 * 审计要点：jsonString 列明文存储完整设备信息 JSON，上限 100 条；onUpgrade 为空实现。
 */

package b.c.a.b;

import android.content.ContentValues;
import android.content.Context;
import android.database.AbstractWindowedCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class d extends SQLiteOpenHelper {

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public static Integer f597b = 1;
    public static String c = "systemhelper";
    public static String d = "history";
    private static AtomicInteger e = new AtomicInteger();
    private static d f = null;

    private d(Context context, String str, int i) {
        this(context, str, null, i);
    }

    private d(Context context, String str, SQLiteDatabase.CursorFactory cursorFactory, int i) {
        super(context, str, cursorFactory, i);
    }

    public static synchronized d a(Context context, String str, int i) {
        e.incrementAndGet();
        if (f == null) {
            f = new d(context, str, i);
        }
        return f;
    }

    public synchronized void a() {
        getWritableDatabase().delete(d, null, null);
    }

    public synchronized void a(String str) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        Cursor cursorQuery = writableDatabase.query(d, new String[]{"jsonString"}, null, null, null, null, null);
        if (cursorQuery.getCount() >= 100) {
            cursorQuery.moveToFirst();
            writableDatabase.delete(d, "jsonString=?", new String[]{cursorQuery.getString(cursorQuery.getColumnIndex("jsonString"))});
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("jsonString", str);
        writableDatabase.insert(d, null, contentValues);
    }

    public synchronized JSONArray b() {
        Cursor cursorQuery = getWritableDatabase().query(d, new String[]{"jsonString"}, null, null, null, null, null);
        if (cursorQuery.getCount() <= 0) {
            return null;
        }
        JSONArray jSONArray = new JSONArray();
        while (cursorQuery.moveToNext()) {
            try {
                jSONArray.put(new JSONObject(cursorQuery.getString(cursorQuery.getColumnIndex("jsonString"))));
            } catch (JSONException e2) {
                e2.printStackTrace();
            }
        }
        return jSONArray;
    }

    public synchronized JSONArray c() {
        AbstractWindowedCursor abstractWindowedCursor = (AbstractWindowedCursor) getWritableDatabase().query(d, new String[]{"jsonString"}, null, null, null, null, null);
        abstractWindowedCursor.setWindow(new CursorWindow("queryWindow", 20971520L));
        if (abstractWindowedCursor.getCount() <= 0) {
            return null;
        }
        JSONArray jSONArray = new JSONArray();
        while (abstractWindowedCursor.moveToNext()) {
            try {
                jSONArray.put(new JSONObject(abstractWindowedCursor.getString(abstractWindowedCursor.getColumnIndex("jsonString"))));
            } catch (JSONException e2) {
                e2.printStackTrace();
            }
        }
        return jSONArray;
    }

    @Override // android.database.sqlite.SQLiteOpenHelper, java.lang.AutoCloseable
    public synchronized void close() {
        super.close();
        if (e.decrementAndGet() == 0) {
            f = null;
        }
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public synchronized void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("create table history(id integer primary key autoincrement, jsonString text)");
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public synchronized void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
    }
}
