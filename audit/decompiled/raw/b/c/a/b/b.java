/*
 * 反编译产物 —— 未反混淆（原始）
 * 来源：SystemHelper_1.2-20221110.apk / classes.dex
 * 类：com.mobiletools.systemhelper.control.b  —— AppInfo 单项模型
 * 工具：JADX（MCP）
 * 说明：本文件为 JADX 原始输出，未作任何改写。
 * 审计要点：a() 序列化的字段即每台设备上每个已安装应用的名称、包名、前台时长与分卡流量。
 *   注意 d(long) 与 j(long) 为空实现，导致 totalData1 / totalDataWiFi 恒为 0（数据失真）。
 */

package b.c.a.b;

import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class b {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private String f595a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private String f596b;
    private long c = 0;
    private long d = 0;
    private long e = 0;
    private long f = 0;
    private int g = -1;
    private long h = 0;
    private long i = 0;
    private long j = 0;
    private long k = 0;

    public JSONObject a() {
        JSONObject jSONObject = new JSONObject();
        try {
            if (this.f596b == null) {
                this.f596b = "";
            }
            jSONObject.put("appLabel", this.f596b);
            if (this.f595a == null) {
                this.f595a = "";
            }
            jSONObject.put("pkgName", this.f595a);
            jSONObject.put("runTime", this.c + "min");
            jSONObject.put("tx1", this.h + "KB");
            jSONObject.put("rx1", this.i + "KB");
            jSONObject.put("tx2", this.j + "KB");
            jSONObject.put("rx2", this.k + "KB");
            jSONObject.put("totalData1", this.d + "KB");
            jSONObject.put("totalData2", this.e + "KB");
            jSONObject.put("totalDataWiFi", this.f + "MB");
        } catch (JSONException e) {
            j.b("AppInfo", "getJsonObject->" + e.getMessage());
        }
        return jSONObject;
    }

    public void a(int i) {
        this.g = i;
    }

    public void a(long j) {
        this.c = j;
    }

    public void a(String str) {
        this.f596b = str;
    }

    public int b() {
        return this.g;
    }

    public void b(long j) {
        this.i = j;
    }

    public void b(String str) {
        this.f595a = str;
    }

    public void c(long j) {
        this.k = j;
    }

    public void d(long j) {
    }

    public void e(long j) {
        this.d = j;
    }

    public void f(long j) {
        this.e = j;
    }

    public void g(long j) {
        this.f = j;
    }

    public void h(long j) {
        this.h = j;
    }

    public void i(long j) {
        this.j = j;
    }

    public void j(long j) {
    }
}
