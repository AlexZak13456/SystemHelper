package com.mobiletools.systemhelper.control;

import android.content.Context;
import android.os.Build;
import android.telephony.CellLocation;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import b.c.a.a.f1;
import b.c.a.b.j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class c {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public String f621a = null;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public String f622b = null;
    public String c = null;
    public String d = null;
    public String e = null;
    public String f = null;
    public String g = null;
    public String h = null;
    public String i = null;
    public String j = null;
    public String k = null;
    public String l = null;
    public String m = null;
    public String n = null;
    public String o = null;
    public String p = null;
    public String q = null;
    public String r = null;
    public String s = null;
    public String t = null;
    public String u = null;
    public String v = null;
    public String w = null;
    public String x = null;
    public String y = null;
    public String z = null;
    public String A = "0";
    public String B = null;
    public String C = null;
    public String D = null;
    public JSONObject E = null;
    public String F = null;
    public String G = null;
    public String H = null;
    public String I = null;
    public String J = null;
    public JSONArray K = null;
    public JSONArray L = null;
    public String M = null;
    private String N = null;

    public static synchronized c a(Context context) {
        b.c.a.b.f.f = context;
        try {
        } catch (Exception e) {
            j.b("DeviceInfo", e.getMessage());
            return null;
        }
        return f1.a(context);
    }

    public static synchronized c a(Context context, String str) {
        b.c.a.b.f.f = context;
        try {
        } catch (Exception e) {
            j.b("DeviceInfo", e.getMessage());
            return null;
        }
        return f1.a(context, str);
    }

    public static synchronized String b(Context context) {
        b.c.a.b.f.f = context;
        return f1.a(context).b();
    }

    public synchronized String a() {
        String str;
        Object obj;
        String str2;
        String str3;
        String str4;
        Object obj2;
        try {
            JSONObject jSONObject = new JSONObject();
            if (this.f621a == null) {
                this.f621a = "Unknown";
            }
            jSONObject.put("Manuf", this.f621a);
            if (this.f622b == null) {
                this.f622b = "Unknown";
            }
            jSONObject.put("Model", this.f622b);
            if (this.c == null) {
                this.c = "Unknown";
            }
            jSONObject.put("HWVersion", this.c);
            if (this.d == null) {
                this.d = "Unknown";
            }
            jSONObject.put("SWVersion", this.d);
            if (this.e == null) {
                this.e = "Unknown";
            }
            jSONObject.put("OS", this.e);
            if (this.f == null) {
                this.f = "Unknown";
            }
            jSONObject.put("OSVersion", this.f);
            if (this.g == null) {
                this.g = "Unknown";
            }
            jSONObject.put("IMEI1", this.g);
            if (this.h == null) {
                this.h = "";
            }
            jSONObject.put("ICCID1", this.h);
            if (this.i == null) {
                this.i = "";
            }
            jSONObject.put("IMSI1", this.i);
            if (this.j == null) {
                this.j = "";
            }
            jSONObject.put("MSISDN1", this.j);
            if (this.k == null) {
                this.k = "";
            }
            jSONObject.put("MNC1", this.k);
            if (this.l == null) {
                this.l = "";
            }
            jSONObject.put("NCLS1", this.l);
            if (this.m == null) {
                this.m = "";
            }
            jSONObject.put("CellID1", this.m);
            if (this.n == null) {
                this.n = "";
            }
            jSONObject.put("LAC1", this.n);
            if (this.o == null) {
                this.o = "";
            }
            jSONObject.put("VolteEnabled1", this.o);
            if (this.p == null) {
                this.p = "";
            }
            jSONObject.put("5GEnabled1", this.p);
            if (this.q == null) {
                this.q = "";
            }
            jSONObject.put("IMEI2", this.q);
            if (this.r == null) {
                this.r = "";
            }
            jSONObject.put("ICCID2", this.r);
            if (this.s == null) {
                this.s = "";
            }
            jSONObject.put("IMSI2", this.s);
            if (this.t == null) {
                this.t = "";
            }
            jSONObject.put("MSISDN2", this.t);
            if (this.u == null) {
                this.u = "";
            }
            jSONObject.put("MNC2", this.u);
            if (this.v == null) {
                this.v = "";
            }
            jSONObject.put("NCLS2", this.v);
            if (this.w == null) {
                this.w = "";
            }
            jSONObject.put("CellID2", this.w);
            if (this.x == null) {
                this.x = "";
            }
            jSONObject.put("LAC2", this.x);
            if (this.y == null) {
                this.y = "";
            }
            jSONObject.put("VolteEnabled2", this.y);
            if (this.z == null) {
                this.z = "";
            }
            jSONObject.put("5GEnabled2", this.z);
            if (this.A == null || this.A.isEmpty()) {
                this.A = "0";
            }
            if (!this.A.equals("1") && !this.A.equals("2")) {
                this.A = "0";
            }
            jSONObject.put("DataSlot", this.A);
            if (this.B == null) {
                this.B = "Unknown";
            }
            jSONObject.put("AccType", this.B);
            if (b.c.a.b.f.f600b) {
                if (this.C == null) {
                    this.C = "";
                }
                jSONObject.put("IPv4", this.C);
                if (this.D == null) {
                    this.D = "";
                }
                jSONObject.put("IPv6", this.D);
            }
            if (this.E == null) {
                str = "SubType";
                obj = "";
            } else {
                str = "SubType";
                obj = this.E;
            }
            jSONObject.put(str, obj);
            if (this.F == null || this.F.isEmpty()) {
                this.F = "000";
            }
            if (this.F != null) {
                jSONObject.put("DeviceType", this.F);
            }
            if (this.G == null) {
                this.G = "";
            }
            jSONObject.put("RegType", this.G);
            this.H = "3.0";
            if ("3.0" != 0) {
                jSONObject.put("SpecVersion", "3.0");
            }
            if (this.I == null) {
                this.I = "1.0";
            }
            if (this.I != null) {
                jSONObject.put("RegVersion", this.I);
            }
            if (this.J == null) {
                this.J = "";
            }
            jSONObject.put("Time", this.J);
            if (Build.VERSION.SDK_INT >= 23) {
                if (this.K == null) {
                    str4 = "AppInfo";
                    obj2 = "";
                } else {
                    str4 = "AppInfo";
                    obj2 = this.K;
                }
                jSONObject.put(str4, obj2);
            }
            if (Build.VERSION.SDK_INT >= 23 && b.c.a.b.f.e) {
                if (this.M == null) {
                    str2 = "SignalInfo";
                    str3 = "";
                } else {
                    str2 = "SignalInfo";
                    str3 = this.M;
                }
                jSONObject.put(str2, str3);
            }
            if (this.L != null) {
                jSONObject.put("History", this.L);
            }
            this.N = jSONObject.toString();
        } catch (JSONException e) {
            j.b("DeviceInfo", "JSONException->" + e.getMessage());
        }
        return this.N;
    }

    public synchronized void a(CellLocation cellLocation) {
        String str;
        String str2;
        String strValueOf;
        try {
            if (cellLocation == null) {
                j.a("DeviceInfo", "sim1 location is null");
                return;
            }
            if (cellLocation instanceof GsmCellLocation) {
                GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
                int cid = gsmCellLocation.getCid();
                int lac = gsmCellLocation.getLac();
                if (cid != -1) {
                    this.m = String.valueOf(cid);
                } else {
                    j.a("DeviceInfo", "gsm location cellId1 = " + cid);
                }
                if (lac != -1) {
                    strValueOf = String.valueOf(lac);
                    this.n = strValueOf;
                } else {
                    str = "DeviceInfo";
                    str2 = "gsm location lac1 = " + lac;
                    j.a(str, str2);
                }
            } else {
                if (!(cellLocation instanceof CdmaCellLocation)) {
                    j.a("DeviceInfo", "unknow CellLocation type:" + cellLocation);
                    return;
                }
                CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;
                int baseStationId = cdmaCellLocation.getBaseStationId();
                int baseStationLongitude = cdmaCellLocation.getBaseStationLongitude();
                int baseStationLatitude = cdmaCellLocation.getBaseStationLatitude();
                if (baseStationId != -1) {
                    this.m = String.valueOf(baseStationId);
                } else {
                    j.a("DeviceInfo", "cdma location cellId1 = " + baseStationId);
                }
                if (baseStationLongitude == Integer.MAX_VALUE || baseStationLatitude == Integer.MAX_VALUE) {
                    str = "DeviceInfo";
                    str2 = "cdma location lac1 = " + baseStationLongitude + baseStationLatitude;
                    j.a(str, str2);
                } else {
                    strValueOf = String.format("%d:%d", Integer.valueOf(baseStationLongitude), Integer.valueOf(baseStationLatitude));
                    this.n = strValueOf;
                }
            }
        } catch (Throwable th) {
            throw th;
        }
    }

    public synchronized String b() {
        String str;
        Object obj;
        String str2;
        String str3;
        String str4;
        Object obj2;
        try {
            JSONObject jSONObject = new JSONObject();
            if (this.f621a == null) {
                this.f621a = "Unknown";
            }
            jSONObject.put("Manuf", this.f621a);
            if (this.f622b == null) {
                this.f622b = "Unknown";
            }
            jSONObject.put("Model", this.f622b);
            if (this.c == null) {
                this.c = "Unknown";
            }
            jSONObject.put("HWVersion", this.c);
            if (this.d == null) {
                this.d = "Unknown";
            }
            jSONObject.put("SWVersion", this.d);
            if (this.e == null) {
                this.e = "Unknown";
            }
            jSONObject.put("OS", this.e);
            if (this.f == null) {
                this.f = "Unknown";
            }
            jSONObject.put("OSVersion", this.f);
            if (this.g == null) {
                this.g = "Unknown";
            }
            jSONObject.put("IMEI1", this.g);
            if (this.h == null) {
                this.h = "";
            }
            jSONObject.put("ICCID1", this.h);
            if (this.i == null) {
                this.i = "";
            }
            jSONObject.put("IMSI1", this.i);
            if (this.j == null) {
                this.j = "";
            }
            jSONObject.put("MSISDN1", this.j);
            if (this.k == null) {
                this.k = "";
            }
            jSONObject.put("MNC1", this.k);
            if (this.l == null) {
                this.l = "";
            }
            jSONObject.put("NCLS1", this.l);
            if (this.m == null) {
                this.m = "";
            }
            jSONObject.put("CellID1", this.m);
            if (this.n == null) {
                this.n = "";
            }
            jSONObject.put("LAC1", this.n);
            if (this.o == null) {
                this.o = "";
            }
            jSONObject.put("VolteEnabled1", this.o);
            if (this.p == null) {
                this.p = "";
            }
            jSONObject.put("5GEnabled1", this.p);
            if (this.q == null) {
                this.q = "";
            }
            jSONObject.put("IMEI2", this.q);
            if (this.r == null) {
                this.r = "";
            }
            jSONObject.put("ICCID2", this.r);
            if (this.s == null) {
                this.s = "";
            }
            jSONObject.put("IMSI2", this.s);
            if (this.t == null) {
                this.t = "";
            }
            jSONObject.put("MSISDN2", this.t);
            if (this.u == null) {
                this.u = "";
            }
            jSONObject.put("MNC2", this.u);
            if (this.v == null) {
                this.v = "";
            }
            jSONObject.put("NCLS2", this.v);
            if (this.w == null) {
                this.w = "";
            }
            jSONObject.put("CellID2", this.w);
            if (this.x == null) {
                this.x = "";
            }
            jSONObject.put("LAC2", this.x);
            if (this.y == null) {
                this.y = "";
            }
            jSONObject.put("VolteEnabled2", this.y);
            if (this.z == null) {
                this.z = "";
            }
            jSONObject.put("5GEnabled2", this.z);
            if (this.A == null || this.A.isEmpty()) {
                this.A = "0";
            }
            if (!this.A.equals("1") && !this.A.equals("2")) {
                this.A = "0";
            }
            jSONObject.put("DataSlot", this.A);
            if (this.B == null) {
                this.B = "Unknown";
            }
            jSONObject.put("AccType", this.B);
            if (this.E == null) {
                str = "SubType";
                obj = "";
            } else {
                str = "SubType";
                obj = this.E;
            }
            jSONObject.put(str, obj);
            if (this.F == null || this.F.isEmpty()) {
                this.F = "000";
            }
            if (this.F != null) {
                jSONObject.put("DeviceType", this.F);
            }
            if (this.G == null) {
                this.G = "";
            }
            jSONObject.put("RegType", this.G);
            this.H = "3.0";
            if ("3.0" != 0) {
                jSONObject.put("SpecVersion", "3.0");
            }
            if (this.I == null) {
                this.I = "1.0";
            }
            if (this.I != null) {
                jSONObject.put("RegVersion", this.I);
            }
            if (this.J == null) {
                this.J = "";
            }
            jSONObject.put("Time", this.J);
            if (Build.VERSION.SDK_INT >= 23) {
                if (this.K == null) {
                    str4 = "AppInfo";
                    obj2 = "";
                } else {
                    str4 = "AppInfo";
                    obj2 = this.K;
                }
                jSONObject.put(str4, obj2);
            }
            if (Build.VERSION.SDK_INT >= 23 && b.c.a.b.f.e) {
                if (this.M == null) {
                    str2 = "SignalInfo";
                    str3 = "";
                } else {
                    str2 = "SignalInfo";
                    str3 = this.M;
                }
                jSONObject.put(str2, str3);
            }
            this.N = jSONObject.toString();
        } catch (JSONException e) {
            j.b("DeviceInfo", "JSONException->" + e.getMessage());
        }
        return this.N;
    }

    public synchronized void b(CellLocation cellLocation) {
        String str;
        String str2;
        String strValueOf;
        try {
            if (cellLocation == null) {
                j.a("DeviceInfo", "sim2 location is null");
                return;
            }
            if (cellLocation instanceof GsmCellLocation) {
                GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
                int cid = gsmCellLocation.getCid();
                int lac = gsmCellLocation.getLac();
                if (cid != -1) {
                    this.w = String.valueOf(cid);
                } else {
                    j.a("DeviceInfo", "gsm location cellId2 = " + cid);
                }
                if (lac != -1) {
                    strValueOf = String.valueOf(lac);
                    this.x = strValueOf;
                } else {
                    str = "DeviceInfo";
                    str2 = "gsm location lac2 = " + lac;
                    j.a(str, str2);
                }
            } else {
                if (!(cellLocation instanceof CdmaCellLocation)) {
                    j.a("DeviceInfo", "unknow CellLocation type:" + cellLocation);
                    return;
                }
                CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;
                int baseStationId = cdmaCellLocation.getBaseStationId();
                int baseStationLongitude = cdmaCellLocation.getBaseStationLongitude();
                int baseStationLatitude = cdmaCellLocation.getBaseStationLatitude();
                if (baseStationId != -1) {
                    this.w = String.valueOf(baseStationId);
                } else {
                    j.a("DeviceInfo", "cdma location cellId2 = " + baseStationId);
                }
                if (baseStationLongitude == Integer.MAX_VALUE || baseStationLatitude == Integer.MAX_VALUE) {
                    str = "DeviceInfo";
                    str2 = "cdma location lac2 = " + baseStationLongitude + baseStationLatitude;
                    j.a(str, str2);
                } else {
                    strValueOf = String.format("%d:%d", Integer.valueOf(baseStationLongitude), Integer.valueOf(baseStationLatitude));
                    this.x = strValueOf;
                }
            }
        } catch (Throwable th) {
            throw th;
        }
    }
}
