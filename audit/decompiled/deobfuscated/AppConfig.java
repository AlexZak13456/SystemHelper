package com.mobiletools.systemhelper.deob;

import android.content.Context;
import android.telephony.TelephonyManager;

public final class AppConfig {

    public static b.a.b.a.a greeTelephonyProxy = null;
    public static b.b.a.a nubiaTelephonyProxy = null;

    public static boolean logEnabled = false;

    public static boolean fetchIpBeforeUpload = false;

    public static boolean probeVolteForUnicom = false;

    public static boolean dynamicXiaomiModelMatch = false;

    public static boolean reportSignalInfo = false;

    public static Context appContext = null;

    public static boolean userConsentGranted = true;

    public static TelephonyManager telephonySlot1 = null;
    public static TelephonyManager telephonySlot2 = null;
    public static com.mobiletools.systemhelper.control.a phoneStateListener1 = null;
    public static com.mobiletools.systemhelper.control.a phoneStateListener2 = null;
    public static com.mobiletools.systemhelper.control.b telephonyCallback1 = null;
    public static com.mobiletools.systemhelper.control.b telephonyCallback2 = null;

    public static String networkTypeSlot1 = "";
    public static String networkTypeSlot2 = "";
    public static String reserved1 = "";
    public static String reserved2 = "";
    public static String reserved3 = "";
    public static String reserved4 = "";

    public static int networkWaitRetry = 2;

    public static boolean simReceiverRegistered = false;
    public static boolean connectReceiverRegistered = false;

    public static String registerUrl = "http://dm.wo.com.cn:18080/registerv3";

    public static String publicIpUrl = "http://dm.wo.com.cn:18080/public/ip";

    public static long scheduleIntervalMs = 86400000L;

    public static long appInfoIntervalMs = 604800000L;
}
