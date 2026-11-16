/*
 * 反编译产物 —— 未反混淆（原始·节选）
 * 来源：SystemHelper_1.2-20221110.apk / classes.dex
 * 类：b.c.a.a.f1  —— 设备信息工厂 + 机型路由
 * 工具：JADX（MCP）
 *
 * 【重要说明】本类原始反编译输出约 1,000 行，其中约 800 行为
 *   Build.MODEL → 厂商代号 的静态映射表（200+ 条）。
 *   为保证可读性，本文件对以下两处作节选，其余代码完整保真：
 *     · b(Context) 的 if-else 分支链：保留前 25 条
 *     · c() 的 f583a.put(...)：保留前 40 条
 *   省略处均以 "/* 省略 N 条 *\/" 标注，未修改任何标识符名称。
 *
 * 审计要点：按 Build.MODEL 精确匹配机型，命中后实例化对应的 DeviceInfoReader 子类；
 *   未命中则回落到通用实现 b.c.a.a.c。该表实质上是一份运营商定制机型清单。
 */

package b.c.a.a;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;

/* JADX INFO: loaded from: classes.dex */
public class f1 {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public static Map<String, String> f583a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private static c f584b;
    private static List<b.c.a.b.b> c;
    private static com.mobiletools.systemhelper.control.e.a d = new a();
    private static com.mobiletools.systemhelper.control.d.a e = new b();

    static class a implements com.mobiletools.systemhelper.control.e.a {
        a() {
        }

        @Override // com.mobiletools.systemhelper.control.e.a
        public void a(Context context, com.mobiletools.systemhelper.control.c cVar, List<b.c.a.b.b> list) {
            List unused = f1.c = list;
            if (f1.c != null) {
                JSONArray jSONArray = new JSONArray();
                Iterator it = f1.c.iterator();
                while (it.hasNext()) {
                    jSONArray.put(((b.c.a.b.b) it.next()).a());
                }
                if (cVar != null) {
                    cVar.K = jSONArray;
                    b.c.a.b.j.a("DeviceManager", "appInfo:" + jSONArray.toString());
                    b.c.a.b.j.a("DeviceManager", "appInfolength:" + jSONArray.length());
                }
            }
            (b.c.a.b.f.f600b ? new com.mobiletools.systemhelper.control.d(context, cVar, f1.e) : new com.mobiletools.systemhelper.control.g(context, cVar)).start();
        }
    }

    static class b implements com.mobiletools.systemhelper.control.d.a {
        b() {
        }

        @Override // com.mobiletools.systemhelper.control.d.a
        public void a(Context context, com.mobiletools.systemhelper.control.c cVar, String str, String str2) {
            if (cVar != null) {
                cVar.C = str;
                cVar.D = str2;
            }
            b.c.a.b.j.a("DeviceManager", "Ipv4:" + cVar.C + ", Ipv6:" + cVar.D);
            new com.mobiletools.systemhelper.control.g(context, cVar).start();
        }
    }

    public interface c {
        void a(Context context, com.mobiletools.systemhelper.control.c cVar);

        boolean a(Context context);
    }

    public static synchronized com.mobiletools.systemhelper.control.c a(Context context) {
        com.mobiletools.systemhelper.control.c cVar;
        b.c.a.b.j.a("DeviceManager", "enter getCurDeviceInfo");
        c();
        cVar = new com.mobiletools.systemhelper.control.c();
        c(context, cVar);
        a(context, cVar);
        b(context, cVar);
        a(cVar);
        (Build.VERSION.SDK_INT >= 23 ? new com.mobiletools.systemhelper.control.e(context, cVar, d) : new com.mobiletools.systemhelper.control.g(context, cVar)).start();
        return cVar;
    }

    public static synchronized com.mobiletools.systemhelper.control.c a(Context context, String str) {
        Thread gVar;
        b.c.a.b.j.a("DeviceManager", "enter getCurDeviceInfo--RegType:" + str);
        com.mobiletools.systemhelper.control.c cVar = new com.mobiletools.systemhelper.control.c();
        cVar.G = str;
        c(context, cVar);
        if (!str.trim().equals("T")) {
            a(context, cVar);
            b(context, cVar);
        } else if (!a(context, cVar) && !b(context, cVar)) {
            b.c.a.b.j.a("DeviceManager", "SIM not changed，not post！");
            return cVar;
        }
        a(cVar);
        if (Build.VERSION.SDK_INT < 23 || !str.trim().equals("A")) {
            if (Build.VERSION.SDK_INT < 23 || !str.trim().equals("R")) {
                gVar = new com.mobiletools.systemhelper.control.g(context, cVar);
            } else {
                new com.mobiletools.systemhelper.control.f(context, cVar).start();
                b.c.a.b.j.a("DeviceManager", "deviceinfo.signalinfo:" + cVar.M);
            }
            return cVar;
        }
        gVar = new com.mobiletools.systemhelper.control.e(context, cVar, d);
        gVar.start();
        return cVar;
    }

    private static synchronized void a(com.mobiletools.systemhelper.control.c cVar) {
        String str;
        if (cVar.g != null && cVar.q != null && !cVar.g.isEmpty() && !cVar.q.isEmpty() && cVar.g.compareTo(cVar.q) > 0) {
            com.mobiletools.systemhelper.control.c cVar2 = new com.mobiletools.systemhelper.control.c();
            cVar2.g = cVar.g;
            cVar2.h = cVar.h;
            cVar2.i = cVar.i;
            cVar2.j = cVar.j;
            cVar2.k = cVar.k;
            cVar2.l = cVar.l;
            cVar2.m = cVar.m;
            cVar2.n = cVar.n;
            cVar2.o = cVar.o;
            cVar2.p = cVar.p;
            String str2 = cVar.q;
            cVar2.q = str2;
            String str3 = cVar.r;
            cVar2.r = str3;
            String str4 = cVar.s;
            cVar2.s = str4;
            String str5 = cVar.t;
            cVar2.t = str5;
            String str6 = cVar.u;
            cVar2.u = str6;
            String str7 = cVar.v;
            cVar2.v = str7;
            String str8 = cVar.w;
            cVar2.w = str8;
            String str9 = cVar.x;
            cVar2.x = str9;
            String str10 = cVar.y;
            cVar2.y = str10;
            String str11 = cVar.z;
            cVar2.z = str11;
            cVar.g = str2;
            cVar.h = str3;
            cVar.i = str4;
            cVar.j = str5;
            cVar.k = str6;
            cVar.l = str7;
            cVar.m = str8;
            cVar.n = str9;
            cVar.o = str10;
            cVar.p = str11;
            cVar.q = cVar2.g;
            cVar.r = cVar2.h;
            cVar.s = cVar2.i;
            cVar.t = cVar2.j;
            cVar.u = cVar2.k;
            cVar.v = cVar2.l;
            cVar.w = cVar2.m;
            cVar.x = cVar2.n;
            cVar.y = cVar2.o;
            cVar.z = cVar2.p;
            if (cVar.A != null) {
                if (!cVar.A.equals("1")) {
                    str = cVar.A.equals("2") ? "1" : "2";
                }
                cVar.A = str;
            }
        }
        if (cVar.k != null && !cVar.k.equals("") && !b.c.a.b.l.b(cVar.k).booleanValue()) {
            cVar.h = b.c.a.b.h.a(cVar.h);
            cVar.j = b.c.a.b.h.a(cVar.j);
            cVar.i = b.c.a.b.h.a(cVar.i);
            cVar.m = b.c.a.b.h.a(cVar.m);
            cVar.n = b.c.a.b.h.a(cVar.n);
        }
        if (cVar.u != null && !cVar.u.equals("") && !b.c.a.b.l.b(cVar.u).booleanValue()) {
            cVar.r = b.c.a.b.h.a(cVar.r);
            cVar.t = b.c.a.b.h.a(cVar.t);
            cVar.s = b.c.a.b.h.a(cVar.s);
            cVar.w = b.c.a.b.h.a(cVar.w);
            cVar.x = b.c.a.b.h.a(cVar.x);
        }
    }

    private static synchronized boolean a(Context context, com.mobiletools.systemhelper.control.c cVar) {
        String strA = b.c.a.b.l.a(context, "iccid1", (String) null);
        if (strA == null || strA.isEmpty()) {
            if (cVar.h != null && !cVar.h.isEmpty()) {
                b.c.a.b.j.a("DeviceManager", "SIM1 Changed,old iccid:" + strA + ", new iccid:" + cVar.h);
                b.c.a.b.l.b(context, "iccid1", cVar.h);
                return true;
            }
        } else if (!strA.equals(cVar.h)) {
            b.c.a.b.j.a("DeviceManager", "SIM1 Changed,old iccid:" + strA + ", new iccid:" + cVar.h);
            b.c.a.b.l.b(context, "iccid1", cVar.h);
            return true;
        }
        return false;
    }

    public static c b(Context context) {
        c cVar;
        if (Build.VERSION.SDK_INT >= 30) {
            b.c.a.a.c.b(context);
        }
        c cVar2 = f584b;
        if (cVar2 != null) {
            return cVar2;
        }
        b.c.a.b.j.a(b.c.a.b.f.f, "model is " + Build.MODEL);
        String str = f583a.get(Build.MODEL);
        if (str == null) {
            b.c.a.b.j.b("DeviceManager", "getReader error!! manufacture==null! model=" + Build.MODEL);
            cVar = new b.c.a.a.c();
        } else if (str.equals("LETV")) {
            cVar = new x();
        } else if (str.equals("HUAWEI")) {
            cVar = new p();
        } else if (str.equals("BROR")) {
            cVar = new b.c.a.a.a();
        } else if (str.equals("Xiaomi")) {
            cVar = new b1();
        } else if (str.equals("Onefive")) {
            cVar = new g0();
        } else if (str.equals("OYSIN")) {
            cVar = new j0();
        } else if (str.equals("meizu")) {
            cVar = new a0();
        } else if (str.equals("BenWee")) {
            cVar = new e();
        } else if (str.equals("INTKI")) {
            cVar = new r();
        } else if (str.equals("QIKU")) {
            cVar = new q0();
        } else if (str.equals("EAMEY")) {
            cVar = new j();
        } else if (str.equals("ORANGE")) {
            cVar = new i0();
        } else if (str.equals("K-Touch")) {
            cVar = new v();
        } else if (str.equals("Shown")) {
            cVar = new s0();
        } else if (str.equals("YuLong")) {
            cVar = new d1();
        } else if (str.equals("Hisense")) {
            cVar = new o();
        } else if (str.equals("ZTE")) {
            cVar = new e1();
        } else if (str.equals("kingsun")) {
            cVar = new t();
        } else if (str.equals("KONKA")) {
            cVar = new u();
        } else if (str.equals("lephone")) {
            cVar = new w();
        } else if (str.equals("SINTAVE")) {
            cVar = new t0();
        } else if (str.equals("TCL")) {
            cVar = new x0();
        } else if (str.equals("PPTV")) {
            cVar = new n0();
        } else if (str.equals("BestSonny")) {
            cVar = new f();
        } else if (str.equals("SOP")) {
            cVar = new u0();
        /* 省略约 30 条分支：OPPO / PHICOMM / Spreadtrum / BBK(vivo) / CHANGHONG / PHILIPS /
           Bee / ChuiZi / PROTRULY / Haipainoble / NOAIN / mfox / gionee / Xiaolajiao / moto /
           nubia / sprd / Lenovo / EWPE / samsung / realme / KONKA / OnePlus / 5G HYUN /
           Liantong / H10 / Coolpad / YIK / TOSOT / JingLing / TDTECH / PTAC / HZTL */
        } else {
            cVar = str.equalsIgnoreCase("HZTL") ? new q() : new b.c.a.a.c();
        }
        f584b = cVar;
        return f584b;
    }

    private static synchronized boolean b(Context context, com.mobiletools.systemhelper.control.c cVar) {
        String strA = b.c.a.b.l.a(context, "iccid2", (String) null);
        if (strA == null || strA.isEmpty()) {
            if (cVar.r != null && !cVar.r.isEmpty()) {
                b.c.a.b.j.a("DeviceManager", "SIM2 Changed,old iccid:" + strA + ", new iccid:" + cVar.r);
                b.c.a.b.l.b(context, "iccid2", cVar.r);
                return true;
            }
        } else if (!strA.equals(cVar.r)) {
            b.c.a.b.j.a("DeviceManager", "SIM2 Changed,old iccid:" + strA + ", new iccid:" + cVar.r);
            b.c.a.b.l.b(context, "iccid2", cVar.r);
            return true;
        }
        return false;
    }

    public static void c() {
        if (f583a == null) {
            f583a = new HashMap();
            String strA = b.c.a.b.l.a("ro.product.manufacturer");
            if (strA != null && !strA.isEmpty()) {
                b.c.a.b.j.a("DeviceManager", "manufacturer=" + strA);
                if (strA.trim().equalsIgnoreCase("vivo") || strA.trim().equalsIgnoreCase("samsung") || strA.trim().equalsIgnoreCase("realme")) {
                    f583a.put(Build.MODEL, strA);
                }
            }
            f583a.put("SM-G9750", "samsung");
            f583a.put("SM-N9760", "samsung");
            f583a.put("SM-N9700", "samsung");
            f583a.put("SM-A5070", "samsung");
            f583a.put("SM-A9080", "samsung");
            f583a.put("SM-T735C", "samsung");
            f583a.put("EVR-AN00", "HUAWEI");
            f583a.put("V1911A", "vivo");
            f583a.put("V1916A", "vivo");
            f583a.put("V1922A", "vivo");
            f583a.put("V1923A", "vivo");
            f583a.put("V1924A", "vivo");
            f583a.put("V1930A", "vivo");
            f583a.put("V1901A", "vivo");
            f583a.put("V1921A", "vivo");
            f583a.put("V1914A", "vivo");
            f583a.put("V1936A", "vivo");
            f583a.put("V1913A", "vivo");
            f583a.put("V1934A", "vivo");
            f583a.put("V1932A", "vivo");
            f583a.put("V1941A", "vivo");
            f583a.put("V1945A", "vivo");
            f583a.put("V1938A", "vivo");
            f583a.put("V1936AL", "vivo");
            f583a.put("V1938CA", "vivo");
            f583a.put("V1938T", "vivo");
            f583a.put("V1938CT", "vivo");
            if (Build.VERSION.SDK_INT >= 30 && b.c.a.b.f.d && strA.trim().equalsIgnoreCase("Xiaomi")) {
                f583a.put(Build.MODEL, strA);
            }
            f583a.put("M1908F1XE", "Xiaomi");
            f583a.put("Mi9 Pro 5G", "Xiaomi");
            f583a.put("M1906G7T", "Xiaomi");
            f583a.put("Redmi Note 8 Pro", "Xiaomi");
            f583a.put("M1908C3IC", "Xiaomi");
            f583a.put("Redmi 8", "Xiaomi");
            f583a.put("M1908C3KE", "Xiaomi");
            f583a.put("Redmi 8A", "Xiaomi");
            f583a.put("M1908C3JC", "Xiaomi");
            f583a.put("Redmi Note 8", "Xiaomi");
            f583a.put("M1910F4E", "Xiaomi");
            f583a.put("MI CC9 Pro", "Xiaomi");
            f583a.put("MI CC9 Pro Premium Edition", "Xiaomi");
            f583a.put("M1901F7BE", "Xiaomi");
            /* 省略约 160 条机型映射：小米/Redmi 全系、OPPO 全系、realme、一加、
               中兴、努比亚、联想、海信、酷派、魅族、金立、TCL、格力、中国移动
               定制机等，直至 "TAI-M01" → "HZTL" */
            f583a.put("TAI-M01", "HZTL");
        }
    }

    private static synchronized void c(Context context, com.mobiletools.systemhelper.control.c cVar) {
        String str;
        try {
            cVar.I = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e2) {
            e2.printStackTrace();
        }
        c();
        c cVarB = b(context);
        if (cVarB == null) {
            b.c.a.b.j.b("DeviceManager", "manuf==null,model=" + Build.MODEL);
            return;
        }
        cVarB.a(context, cVar);
        if (cVar.i == null || (cVar.i != null && cVar.i.trim().length() == 0)) {
            cVar.i = null;
        }
        if (cVar.s == null || (cVar.s != null && cVar.s.trim().length() == 0)) {
            cVar.s = null;
        }
        if (a.b.c.a.a.a(context, "android.permission.ACCESS_NETWORK_STATE") == 0) {
            if (b.c.a.b.l.d(context).booleanValue()) {
                str = !b.c.a.b.l.e(context).booleanValue() ? "Mobile" : "WiFi";
            } else {
                str = "Disconnected";
            }
            cVar.B = str;
        }
        cVar.J = b.c.a.b.l.b();
        b.c.a.b.j.a("DeviceManager", "Clear Text mDeivceInfo:" + cVar.a());
    }

    public static boolean c(Context context) {
        c cVarB;
        if (context == null || (cVarB = b(context)) == null) {
            return false;
        }
        return cVarB.a(context);
    }

    public static Boolean d(Context context) {
        boolean zC;
        if (context == null) {
            zC = false;
        } else {
            c();
            zC = c(context);
        }
        return Boolean.valueOf(zC);
    }
}
