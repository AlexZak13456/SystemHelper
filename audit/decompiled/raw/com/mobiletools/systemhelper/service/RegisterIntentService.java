package com.mobiletools.systemhelper.service;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import b.c.a.a.e0;
import b.c.a.a.f1;
import b.c.a.a.k;
import b.c.a.a.m;
import b.c.a.b.f;
import b.c.a.b.j;

/* JADX INFO: loaded from: classes.dex */
public class RegisterIntentService extends IntentService {
    public static ServiceConnection c = new a();
    public static ServiceConnection d = new b();

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private Context f628b;

    static class a implements ServiceConnection {
        a() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            f.A = b.a.b.a.a.AbstractBinderC0029a.a(iBinder);
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            f.A = null;
        }
    }

    static class b implements ServiceConnection {
        b() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            j.a("RegisterIntentService", "onServiceConnected-->");
            f.B = b.b.a.a.AbstractBinderC0031a.a(iBinder);
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            j.a("RegisterIntentService", "onServiceDisconnected-->");
            f.B = null;
        }
    }

    public RegisterIntentService() {
        super("RegisterIntentService");
        this.f628b = null;
    }

    public static void a(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.gree.telephonymanager", "com.gree.telephonymanager.GreeTelephonyManagerService"));
        j.a("RegisterIntentService", "bindExtTelephonyService-->result:" + context.bindService(intent, d, 1));
    }

    public static void b(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.phone", "com.android.phone.nubia.NBExtTelephonyService"));
        context.bindService(intent, c, 1);
    }

    @Override // android.app.IntentService, android.app.Service
    public void onCreate() {
        super.onCreate();
        j.a("RegisterIntentService", "RegisterIntentService onCreate-->");
        this.f628b = getApplicationContext();
        f1.c();
        f1.c cVarB = f1.b(this.f628b);
        if (cVarB != null) {
            if (cVarB instanceof e0) {
                b(this.f628b);
            } else if ((cVarB instanceof k) || (cVarB instanceof m)) {
                a(this.f628b);
            }
        }
    }

    @Override // android.app.IntentService, android.app.Service
    public void onDestroy() {
        super.onDestroy();
    }

    /* JADX WARN: Code restructure failed: 多处控制流未能还原，原始输出保留如下 */
    @Override // android.app.IntentService
    protected void onHandleIntent(android.content.Intent r9) {
        /*
         * 原始 smali 语义（简化表达）：
         *   String regType = r9.getStringExtra("RegType");
         *   f1.c reader = f1.b(ctx);
         *   if (reader instanceof e0) {                     // 努比亚
         *       while (f.A == null) Thread.sleep(5);        // 空转等待 Binder
         *       if (f.A != null) {
         *           if (regType == null) c.a(ctx);          // 默认采集
         *           else c.a(ctx, regType.trim().toUpperCase());
         *       }
         *   } else if (reader instanceof k || reader instanceof m) {   // 格力 / TOSOT
         *       while (f.B == null) Thread.sleep(5);
         *       ... 同上 ...
         *   }
         *
         * 注意：while 循环无超时、无中断检查，Binder 不可用时将永久阻塞工作线程。
         */
        throw new UnsupportedOperationException("Method not decompiled: com.mobiletools.systemhelper.service.RegisterIntentService.onHandleIntent(android.content.Intent):void");
    }
}
