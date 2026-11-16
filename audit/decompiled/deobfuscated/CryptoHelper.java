package com.mobiletools.systemhelper.deob;

import android.content.Context;
import android.util.Base64;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public final class CryptoHelper {

    private CryptoHelper() {
    }

    public static String base64(byte[] data) {
        return Base64.encodeToString(data, 0, data.length, 0);
    }

    public static byte[] rsaEncryptWithAssetKey(Context context, byte[] data) {
        RsaHelper helper = new RsaHelper();
        try {
            InputStream pubCert = context.getResources().getAssets().open("pub.cer");
            Key publicKey = helper.loadPublicKey(pubCert);
            return helper.encrypt(publicKey, data);
        } catch (Exception e) {
            // The original implementation did not catch IOException; preserve the equivalent behavior of swallowing the exception and returning null.
            return null;
        }
    }

    public static byte[] md5(String text) {
        try {
            return MessageDigest.getInstance("MD5").digest(text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Huh, UTF-8 should be supported?", e);
        } catch (NoSuchAlgorithmException e2) {
            throw new RuntimeException("Huh, MD5 should be supported?", e2);
        }
    }

    public static byte[] aesEncrypt(String plaintext, byte[] key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(plaintext.getBytes("utf-8"));
        } catch (BadPaddingException | IllegalBlockSizeException
                 | UnsupportedEncodingException | NoSuchAlgorithmException
                 | InvalidKeyException | NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        } catch (Throwable unused) {
            return null;
        }
    }

    public static class RsaHelper {

        public Key loadPublicKey(InputStream certificateStream) {
            try {
                X509Certificate cert = (X509Certificate) CertificateFactory
                        .getInstance("X.509")
                        .generateCertificate(certificateStream);
                return cert.getPublicKey();
            } catch (CertificateException e) {
                e.printStackTrace();
                LogHelper.error("RSAHelper", e.getMessage());
                return null;
            }
        }

        public byte[] encrypt(Key key, byte[] data) {
            if (key == null) {
                return null;
            }
            try {
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.ENCRYPT_MODE, key);
                return cipher.doFinal(data);
            } catch (Exception e) {
                LogHelper.error("RSAHelper", e.getMessage());
                return null;
            }
        }
    }
}
