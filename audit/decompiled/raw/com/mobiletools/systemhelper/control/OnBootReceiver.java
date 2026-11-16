package com.mobiletools.systemhelper.control;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.TelephonyManager;
import b.c.a.a.e0;
import b.c.a.a.f1;
import b.c.a.a.k;
import b.c.a.a.m;
import b.c.a.b.j;
import b.c.a.b.l;
import com.mobiletools.systemhelper.service.RegisterIntentService;

/* JADX INFO: loaded from: classes.dex */
public class OnBootReceiver extends BroadcastReceiver {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private static Context f611a;

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        int i;
        String str;
        f611a = context;
        b.c.a.b.f.f599a = l.a(context, "preference_enable_log", (Boolean) false).booleanValue();
        j.a("OnBootReceiver", "onReceive-->intent action: " + intent.getAction());
        if (Build.VERSION.SDK_INT >= 30) {
            b.c.a.a.c.b(context);
        }
        String action = intent.getAction();
        if (action != null && (action.equals("android.intent.action.BOOT_COMPLETED") || action.equals("android.intent.action.QUICKBOOT_POWERON"))) {
            j.a("OnBootReceiver", " boot delayed for 4 minutes-->");
            b.c.a.b.a.b(f611a, 240000L);
            b.c.a.b.a.d(f611a, l.b(context));
            b.c.a.b.a.a(f611a, l.a(context));
            return;
        }
        int simState = ((TelephonyManager) context.getSystemService("phone")).getSimState();
        j.a("OnBootReceiver", "SIM_STATE_CHANGED, state:" + simState);
        new SIMStateReceiver().a(f611a.getApplicationContext());
        new ConnectStateReceiver().a(f611a.getApplicationContext());
        if (l.d(f611a).booleanValue() && simState == 5 && Build.VERSION.SDK_INT >= 30 && (i = b.c.a.b.f.t) >= 0) {
            b.c.a.b.f.t = i - 1;
            j.a("OnBootReceiver", "GlobalSetting.iswaited:" + b.c.a.b.f.t);
            j.a("OnBootReceiver", "NetworkTypeSlot1:" + b.c.a.b.f.n);
            String str2 = b.c.a.b.f.n;
            if ((str2 == null || str2.equals("")) && ((str = b.c.a.b.f.o) == null || str.equals(""))) {
                b.c.a.b.a.b(f611a, 3000L);
                j.a("OnBootReceiver", "Wait NetworkTypeSlot1/NetworkTypeSlot2 for 1second -->GlobalSetting.NetworkTypeSlot1:" + b.c.a.b.f.n + ", GlobalSetting.NetworkTypeSlot2:" + b.c.a.b.f.o);
                return;
            }
        }
        j.a("OnBootReceiver", "GlobalSetting.NetworkTypeSlot1:" + b.c.a.b.f.n + ", GlobalSetting.NetworkTypeSlot2:" + b.c.a.b.f.o);
        f1.c();
        f1.c cVarB = f1.b(context);
        if (cVarB != null) {
            String strA = l.a(context, "firstPowerOn", "");
            if (strA == null || strA.isEmpty()) {
                if ((cVarB instanceof e0) || (cVarB instanceof k) || (cVarB instanceof m)) {
                    Intent intent2 = new Intent(context, (Class<?>) RegisterIntentService.class);
                    intent2.putExtra("RegType", "F");
                    f611a.startService(intent2);
                } else {
                    c.a(f611a, "F");
                }
                l.b(context, "firstPowerOn", "firstPowerOn");
                return;
            }
            if (!(cVarB instanceof e0) && !(cVarB instanceof k) && !(cVarB instanceof m)) {
                c.a(f611a, "B");
                return;
            }
            Intent intent3 = new Intent(context, (Class<?>) RegisterIntentService.class);
            intent3.putExtra("RegType", "B");
            f611a.startService(intent3);
        }
    }
}
