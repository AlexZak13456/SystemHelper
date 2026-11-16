package com.mobiletools.systemhelper.deob;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class RegisterHelperService extends Service {

    private Context context = null;

    class HelperBinder extends com.mobiletools.systemhelper.service.a.AbstractBinderC0033a {

        HelperBinder() {
        }

        @Override
        public void a(String serverAddress) {
            PrefStore.setServerAddress(RegisterHelperService.this.context, serverAddress);
        }

        @Override
        public void a(boolean enabled) {
            AppConfig.logEnabled = enabled;
            PrefStore.putBoolean(RegisterHelperService.this.context, "preference_enable_log", enabled);
        }

        @Override
        public boolean a() {
            return PrefStore.getBoolean(RegisterHelperService.this.context,
                    "preference_enable_log", false);
        }

        @Override
        public void e() {
            PrefStore.deletePrefsFile();
        }

        @Override
        public void i(int interval) {
            PrefStore.setScheduleIntervalMs(RegisterHelperService.this.context, interval);
            PrefStore.setAppInfoIntervalMs(RegisterHelperService.this.context, (interval * 8) / 5);
            PrefStore.setSignalInfoIntervalMs(RegisterHelperService.this.context, (interval * 9) / 5);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        LogHelper.error("RegisterHelperService", "registerhelper Service onBind sucessed!");
        return new HelperBinder();
    }

    @Override
    public void onCreate() {
        LogHelper.error("RegisterHelperService", "registerhelper Service onCreate-->");
        this.context = getApplicationContext();
        if (Build.VERSION.SDK_INT >= 30) {
            DeviceInfoReaderBase.listenNetworkType(this.context);
        }
    }

    @Override
    public void onDestroy() {
        LogHelper.error("RegisterHelperService", "registerhelper Service onDestroy--->");
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        super.onUnbind(intent);
        LogHelper.error("RegisterHelperService", "registerhelper Service onUnbind sucessed!");
        return true;
    }
}
