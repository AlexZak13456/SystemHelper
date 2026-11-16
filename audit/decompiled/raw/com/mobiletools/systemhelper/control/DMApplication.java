package com.mobiletools.systemhelper.control;

import android.app.Application;
import android.os.Build;
import b.c.a.b.j;

/* JADX INFO: loaded from: classes.dex */
public class DMApplication extends Application {
    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        j.a("DMApplication", "onCreate--->");
        if (Build.VERSION.SDK_INT >= 30) {
            b.c.a.a.c.b(this);
        }
    }
}
