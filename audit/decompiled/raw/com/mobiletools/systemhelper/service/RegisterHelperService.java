package com.mobiletools.systemhelper.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import b.c.a.a.c;
import b.c.a.b.f;
import b.c.a.b.j;
import b.c.a.b.l;

/* JADX INFO: loaded from: classes.dex */
public class RegisterHelperService extends Service {

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private Context f626b = null;

    class a extends com.mobiletools.systemhelper.service.a.AbstractBinderC0033a {
        a() {
        }

        @Override // com.mobiletools.systemhelper.service.a
        public void a(String str) {
            l.a(RegisterHelperService.this.f626b, str);
        }

        @Override // com.mobiletools.systemhelper.service.a
        public void a(boolean z) {
            f.f599a = z;
            l.b(RegisterHelperService.this.f626b, "preference_enable_log", Boolean.valueOf(z));
        }

        @Override // com.mobiletools.systemhelper.service.a
        public boolean a() {
            return l.a(RegisterHelperService.this.f626b, "preference_enable_log", (Boolean) false).booleanValue();
        }

        @Override // com.mobiletools.systemhelper.service.a
        public void e() {
            l.a();
        }

        @Override // com.mobiletools.systemhelper.service.a
        public void i(int i) {
            l.b(RegisterHelperService.this.f626b, i);
            l.a(RegisterHelperService.this.f626b, (i * 8) / 5);
            l.c(RegisterHelperService.this.f626b, (i * 9) / 5);
        }
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        j.b("RegisterHelperService", "registerhelper Service onBind sucessed!");
        return new a();
    }

    @Override // android.app.Service
    public void onCreate() {
        j.b("RegisterHelperService", "registerhelper Service onCreate-->");
        Context applicationContext = getApplicationContext();
        this.f626b = applicationContext;
        if (Build.VERSION.SDK_INT >= 30) {
            c.b(applicationContext);
        }
    }

    @Override // android.app.Service
    public void onDestroy() {
        j.b("RegisterHelperService", "registerhelper Service onDestroy--->");
        super.onDestroy();
    }

    @Override // android.app.Service
    public boolean onUnbind(Intent intent) {
        super.onUnbind(intent);
        j.b("RegisterHelperService", "registerhelper Service onUnbind sucessed!");
        return true;
    }
}
