package com.mobiletools.systemhelper.control;

import android.content.Context;
import android.os.Build;
import b.c.a.a.f1;
import b.c.a.b.j;
import b.c.a.b.l;
import java.net.URLEncoder;

/* JADX INFO: loaded from: classes.dex */
public class g extends Thread {

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private Context f625b;
    private c c;

    public g(Context context, c cVar) {
        this.f625b = null;
        this.c = null;
        this.f625b = context;
        this.c = cVar;
    }

    private void a(boolean z) {
    }

    private synchronized boolean a(c cVar) {
        boolean z;
        b.c.a.b.d dVarA = b.c.a.b.d.a(this.f625b, b.c.a.b.d.c, b.c.a.b.d.f597b.intValue());
        cVar.L = Build.VERSION.SDK_INT >= 28 ? dVarA.c() : dVarA.b();
        dVarA.close();
        String strA = cVar.a();
        j.a("RegisterThread", "register postString：" + strA);
        z = false;
        try {
            byte[] bArrA = b.c.a.b.c.a(strA);
            String strEncode = URLEncoder.encode(b.c.a.b.c.a(b.c.a.b.c.a(this.f625b, bArrA)), "utf-8");
            String str = l.c(this.f625b) + "?ver=3.0&model=" + URLEncoder.encode(cVar.f622b, "utf-8") + "&manuf=" + URLEncoder.encode(cVar.f621a, "utf-8") + "&sign=" + strEncode;
            byte[] bArrA2 = b.c.a.b.c.a(strA, bArrA);
            try {
                b.c.a.b.g gVar = new b.c.a.b.g();
                boolean zA = gVar.a(str, bArrA2, bArrA);
                if (!zA && gVar.b() == 416) {
                    zA = true;
                }
                z = zA;
            } catch (Exception e) {
                j.b("RegisterThread", "sendPOSTRequest exception--->" + e.toString());
            }
            if (z) {
                b.c.a.b.d dVarA2 = b.c.a.b.d.a(this.f625b, b.c.a.b.d.c, b.c.a.b.d.f597b.intValue());
                dVarA2.a();
                dVarA2.close();
            } else {
                cVar.L = null;
                b.c.a.b.d dVarA3 = b.c.a.b.d.a(this.f625b, b.c.a.b.d.c, b.c.a.b.d.f597b.intValue());
                dVarA3.a(cVar.a());
                dVarA3.close();
            }
        } catch (Exception e2) {
            j.b("RegisterThread", "doEncrpytPost catch exception " + e2.getMessage());
            return false;
        }
        return z;
    }

    private boolean a(String str) {
        StringBuilder sb;
        String str2;
        if (str == null || str.isEmpty()) {
            return true;
        }
        if (str.length() != 15) {
            sb = new StringBuilder();
            sb.append("imei: ");
            sb.append(str);
            str2 = " length is not 15!";
        } else {
            if (str.matches("[0-9]+")) {
                return true;
            }
            sb = new StringBuilder();
            sb.append("imei: ");
            sb.append(str);
            str2 = " is not all number!";
        }
        sb.append(str2);
        j.b("RegisterThread", sb.toString());
        return false;
    }

    private synchronized boolean b(c cVar) {
        String str;
        String str2;
        if (a(cVar.g) && a(cVar.q)) {
            if (!l.d(this.f625b).booleanValue()) {
                b.c.a.b.d dVarA = b.c.a.b.d.a(this.f625b, b.c.a.b.d.c, b.c.a.b.d.f597b.intValue());
                dVarA.a(cVar.a());
                dVarA.close();
                str = "RegisterThread";
                str2 = "No network connected!";
            } else {
                if (l.e(this.f625b).booleanValue()) {
                    return a(cVar);
                }
                if (!f1.d(this.f625b).booleanValue() && l.a(cVar).booleanValue()) {
                    return a(cVar);
                }
                b.c.a.b.d dVarA2 = b.c.a.b.d.a(this.f625b, b.c.a.b.d.c, b.c.a.b.d.f597b.intValue());
                dVarA2.a(cVar.a());
                dVarA2.close();
                str = "RegisterThread";
                str2 = "isUnicomNetwork=false, not post!";
            }
            j.a(str, str2);
            return false;
        }
        return false;
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        if (b.c.a.b.f.g) {
            a(b(this.c));
            return;
        }
        b.c.a.b.d dVarA = b.c.a.b.d.a(this.f625b, b.c.a.b.d.c, b.c.a.b.d.f597b.intValue());
        dVarA.a(this.c.a());
        dVarA.close();
        j.a("RegisterThread", "User Permission Closed!");
    }
}
