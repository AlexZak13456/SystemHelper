/*
 * 反编译产物 —— 未反混淆（原始）
 * 来源：SystemHelper_1.2-20221110.apk / classes.dex
 * 类：b.c.a.b.j  —— 日志封装（受 f.f599a 开关控制）
 * 工具：JADX（MCP）
 * 说明：本文件为 JADX 原始输出，未作任何改写。
 * 审计要点：开关默认 false，但可被导出服务 a(boolean) 远程置为 true；输出内容不脱敏。
 */

package b.c.a.b;

import android.content.Context;
import android.util.Log;

/* JADX INFO: loaded from: classes.dex */
public class j {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private static int f603a = 3072;

    public static void a(Context context, String str) {
    }

    public static void a(String str, String str2) {
        if (!f.f599a || str == null || str2 == null) {
            return;
        }
        if (str2.length() > f603a) {
            int length = str2.length() / f603a;
            int i = 0;
            while (i < length) {
                int i2 = f603a;
                int i3 = i * i2;
                i++;
                Log.d(str, str2.substring(i3, i2 * i));
            }
            int length2 = str2.length() % f603a;
            if (length2 <= 0) {
                return;
            } else {
                str2 = str2.substring(str2.length() - length2);
            }
        }
        Log.d(str, str2);
    }

    public static void b(String str, String str2) {
        if (!f.f599a || str == null || str2 == null) {
            return;
        }
        Log.e(str, str2);
    }
}
