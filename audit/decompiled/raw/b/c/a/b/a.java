/*
 * 反编译产物 —— 未反混淆（原始）
 * 来源：SystemHelper_1.2-20221110.apk / classes.dex
 * 类：b.c.a.b.a  —— AlarmManager 调度器（4 个入口，均自循环）
 * 工具：JADX（MCP）
 * 说明：本文件为 JADX 原始输出，未作任何改写。
 * 审计要点：使用 setExactAndAllowWhileIdle 穿透 Doze 待机；PendingIntent flag 0x0C000000。
 */

package b.c.a.b;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import com.mobiletools.systemhelper.control.AppInfoRegisterReceiver;
import com.mobiletools.systemhelper.control.OnBootReceiver;
import com.mobiletools.systemhelper.control.SIMStateReceiver;
import com.mobiletools.systemhelper.control.ScheduleRegisterReceiver;

/* JADX INFO: loaded from: classes.dex */
public class a {
    public static void a(Context context, long j) {
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, new Intent(context, (Class<?>) AppInfoRegisterReceiver.class), 201326592);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
        if (!a()) {
            alarmManager.set(2, SystemClock.elapsedRealtime() + j, broadcast);
            return;
        }
        boolean zB = b();
        long jElapsedRealtime = SystemClock.elapsedRealtime() + j;
        if (zB) {
            alarmManager.setExactAndAllowWhileIdle(2, jElapsedRealtime, broadcast);
        } else {
            alarmManager.setExact(2, jElapsedRealtime, broadcast);
        }
    }

    public static boolean a() {
        return Build.VERSION.SDK_INT > 18;
    }

    public static void b(Context context, long j) {
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, new Intent(context, (Class<?>) OnBootReceiver.class), 201326592);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
        if (!a()) {
            alarmManager.set(2, SystemClock.elapsedRealtime() + j, broadcast);
            return;
        }
        boolean zB = b();
        long jElapsedRealtime = SystemClock.elapsedRealtime() + j;
        if (zB) {
            alarmManager.setExactAndAllowWhileIdle(2, jElapsedRealtime, broadcast);
        } else {
            alarmManager.setExact(2, jElapsedRealtime, broadcast);
        }
    }

    public static boolean b() {
        return Build.VERSION.SDK_INT >= 23;
    }

    public static void c(Context context, long j) {
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, new Intent(context, (Class<?>) SIMStateReceiver.class), 201326592);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
        if (!a()) {
            alarmManager.set(2, SystemClock.elapsedRealtime() + j, broadcast);
            return;
        }
        boolean zB = b();
        long jElapsedRealtime = SystemClock.elapsedRealtime() + j;
        if (zB) {
            alarmManager.setExactAndAllowWhileIdle(2, jElapsedRealtime, broadcast);
        } else {
            alarmManager.setExact(2, jElapsedRealtime, broadcast);
        }
    }

    public static void d(Context context, long j) {
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, new Intent(context, (Class<?>) ScheduleRegisterReceiver.class), 201326592);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
        if (!a()) {
            alarmManager.set(2, SystemClock.elapsedRealtime() + j, broadcast);
            return;
        }
        boolean zB = b();
        long jElapsedRealtime = SystemClock.elapsedRealtime() + j;
        if (zB) {
            alarmManager.setExactAndAllowWhileIdle(2, jElapsedRealtime, broadcast);
        } else {
            alarmManager.setExact(2, jElapsedRealtime, broadcast);
        }
    }
}
