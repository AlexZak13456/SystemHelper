package com.mobiletools.systemhelper.control;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import b.c.a.a.e0;
import b.c.a.a.f1;
import b.c.a.a.k;
import b.c.a.a.m;
import b.c.a.b.j;
import b.c.a.b.l;
import com.mobiletools.systemhelper.service.RegisterIntentService;

/* JADX INFO: loaded from: classes.dex */
public class AppInfoRegisterReceiver extends BroadcastReceiver {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private static Context f608a;

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        f608a = context;
        j.a("AppInfoRegisterReceiver", "onReceive-->intent action: " + intent.getAction());
        if (Build.VERSION.SDK_INT >= 30) {
            b.c.a.a.c.b(context);
        }
        b.c.a.b.a.a(f608a, l.a(context));
        f1.c();
        f1.c cVarB = f1.b(context);
        if (cVarB != null) {
            if ((cVarB instanceof e0) || (cVarB instanceof k) || (cVarB instanceof m)) {
                Intent intent2 = new Intent(context, (Class<?>) RegisterIntentService.class);
                intent2.putExtra("RegType", "A");
                f608a.startService(intent2);
            } else {
                c.a(f608a, "A");
                if (Build.VERSION.SDK_INT >= 31) {
                    b.c.a.a.c.c(context);
                }
            }
        }
    }
}
