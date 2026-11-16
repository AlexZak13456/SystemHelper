/*
 * 反编译产物 —— 未反混淆（原始）
 * 来源：SystemHelper_1.2-20221110.apk / classes.dex
 * 类：b.c.a.a.c  —— 设备信息读取基类（各厂商适配器的父类）
 * 工具：JADX（MCP）
 * 说明：本文件为 JADX 原始输出，未作任何改写。
 * 审计要点：
 *   b(Context, c)  双卡 IMEI / IMSI / ICCID / 手机号 / 基站 / VoLTE / 5G 状态（Android 11+）
 *   f(Context, c)  Android 8-10 路径；d/e(Context, c) Android 6-7 路径；c(Context, c) Android 5-
 *   a(Context, int) 用反射调用 setAdvancedCallingSettingEnabled(true) 强制开启 VoLTE
 *   a(Context, int, c) 遍历 ServiceState 提取 CellIdentity，写入 CellID / LAC
 */

package b.c.a.a;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityTdscdma;
import android.telephony.CellIdentityWcdma;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
public class c implements f1.c {
    public static int b(int i) {
        Class<?> clsA = b.c.a.b.o.a("android.telephony.SubscriptionManager");
        int[] iArr = (int[]) b.c.a.b.o.a(b.c.a.b.o.a(clsA, "getSubId", (Class<?>[]) new Class[]{Integer.TYPE}), (Object) null, Integer.valueOf(i));
        return iArr != null ? iArr[0] : ((Integer) b.c.a.b.o.a(b.c.a.b.o.a(clsA, "getDefaultSubId", (Class<?>[]) new Class[0]), (Object) null, new Object[0])).intValue();
    }

    public static synchronized void b(Context context) {
        b.c.a.b.j.a("DeviceInfoReaderBase", "listenNetworkType---->");
        int iC = c(0);
        if (iC > 0 && b.c.a.b.f.h == null) {
            b.c.a.b.f.h = ((TelephonyManager) context.getSystemService("phone")).createForSubscriptionId(iC);
            if (Build.VERSION.SDK_INT >= 31) {
                if (b.c.a.b.f.l == null) {
                    b.c.a.b.j.a("DeviceInfoReaderBase", "registerTelephonyCallback--mDMTelephonyCallback1");
                    b.c.a.b.f.l = new com.mobiletools.systemhelper.control.b(context, 0);
                    b.c.a.b.f.h.registerTelephonyCallback(context.getMainExecutor(), b.c.a.b.f.l);
                }
            } else if (b.c.a.b.f.j == null) {
                b.c.a.b.f.j = new com.mobiletools.systemhelper.control.a(context, 0);
                b.c.a.b.f.h.listen(b.c.a.b.f.j, 1049856);
            }
        }
        int iC2 = c(1);
        if (iC2 > 0 && b.c.a.b.f.i == null) {
            b.c.a.b.f.i = ((TelephonyManager) context.getSystemService("phone")).createForSubscriptionId(iC2);
            if (Build.VERSION.SDK_INT >= 31) {
                if (b.c.a.b.f.m == null) {
                    b.c.a.b.j.a("DeviceInfoReaderBase", "registerTelephonyCallback--mDMTelephonyCallback2");
                    b.c.a.b.f.m = new com.mobiletools.systemhelper.control.b(context, 1);
                    b.c.a.b.f.i.registerTelephonyCallback(context.getMainExecutor(), b.c.a.b.f.m);
                }
            } else if (b.c.a.b.f.k == null) {
                b.c.a.b.f.k = new com.mobiletools.systemhelper.control.a(context, 1);
                b.c.a.b.f.i.listen(b.c.a.b.f.k, 1049856);
            }
        }
    }

    public static int c(int i) {
        int[] iArr = (int[]) b.c.a.b.o.a(b.c.a.b.o.a(b.c.a.b.o.a("android.telephony.SubscriptionManager"), "getSubId", (Class<?>[]) new Class[]{Integer.TYPE}), (Object) null, Integer.valueOf(i));
        if (iArr != null) {
            return iArr[0];
        }
        return -1;
    }

    public static synchronized void c(Context context) {
        b.c.a.b.j.a("DeviceInfoReaderBase", "unlistenNetworkType-->");
        if (Build.VERSION.SDK_INT >= 31 && b.c.a.b.f.l != null) {
            b.c.a.b.f.h.unregisterTelephonyCallback(b.c.a.b.f.l);
        } else if (b.c.a.b.f.j != null) {
            b.c.a.b.f.h.listen(b.c.a.b.f.j, 0);
        }
        b.c.a.b.f.h = null;
        b.c.a.b.f.l = null;
        b.c.a.b.f.j = null;
        if (Build.VERSION.SDK_INT >= 31 && b.c.a.b.f.m != null) {
            b.c.a.b.f.i.unregisterTelephonyCallback(b.c.a.b.f.m);
        } else if (b.c.a.b.f.k != null) {
            b.c.a.b.f.i.listen(b.c.a.b.f.k, 0);
        }
        b.c.a.b.f.i = null;
        b.c.a.b.f.m = null;
        b.c.a.b.f.k = null;
    }

    public int a(TelephonyManager telephonyManager, int i) {
        try {
            int iIntValue = ((Integer) g1.a(telephonyManager, "getNetworkType", i)).intValue();
            int iIntValue2 = ((Integer) g1.a(telephonyManager, "getVoiceNetworkType", i)).intValue();
            if (iIntValue != 0) {
                return iIntValue;
            }
            if (iIntValue2 != 0) {
                return iIntValue2;
            }
            return 0;
        } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
            return 0;
        }
    }

    synchronized String a(int i, String str) {
        try {
            switch (i) {
                case 1:
                case 2:
                case 16:
                    return "GSM";
                case 3:
                case 8:
                case 9:
                case 10:
                case 15:
                    if (!"46000".equals(str) && !"46002".equals(str) && !"46007".equals(str)) {
                        return "WCDMA";
                    }
                    return "TD-SCDMA";
                case 4:
                case 5:
                case 6:
                case 7:
                case 12:
                case 14:
                    return "CDMA";
                case 11:
                case 18:
                default:
                    b.c.a.b.j.b("DeviceInfoReaderBase", "unknown networktype " + i);
                    return null;
                case 13:
                case 19:
                    return "LTE";
                case 17:
                    return "TD-SCDMA";
                case 20:
                    return "NR";
            }
        } catch (Throwable th) {
            throw th;
        }
    }

    protected synchronized String a(Context context, int i) {
        String str;
        str = "-1";
        ImsManager imsManager = (ImsManager) context.getSystemService("telephony_ims");
        int iC = c(i);
        boolean zIsAdvancedCallingSettingEnabled = false;
        if (iC >= 0) {
            ImsMmTelManager imsMmTelManager = imsManager.getImsMmTelManager(iC);
            if (!imsMmTelManager.isAdvancedCallingSettingEnabled()) {
                b.c.a.b.j.a("DeviceInfoReaderBase", "before open:false");
                b.c.a.b.o.a(b.c.a.b.o.a((Class<?>) ImsMmTelManager.class, "setAdvancedCallingSettingEnabled", (Class<?>[]) new Class[]{Boolean.TYPE}), imsMmTelManager, true);
            }
            zIsAdvancedCallingSettingEnabled = imsMmTelManager.isAdvancedCallingSettingEnabled();
            str = zIsAdvancedCallingSettingEnabled ? "1" : "0";
        }
        b.c.a.b.j.a("DeviceInfoReaderBase", "VoLTE state slotId:" + i + "，enable:" + zIsAdvancedCallingSettingEnabled);
        return str;
    }

    protected synchronized void a(Context context, int i, com.mobiletools.systemhelper.control.c cVar) {
        int lac;
        int tac;
        if (a.b.c.a.a.a(context, "android.permission.READ_PHONE_STATE") != 0) {
            b.c.a.b.j.b("DeviceInfoReaderBase", "setCellLoc no  READ_PHONE_STATE PERMISSION_GRANTED");
            return;
        }
        String strValueOf = "";
        String strValueOf2 = "";
        ServiceState serviceState = ((TelephonyManager) context.getSystemService("phone")).createForSubscriptionId(c(i)).getServiceState();
        if (serviceState == null) {
            b.c.a.b.j.a("DeviceInfoReaderBase", "setCellLoc failed!");
            return;
        }
        for (NetworkRegistrationInfo networkRegistrationInfo : serviceState.getNetworkRegistrationInfoList()) {
            if (networkRegistrationInfo.getDomain() == 2 && networkRegistrationInfo.getTransportType() == 1) {
                b.c.a.b.j.a("DeviceInfoReaderBase", "cellIdentity:" + networkRegistrationInfo.getCellIdentity());
                CellIdentity cellIdentity = networkRegistrationInfo.getCellIdentity();
                if (cellIdentity instanceof CellIdentityGsm) {
                    CellIdentityGsm cellIdentityGsm = (CellIdentityGsm) cellIdentity;
                    strValueOf = String.valueOf(cellIdentityGsm.getCid());
                    lac = cellIdentityGsm.getLac();
                } else if (cellIdentity instanceof CellIdentityWcdma) {
                    CellIdentityWcdma cellIdentityWcdma = (CellIdentityWcdma) cellIdentity;
                    strValueOf = String.valueOf(cellIdentityWcdma.getCid());
                    lac = cellIdentityWcdma.getLac();
                } else if (!(cellIdentity instanceof CellIdentityTdscdma)) {
                    if (!(cellIdentity instanceof CellIdentityCdma)) {
                        if (cellIdentity instanceof CellIdentityLte) {
                            CellIdentityLte cellIdentityLte = (CellIdentityLte) cellIdentity;
                            strValueOf = String.valueOf(cellIdentityLte.getCi());
                            tac = cellIdentityLte.getTac();
                        } else if (cellIdentity instanceof CellIdentityNr) {
                            CellIdentityNr cellIdentityNr = (CellIdentityNr) cellIdentity;
                            strValueOf = String.valueOf(cellIdentityNr.getNci());
                            tac = cellIdentityNr.getTac();
                        }
                        strValueOf2 = String.valueOf(tac);
                        break;
                    }
                    CellIdentityCdma cellIdentityCdma = (CellIdentityCdma) cellIdentity;
                    strValueOf = String.valueOf(cellIdentityCdma.getBasestationId());
                    int longitude = cellIdentityCdma.getLongitude();
                    int latitude = cellIdentityCdma.getLatitude();
                    if (longitude != Integer.MAX_VALUE && latitude != Integer.MAX_VALUE) {
                        strValueOf2 = String.format("%d:%d", Integer.valueOf(longitude), Integer.valueOf(latitude));
                        break;
                    }
                } else {
                    CellIdentityTdscdma cellIdentityTdscdma = (CellIdentityTdscdma) cellIdentity;
                    strValueOf = String.valueOf(cellIdentityTdscdma.getCid());
                    lac = cellIdentityTdscdma.getLac();
                }
                strValueOf2 = String.valueOf(lac);
            }
        }
        if (i == 0) {
            cVar.m = strValueOf;
            cVar.n = strValueOf2;
        } else if (i == 1) {
            cVar.w = strValueOf;
            cVar.x = strValueOf2;
        }
    }

    @Override // b.c.a.a.f1.c
    public void a(Context context, com.mobiletools.systemhelper.control.c cVar) {
        cVar.f622b = Build.MODEL;
        cVar.f621a = Build.MANUFACTURER;
        cVar.e = "Android";
        cVar.f = Build.VERSION.RELEASE;
        cVar.d = Build.DISPLAY;
        cVar.c = b.c.a.b.l.a("ro.hardware.version", (String) null);
        int i = Build.VERSION.SDK_INT;
        if (i < 26) {
            c(context, cVar);
            return;
        }
        if (i >= 22 && i < 24) {
            d(context, cVar);
            return;
        }
        int i2 = Build.VERSION.SDK_INT;
        if (i2 >= 24 && i2 < 26) {
            e(context, cVar);
            return;
        }
        int i3 = Build.VERSION.SDK_INT;
        if (i3 >= 26) {
            f(context, cVar);
        } else if (i3 >= 30) {
            b(context, cVar);
        }
    }

    @Override // b.c.a.a.f1.c
    public boolean a(Context context) {
        return new ServiceState().getRoaming();
    }

    /* JADX WARN: Code duplicated, block: B:30:0x005f  */
    /* JADX WARN: Code duplicated, block: B:31:0x006d  */
    protected int[] a(int i) {
        Method declaredMethod;
        int[] iArr;
        StringBuilder sb;
        int[] iArr2 = null;
        try {
            declaredMethod = Class.forName("android.telephony.SubscriptionManager").getDeclaredMethod("getSubId", Integer.TYPE);
        } catch (Exception e) {
            e.printStackTrace();
            declaredMethod = null;
        }
        if (declaredMethod != null) {
            try {
                long[] jArr = (long[]) declaredMethod.invoke(null, Integer.valueOf(i));
                if (jArr != null && jArr.length > 0) {
                    iArr = new int[jArr.length];
                    for (int i2 = 0; i2 < jArr.length; i2++) {
                        try {
                            iArr[i2] = (int) jArr[i2];
                        } catch (ClassCastException | IllegalAccessException | IllegalArgumentException | InvocationTargetException unused) {
                            try {
                                iArr2 = (int[]) declaredMethod.invoke(null, Integer.valueOf(i));
                            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e2) {
                                e2.printStackTrace();
                                iArr2 = iArr;
                            }
                            if (iArr2 != null) {
                                sb = new StringBuilder();
                                sb.append("[getSubId] sub[0] is ");
                                sb.append(iArr2[0]);
                            } else {
                                sb = new StringBuilder();
                                sb.append("[getSubId] result is ");
                                sb.append(iArr2);
                            }
                            Log.e("DeviceInfoReaderBase", sb.toString());
                            return iArr2;
                        }
                    }
                    iArr2 = iArr;
                }
            } catch (ClassCastException | IllegalAccessException | IllegalArgumentException | InvocationTargetException unused2) {
                iArr = null;
            }
            if (iArr2 != null) {
                sb = new StringBuilder();
                sb.append("[getSubId] sub[0] is ");
                sb.append(iArr2[0]);
            } else {
                sb = new StringBuilder();
                sb.append("[getSubId] result is ");
                sb.append(iArr2);
            }
            Log.e("DeviceInfoReaderBase", sb.toString());
        }
        return iArr2;
    }

    protected synchronized String b(Context context, int i) {
        String str;
        str = "-1";
        ImsManager imsManager = (ImsManager) context.getSystemService("telephony_ims");
        int iC = c(i);
        if (iC >= 0) {
            str = imsManager.getImsMmTelManager(iC).isAdvancedCallingSettingEnabled() ? "1" : "0";
        }
        return str;
    }

    void b(Context context, com.mobiletools.systemhelper.control.c cVar) {
        String strB;
        String strB2;
        if (a.b.c.a.a.a(context, "android.permission.READ_PHONE_NUMBERS") != 0 && a.b.c.a.a.a(context, "android.permission.READ_PHONE_STATE") != 0) {
            b.c.a.b.j.b("DeviceInfoReaderBase", "readAndroidR no READ_PHONE_STATE PERMISSION_GRANTED");
            return;
        }
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        cVar.g = telephonyManager.getImei(0);
        cVar.q = telephonyManager.getImei(1);
        int iC = c(0);
        if (iC >= 0) {
            TelephonyManager telephonyManagerCreateForSubscriptionId = ((TelephonyManager) context.getSystemService("phone")).createForSubscriptionId(iC);
            String subscriberId = telephonyManagerCreateForSubscriptionId.getSubscriberId();
            cVar.i = subscriberId;
            if (subscriberId != null && subscriberId.length() > 0) {
                cVar.h = telephonyManagerCreateForSubscriptionId.getSimSerialNumber();
                cVar.j = telephonyManagerCreateForSubscriptionId.getLine1Number();
                cVar.k = telephonyManagerCreateForSubscriptionId.getNetworkOperator();
                if (c(context, iC)) {
                    cVar.p = "1";
                } else {
                    cVar.p = "0";
                }
                cVar.l = b.c.a.b.f.n;
                a(context, 0, cVar);
                if (!b.c.a.b.f.c) {
                    strB2 = b(context, 0);
                } else if (cVar.k.trim().equals("46001") || cVar.k.trim().equals("46006") || cVar.k.trim().equals("46009")) {
                    strB2 = a(context, 0);
                }
                cVar.o = strB2;
            }
        }
        int iC2 = c(1);
        if (iC2 >= 0) {
            TelephonyManager telephonyManagerCreateForSubscriptionId2 = ((TelephonyManager) context.getSystemService("phone")).createForSubscriptionId(iC2);
            String subscriberId2 = telephonyManagerCreateForSubscriptionId2.getSubscriberId();
            cVar.s = subscriberId2;
            if (subscriberId2 == null || subscriberId2.length() <= 0) {
                return;
            }
            cVar.r = telephonyManagerCreateForSubscriptionId2.getSimSerialNumber();
            cVar.t = telephonyManagerCreateForSubscriptionId2.getLine1Number();
            cVar.u = telephonyManagerCreateForSubscriptionId2.getNetworkOperator();
            if (c(context, iC2)) {
                cVar.z = "1";
            } else {
                cVar.z = "0";
            }
            cVar.v = b.c.a.b.f.o;
            a(context, 1, cVar);
            if (!b.c.a.b.f.c) {
                strB = b(context, 1);
            } else if (!cVar.u.trim().equals("46001") && !cVar.u.trim().equals("46006") && !cVar.u.trim().equals("46009")) {
                return;
            } else {
                strB = a(context, 1);
            }
            cVar.y = strB;
        }
    }

    void c(Context context, com.mobiletools.systemhelper.control.c cVar) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        cVar.g = telephonyManager.getDeviceId();
        String subscriberId = telephonyManager.getSubscriberId();
        cVar.i = subscriberId;
        if (subscriberId == null || subscriberId.length() <= 0) {
            return;
        }
        cVar.h = telephonyManager.getSimSerialNumber();
        cVar.j = telephonyManager.getLine1Number();
        cVar.k = telephonyManager.getSimOperator();
        cVar.l = a(telephonyManager.getNetworkType(), cVar.k);
        cVar.a(telephonyManager.getCellLocation());
    }

    protected synchronized boolean c(Context context, int i) {
        boolean z;
        z = false;
        try {
            int i2 = Settings.Global.getInt(context.getContentResolver(), "preferred_network_mode" + i, -1);
            if (i2 >= 23 && i2 <= 33) {
                z = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return z;
    }

    void d(Context context, com.mobiletools.systemhelper.control.c cVar) {
        cVar.g = ((TelephonyManager) context.getSystemService("phone")).getDeviceId();
        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService("telephony_subscription_service");
        SubscriptionInfo activeSubscriptionInfoForSimSlotIndex = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(0);
        SubscriptionInfo activeSubscriptionInfoForSimSlotIndex2 = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(1);
        if (activeSubscriptionInfoForSimSlotIndex != null) {
            cVar.j = ((String) activeSubscriptionInfoForSimSlotIndex.getCarrierName()) + activeSubscriptionInfoForSimSlotIndex.getNumber();
            cVar.h = activeSubscriptionInfoForSimSlotIndex.getIccId();
        }
        if (activeSubscriptionInfoForSimSlotIndex2 != null) {
            cVar.t = ((String) activeSubscriptionInfoForSimSlotIndex2.getCarrierName()) + activeSubscriptionInfoForSimSlotIndex2.getNumber();
            cVar.r = activeSubscriptionInfoForSimSlotIndex2.getIccId();
        }
    }

    void e(Context context, com.mobiletools.systemhelper.control.c cVar) {
        cVar.g = ((TelephonyManager) context.getSystemService("phone")).getDeviceId();
        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService("telephony_subscription_service");
        SubscriptionInfo activeSubscriptionInfoForSimSlotIndex = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(0);
        SubscriptionInfo activeSubscriptionInfoForSimSlotIndex2 = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(1);
        if (activeSubscriptionInfoForSimSlotIndex != null) {
            cVar.j = ((String) activeSubscriptionInfoForSimSlotIndex.getCarrierName()) + activeSubscriptionInfoForSimSlotIndex.getNumber();
            cVar.h = activeSubscriptionInfoForSimSlotIndex.getIccId();
        }
        if (activeSubscriptionInfoForSimSlotIndex2 != null) {
            cVar.t = ((String) activeSubscriptionInfoForSimSlotIndex2.getCarrierName()) + activeSubscriptionInfoForSimSlotIndex2.getNumber();
            cVar.r = activeSubscriptionInfoForSimSlotIndex2.getIccId();
        }
        cVar.A = "" + (SubscriptionManager.getDefaultDataSubscriptionId() + 1);
    }

    void f(Context context, com.mobiletools.systemhelper.control.c cVar) {
        b.c.a.b.j.a("DeviceInfoReaderBase", "read26");
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        String imei = telephonyManager.getImei(0);
        String imei2 = telephonyManager.getImei(1);
        if (imei != null) {
            cVar.g = imei;
        }
        if (imei2 != null) {
            cVar.q = imei2;
        }
        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService("telephony_subscription_service");
        SubscriptionInfo activeSubscriptionInfoForSimSlotIndex = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(0);
        SubscriptionInfo activeSubscriptionInfoForSimSlotIndex2 = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(1);
        if (activeSubscriptionInfoForSimSlotIndex != null) {
            cVar.j = activeSubscriptionInfoForSimSlotIndex.getNumber();
            cVar.h = activeSubscriptionInfoForSimSlotIndex.getIccId();
        }
        if (activeSubscriptionInfoForSimSlotIndex2 != null) {
            cVar.t = activeSubscriptionInfoForSimSlotIndex2.getNumber();
            cVar.r = activeSubscriptionInfoForSimSlotIndex2.getIccId();
        }
        cVar.A = "" + (SubscriptionManager.getDefaultDataSubscriptionId() + 1);
    }

    public void g(Context context, com.mobiletools.systemhelper.control.c cVar) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        try {
            cVar.g = (String) g1.a(telephonyManager, "getDeviceId", 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int[] iArrA = g1.a(0);
        if (iArrA != null && iArrA[0] >= 0) {
            try {
                cVar.i = (String) g1.a(telephonyManager, "getSubscriberId", iArrA[0]);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            String str = cVar.i;
            if (str != null && str.length() > 0) {
                try {
                    cVar.h = (String) g1.a(telephonyManager, "getSimSerialNumber", iArrA[0]);
                } catch (Exception e3) {
                    e3.printStackTrace();
                }
                try {
                    cVar.j = (String) g1.a(telephonyManager, "getLine1NumberForSubscriber", iArrA[0]);
                } catch (Exception e4) {
                    e4.printStackTrace();
                }
                try {
                    cVar.k = (String) g1.a(telephonyManager, "getNetworkOperatorForSubscription", "getNetworkOperator", iArrA[0]);
                } catch (Exception e5) {
                    e5.printStackTrace();
                }
                try {
                    cVar.l = a(((Integer) g1.a(telephonyManager, "getNetworkType", iArrA[0])).intValue(), cVar.k);
                } catch (Exception e6) {
                    e6.printStackTrace();
                }
                try {
                    cVar.m = b.c.a.b.l.a(context, "CellID0", (String) null);
                    cVar.n = b.c.a.b.l.a(context, "LAC0", (String) null);
                } catch (Exception e7) {
                    e7.printStackTrace();
                }
            }
        }
        int[] iArrA2 = g1.a(1);
        try {
            cVar.q = (String) g1.a(telephonyManager, "getDeviceId", 1);
        } catch (Exception e8) {
            e8.printStackTrace();
        }
        if (iArrA2 == null || iArrA2[0] < 0) {
            return;
        }
        try {
            cVar.s = (String) g1.a(telephonyManager, "getSubscriberId", iArrA2[0]);
        } catch (Exception e9) {
            e9.printStackTrace();
        }
        String str2 = cVar.s;
        if (str2 == null || str2.length() <= 0) {
            return;
        }
        try {
            cVar.r = (String) g1.a(telephonyManager, "getSimSerialNumber", iArrA2[0]);
        } catch (Exception e10) {
            e10.printStackTrace();
        }
        try {
            cVar.t = (String) g1.a(telephonyManager, "getLine1NumberForSubscriber", iArrA2[0]);
        } catch (Exception e11) {
            e11.printStackTrace();
        }
        try {
            cVar.u = (String) g1.a(telephonyManager, "getNetworkOperatorForSubscription", "getNetworkOperator", iArrA2[0]);
        } catch (Exception e12) {
            e12.printStackTrace();
        }
        try {
            cVar.v = a(((Integer) g1.a(telephonyManager, "getNetworkType", iArrA2[0])).intValue(), cVar.u);
        } catch (Exception e13) {
            e13.printStackTrace();
        }
        try {
            cVar.w = b.c.a.b.l.a(context, "CellID1", (String) null);
            cVar.x = b.c.a.b.l.a(context, "LAC1", (String) null);
        } catch (Exception e14) {
            e14.printStackTrace();
        }
    }

    public void h(Context context, com.mobiletools.systemhelper.control.c cVar) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        try {
            cVar.g = (String) g1.a(telephonyManager, "getDeviceId", 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int[] iArrA = g1.a(0);
        if (iArrA != null && iArrA[0] >= 0) {
            try {
                cVar.i = (String) g1.a(telephonyManager, "getSubscriberId", iArrA[0]);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            String str = cVar.i;
            if (str != null && str.length() > 0) {
                try {
                    cVar.h = (String) g1.a(telephonyManager, "getSimSerialNumber", iArrA[0]);
                } catch (Exception e3) {
                    e3.printStackTrace();
                }
                try {
                    cVar.j = (String) g1.a(telephonyManager, "getLine1NumberForSubscriber", iArrA[0]);
                } catch (Exception e4) {
                    e4.printStackTrace();
                }
                try {
                    cVar.k = (String) g1.a(telephonyManager, "getNetworkOperatorForSubscription", "getNetworkOperator", iArrA[0]);
                } catch (Exception e5) {
                    e5.printStackTrace();
                }
                try {
                    cVar.l = a(a(telephonyManager, iArrA[0]), cVar.k);
                } catch (Exception e6) {
                    e6.printStackTrace();
                }
                cVar.m = b.c.a.b.l.a(context, "CellID0", (String) null);
                cVar.n = b.c.a.b.l.a(context, "LAC0", (String) null);
            }
        }
        int[] iArrA2 = g1.a(1);
        try {
            cVar.q = (String) g1.a(telephonyManager, "getDeviceId", 1);
        } catch (Exception e7) {
            e7.printStackTrace();
        }
        if (iArrA2 == null || iArrA2[0] < 0) {
            return;
        }
        try {
            cVar.s = (String) g1.a(telephonyManager, "getSubscriberId", iArrA2[0]);
        } catch (Exception e8) {
            e8.printStackTrace();
        }
        String str2 = cVar.s;
        if (str2 == null || str2.length() <= 0) {
            return;
        }
        try {
            cVar.r = (String) g1.a(telephonyManager, "getSimSerialNumber", iArrA2[0]);
        } catch (Exception e9) {
            e9.printStackTrace();
        }
        try {
            cVar.t = (String) g1.a(telephonyManager, "getLine1NumberForSubscriber", iArrA2[0]);
        } catch (Exception e10) {
            e10.printStackTrace();
        }
        try {
            cVar.u = (String) g1.a(telephonyManager, "getNetworkOperatorForSubscription", "getNetworkOperator", iArrA2[0]);
        } catch (Exception e11) {
            e11.printStackTrace();
        }
        try {
            cVar.v = a(a(telephonyManager, iArrA2[0]), cVar.u);
        } catch (Exception e12) {
            e12.printStackTrace();
        }
        cVar.w = b.c.a.b.l.a(context, "CellID1", (String) null);
        cVar.x = b.c.a.b.l.a(context, "LAC1", (String) null);
    }
}
