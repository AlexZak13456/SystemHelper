/*
 * 反编译产物 —— 未反混淆（原始）
 * 来源：SystemHelper_1.2-20221110.apk / classes.dex
 * 类：b.c.a.b.l  —— SharedPreferences 封装（MODE_WORLD_READABLE=4）
 * 工具：JADX（MCP）
 * 说明：本文件为 JADX 原始输出，未作任何改写。
 * 审计要点：
 *   1. getSharedPreferences(..., 4) 使用 MODE_WORLD_READABLE；
 *   2. c(Context) 读取的 "serverAddress" 即上报 URL 前缀，可被导出服务改写；
 *   3. a() 以硬编码绝对路径删除配置文件。
 */

package b.c.a.b;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
public class l {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private static SimpleDateFormat f604a = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public static long a(Context context) {
        return a(context, "appInfoInterval", f.z);
    }

    public static long a(Context context, String str, long j) {
        return context.getSharedPreferences("com_mobiletools_systemhelper_preference", 4).getLong(str, j);
    }

    public static Boolean a(Context context, String str, Boolean bool) {
        return Boolean.valueOf(context.getSharedPreferences("com_mobiletools_systemhelper_preference", 4).getBoolean(str, bool.booleanValue()));
    }

    public static Boolean a(com.mobiletools.systemhelper.control.c cVar) {
        String str = cVar.A;
        if (str != null && !str.isEmpty()) {
            if (cVar.A.equals("1")) {
                String str2 = cVar.k;
                if (str2 != null && !str2.isEmpty() && (cVar.k.trim().equals("46001") || cVar.k.trim().equals("46006") || cVar.k.trim().equals("46009"))) {
                    return true;
                }
            } else if (cVar.A.equals("2")) {
                String str3 = cVar.u;
                if (str3 != null && !str3.isEmpty() && (cVar.u.trim().equals("46001") || cVar.u.trim().equals("46006") || cVar.u.trim().equals("46009"))) {
                    return true;
                }
            } else {
                j.b("RegisterUtils", "isUnicomNetwork->DataSlot：" + cVar.A);
            }
        }
        return false;
    }

    public static String a(Context context, String str, String str2) {
        return context.getSharedPreferences("com_mobiletools_systemhelper_preference", 4).getString(str, str2);
    }

    public static String a(String str) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return (String) cls.getMethod("get", String.class).invoke(cls, str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String a(String str, String str2) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return (String) cls.getMethod("get", String.class, String.class).invoke(cls.newInstance(), str, str2);
        } catch (Exception e) {
            j.b("RegisterUtils", "getSystemProperties->" + e.getMessage());
            return str2;
        }
    }

    @SuppressLint({"SdCardPath"})
    public static void a() {
        File file = new File("/data/data/com.mobiletools.systemhelper/shared_prefs/com_mobiletools_systemhelper_preference.xml");
        if (file.exists()) {
            file.delete();
        }
    }

    public static void a(Context context, long j) {
        b(context, "appInfoInterval", j);
    }

    public static void a(Context context, String str) {
        b(context, "serverAddress", str);
    }

    public static long b(Context context) {
        return a(context, "scheduleInterval", f.y);
    }

    public static Boolean b(String str) {
        return Boolean.valueOf((str == null || str.isEmpty() || (!str.trim().equals("46001") && !str.trim().equals("46006") && !str.trim().equals("46009"))) ? false : true);
    }

    public static String b() {
        return f604a.format(new Date());
    }

    public static String b(String str, String str2) {
        try {
            try {
                Class<?> cls = Class.forName("android.os.SystemProperties");
                return (String) cls.getMethod("get", String.class, String.class).invoke(cls, str, "unknown");
            } catch (Exception e) {
                e.printStackTrace();
                return str2;
            }
        } catch (Throwable unused) {
            return str2;
        }
    }

    public static void b(Context context, long j) {
        b(context, "scheduleInterval", j);
    }

    public static void b(Context context, String str, long j) {
        SharedPreferences.Editor editorEdit = context.getSharedPreferences("com_mobiletools_systemhelper_preference", 4).edit();
        editorEdit.putLong(str, j);
        editorEdit.commit();
    }

    public static void b(Context context, String str, Boolean bool) {
        SharedPreferences.Editor editorEdit = context.getSharedPreferences("com_mobiletools_systemhelper_preference", 4).edit();
        editorEdit.putBoolean(str, bool.booleanValue());
        editorEdit.commit();
    }

    public static void b(Context context, String str, String str2) {
        SharedPreferences.Editor editorEdit = context.getSharedPreferences("com_mobiletools_systemhelper_preference", 4).edit();
        editorEdit.putString(str, str2);
        editorEdit.commit();
    }

    public static String c(Context context) {
        return a(context, "serverAddress", f.w);
    }

    public static void c(Context context, long j) {
        b(context, "signalInfoInterval", j);
    }

    public static Boolean d(Context context) {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo();
        return Boolean.valueOf(activeNetworkInfo != null && activeNetworkInfo.isConnected());
    }

    public static Boolean e(Context context) {
        return Boolean.valueOf(((ConnectivityManager) context.getSystemService("connectivity")).getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED);
    }
}
