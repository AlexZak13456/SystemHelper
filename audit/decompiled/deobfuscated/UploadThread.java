package com.mobiletools.systemhelper.deob;

import android.content.Context;
import android.os.Build;

import java.net.URLEncoder;

public class UploadThread extends Thread {

    private Context context;
    private DeviceInfo deviceInfo;

    public UploadThread(Context context, DeviceInfo deviceInfo) {
        this.context = context;
        this.deviceInfo = deviceInfo;
    }

    private void onUploadResult(boolean uploaded) {
    }

    private synchronized boolean doUpload(DeviceInfo info) {
        PendingQueueDb historyDb = PendingQueueDb.open(this.context);
        info.history = Build.VERSION.SDK_INT >= 28 ? historyDb.readLarge() : historyDb.read();
        historyDb.close();

        String plainJson = info.toJson();

        LogHelper.debug("RegisterThread", "register postString：" + plainJson);

        boolean uploaded = false;
        try {
            byte[] jsonMd5 = CryptoHelper.md5(plainJson);

            String sign = URLEncoder.encode(
                    CryptoHelper.base64(CryptoHelper.rsaEncryptWithAssetKey(this.context, jsonMd5)),
                    "utf-8");

            String url = PrefStore.getServerAddress(this.context)
                    + "?ver=3.0&model=" + URLEncoder.encode(info.model, "utf-8")
                    + "&manuf=" + URLEncoder.encode(info.manufacturer, "utf-8")
                    + "&sign=" + sign;

            byte[] encryptedBody = CryptoHelper.aesEncrypt(plainJson, jsonMd5);

            try {
                HttpUploader uploader = new HttpUploader();
                boolean ok = uploader.upload(url, encryptedBody, jsonMd5);
                if (!ok && uploader.lastResponseCode() == 416) {
                    ok = true;
                }
                uploaded = ok;
            } catch (Exception e) {
                LogHelper.error("RegisterThread", "sendPOSTRequest exception--->" + e.toString());
            }

            if (uploaded) {
                PendingQueueDb db = PendingQueueDb.open(this.context);
                db.clear();
                db.close();
            } else {
                info.history = null;
                PendingQueueDb db = PendingQueueDb.open(this.context);
                db.enqueue(info.toJson());
                db.close();
            }
        } catch (Exception e) {
            LogHelper.error("RegisterThread", "doEncrpytPost catch exception " + e.getMessage());
            return false;
        }
        return uploaded;
    }

    private boolean isValidImei(String imei) {
        if (imei == null || imei.isEmpty()) {
            return true;
        }
        if (imei.length() != 15) {
            LogHelper.error("RegisterThread", "imei: " + imei + " length is not 15!");
            return false;
        }
        if (imei.matches("[0-9]+")) {
            return true;
        }
        LogHelper.error("RegisterThread", "imei: " + imei + " is not all number!");
        return false;
    }

    private synchronized boolean shouldUpload(DeviceInfo info) {
        if (!isValidImei(info.imei1) || !isValidImei(info.imei2)) {
            return false;
        }
        if (!PrefStore.isNetworkAvailable(this.context)) {
            enqueueOnly(info, "No network connected!");
            return false;
        }
        if (PrefStore.isWifiConnected(this.context)) {
            return doUpload(info);
        }
        if (!DeviceInfoFactory.isRoaming(this.context) && PrefStore.isUnicomDataSlot(info)) {
            return doUpload(info);
        }
        enqueueOnly(info, "isUnicomNetwork=false, not post!");
        return false;
    }

    private void enqueueOnly(DeviceInfo info, String reason) {
        PendingQueueDb db = PendingQueueDb.open(this.context);
        db.enqueue(info.toJson());
        db.close();
        LogHelper.debug("RegisterThread", reason);
    }

    @Override
    public void run() {
        if (AppConfig.userConsentGranted) {
            onUploadResult(shouldUpload(this.deviceInfo));
            return;
        }
        PendingQueueDb db = PendingQueueDb.open(this.context);
        db.enqueue(this.deviceInfo.toJson());
        db.close();
        LogHelper.debug("RegisterThread", "User Permission Closed!");
    }
}
