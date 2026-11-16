/*
 * 反编译产物 —— 未反混淆（原始）
 * 来源：SystemHelper_1.2-20221110.apk / classes.dex
 * 类：b.c.a.b.c  —— 加密原语（MD5 / AES-ECB / RSA 封装入口）
 * 工具：JADX（MCP）
 * 说明：本文件为 JADX 原始输出，未作任何改写。
 */

package b.c.a.b;

import android.content.Context;
import android.util.Base64;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/* JADX INFO: loaded from: classes.dex */
public class c {
    public static String a(byte[] bArr) {
        return Base64.encodeToString(bArr, 0, bArr.length, 0);
    }

    public static byte[] a(Context context, byte[] bArr) {
        k kVar = new k();
        return kVar.a(kVar.a(context.getResources().getAssets().open("pub.cer")), bArr);
    }

    public static byte[] a(String str) {
        try {
            return MessageDigest.getInstance("MD5").digest(str.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Huh, UTF-8 should be supported?", e);
        } catch (NoSuchAlgorithmException e2) {
            throw new RuntimeException("Huh, MD5 should be supported?", e2);
        }
    }

    public static byte[] a(String str, byte[] bArr) {
        try {
            try {
                try {
                    try {
                        try {
                            SecretKeySpec secretKeySpec = new SecretKeySpec(bArr, "AES");
                            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                            cipher.init(1, secretKeySpec);
                            return cipher.doFinal(str.getBytes("utf-8"));
                        } catch (BadPaddingException e) {
                            e.printStackTrace();
                            return null;
                        } catch (IllegalBlockSizeException e2) {
                            e2.printStackTrace();
                            return null;
                        }
                    } catch (UnsupportedEncodingException e3) {
                        e3.printStackTrace();
                        return null;
                    } catch (NoSuchAlgorithmException e4) {
                        e4.printStackTrace();
                        return null;
                    }
                } catch (InvalidKeyException e5) {
                    e5.printStackTrace();
                    return null;
                }
            } catch (NoSuchPaddingException e6) {
                e6.printStackTrace();
                return null;
            }
        } catch (Throwable unused) {
            return null;
        }
    }
}
