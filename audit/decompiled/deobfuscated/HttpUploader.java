package com.mobiletools.systemhelper.deob;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUploader {

    private HttpURLConnection connection = null;
    private int lastResponseCode = 0;

    public void disconnect() {
        HttpURLConnection conn = this.connection;
        if (conn != null) {
            conn.disconnect();
        }
    }

    public boolean upload(String url, byte[] body, byte[] rsaWrappedSessionKey) {
        LogHelper.debug("HttpEncryptHelper", "sendPOSTRequest() path = " + url);
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            this.connection = conn;
            conn.setConnectTimeout(10000);
            this.connection.setReadTimeout(15000);
            this.connection.setRequestMethod("POST");
            boolean success = true;
            this.connection.setDoOutput(true);
            this.connection.setDoInput(true);
            this.connection.setRequestProperty("Content-Type", "application/encrypted-json");
            this.connection.setRequestProperty("Content-Length", String.valueOf(body.length));

            OutputStream out = this.connection.getOutputStream();
            out.write(body);
            out.flush();
            out.close();

            this.lastResponseCode = this.connection.getResponseCode();
            LogHelper.debug("HttpEncryptHelper", "server responseCode-->" + this.lastResponseCode);
            if (this.lastResponseCode == 200) {
                LogHelper.debug("HttpEncryptHelper", "register success!");
            } else {
                LogHelper.error("HttpEncryptHelper", "register fail!");
                success = false;
            }
            return success;
        } finally {
            disconnect();
        }
    }

    public int lastResponseCode() {
        return this.lastResponseCode;
    }
}
