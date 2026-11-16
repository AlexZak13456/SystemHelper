/*
 * 反编译产物 —— 未反混淆（原始）
 * 来源：SystemHelper_1.2-20221110.apk / classes.dex
 * 类：b.c.a.b.k  —— RSA 加密（读取 assets/pub.cer 公钥）
 * 工具：JADX（MCP）
 * 说明：本文件为 JADX 原始输出，未作任何改写。
 */

package b.c.a.b;

import java.io.InputStream;
import java.security.Key;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.crypto.Cipher;

/* JADX INFO: loaded from: classes.dex */
public class k {
    public Key a(InputStream inputStream) {
        try {
            return ((X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(inputStream)).getPublicKey();
        } catch (CertificateException e) {
            e.printStackTrace();
            j.b("RSAHelper", e.getMessage());
            return null;
        }
    }

    public byte[] a(Key key, byte[] bArr) {
        if (key == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(1, key);
            return cipher.doFinal(bArr);
        } catch (Exception e) {
            j.b("RSAHelper", e.getMessage());
            return null;
        }
    }
}
