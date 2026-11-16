/*
 * 反编译产物 —— 未反混淆（原始）
 * 来源：SystemHelper_1.2-20221110.apk / classes.dex
 * 类：b.c.a.b.g  —— HTTP POST 上报（application/encrypted-json）
 * 工具：JADX（MCP）
 * 说明：本文件为 JADX 原始输出，未作任何改写。
 * 审计要点：方法 a(String, byte[], byte[]) 的第三个参数 bArr2 未被使用。
 */

package b.c.a.b;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/* JADX INFO: loaded from: classes.dex */
public class g {
    private HttpURLConnection f601a = null;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private int f602b = 0;

    public void a() {
        HttpURLConnection httpURLConnection = this.f601a;
        if (httpURLConnection != null) {
            httpURLConnection.disconnect();
        }
    }

    public boolean a(String str, byte[] bArr, byte[] bArr2) {
        j.a("HttpEncryptHelper", "sendPOSTRequest() path = " + str);
        try {
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
            this.f601a = httpURLConnection;
            httpURLConnection.setConnectTimeout(10000);
            this.f601a.setReadTimeout(15000);
            this.f601a.setRequestMethod("POST");
            boolean z = true;
            this.f601a.setDoOutput(true);
            this.f601a.setDoInput(true);
            this.f601a.setRequestProperty("Content-Type", "application/encrypted-json");
            this.f601a.setRequestProperty("Content-Length", String.valueOf(bArr.length));
            OutputStream outputStream = this.f601a.getOutputStream();
            outputStream.write(bArr);
            outputStream.flush();
            outputStream.close();
            this.f602b = this.f601a.getResponseCode();
            j.a("HttpEncryptHelper", "server responseCode-->" + String.valueOf(this.f602b));
            if (this.f602b == 200) {
                j.a("HttpEncryptHelper", "register success!");
            } else {
                j.b("HttpEncryptHelper", "register fail!");
                z = false;
            }
            return z;
        } finally {
            a();
        }
    }

    public int b() {
        return this.f602b;
    }
}
