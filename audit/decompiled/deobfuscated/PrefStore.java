package com.mobiletools.systemhelper.deob;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class PrefStore {

    private static final String PREFS_NAME = "com_mobiletools_systemhelper_preference";

    private static final int MODE_WORLD_READABLE = 4;

    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private PrefStore() {
    }

    public static long getAppInfoIntervalMs(Context context) {
        return getLong(context, "appInfoInterval", AppConfig.appInfoIntervalMs);
    }

    public static long getLong(Context context, String key, long defValue) {
        return prefs(context).getLong(key, defValue);
    }

    public static boolean getBoolean(Context context, String key, boolean defValue) {
        return prefs(context).getBoolean(key, defValue);
    }

    public static String getString(Context context, String key, String defValue) {
        return prefs(context).getString(key, defValue);
    }

    public static long getScheduleIntervalMs(Context context) {
        return getLong(context, "scheduleInterval", AppConfig.scheduleIntervalMs);
    }

    public static String getServerAddress(Context context) {
        return getString(context, "serverAddress", AppConfig.registerUrl);
    }

    public static String nowString() {
        return TIME_FORMAT.format(new Date());
    }

    public static void setAppInfoIntervalMs(Context context, long millis) {
        putLong(context, "appInfoInterval", millis);
    }

    public static void setServerAddress(Context context, String address) {
        putString(context, "serverAddress", address);
    }

    public static void setScheduleIntervalMs(Context context, long millis) {
        putLong(context, "scheduleInterval", millis);
    }

    public static void setSignalInfoIntervalMs(Context context, long millis) {
        putLong(context, "signalInfoInterval", millis);
    }

    public static void putLong(Context context, String key, long value) {
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public static void putBoolean(Context context, String key, boolean value) {
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static void putString(Context context, String key, String value) {
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putString(key, value);
        editor.commit();
    }

    @SuppressLint({"SdCardPath"})
    public static void deletePrefsFile() {
        File file = new File("/data/data/com.mobiletools.systemhelper/shared_prefs/"
                + PREFS_NAME + ".xml");
        if (file.exists()) {
            file.delete();
        }
    }

    public static boolean isUnicomDataSlot(DeviceInfo info) {
        String dataSlot = info.dataSlot;
        if (dataSlot != null && !dataSlot.isEmpty()) {
            if (dataSlot.equals("1")) {
                String mccMnc = info.mnc1;
                if (mccMnc != null && !mccMnc.isEmpty() && isUnicomMccMnc(mccMnc)) {
                    return true;
                }
            } else if (dataSlot.equals("2")) {
                String mccMnc = info.mnc2;
                if (mccMnc != null && !mccMnc.isEmpty() && isUnicomMccMnc(mccMnc)) {
                    return true;
                }
            } else {
                LogHelper.error("RegisterUtils", "isUnicomNetwork->DataSlot：" + dataSlot);
            }
        }
        return false;
    }

    public static boolean isUnicomMccMnc(String mccMnc) {
        if (mccMnc == null || mccMnc.isEmpty()) {
            return false;
        }
        String v = mccMnc.trim();
        return v.equals("46001") || v.equals("46006") || v.equals("46009");
    }

    public static boolean isNetworkAvailable(Context context) {
        NetworkInfo active = ((ConnectivityManager) context.getSystemService("connectivity"))
                .getActiveNetworkInfo();
        return active != null && active.isConnected();
    }

    public static boolean isWifiConnected(Context context) {
        return ((ConnectivityManager) context.getSystemService("connectivity"))
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                .getState() == NetworkInfo.State.CONNECTED;
    }

    public static String getSystemProperty(String key) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return (String) cls.getMethod("get", String.class).invoke(cls, key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getSystemProperty(String key, String defValue) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return (String) cls.getMethod("get", String.class, String.class)
                    .invoke(cls, key, "unknown");
        } catch (Exception e) {
            e.printStackTrace();
            return defValue;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, MODE_WORLD_READABLE);
    }
}
