package com.mobiletools.systemhelper.control;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class e extends Thread {
    private PackageManager d;
    private Context e;
    private a g;
    private c h;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private long f624b = 0;
    private long c = 0;
    private List<b.c.a.b.b> f = null;

    public interface a {
        void a(Context context, c cVar, List<b.c.a.b.b> list);
    }

    class b implements Comparator<UsageStats> {
        b(e eVar) {
        }

        @Override // java.util.Comparator
        /* JADX INFO: renamed from: a, reason: merged with bridge method [inline-methods] */
        public int compare(UsageStats usageStats, UsageStats usageStats2) {
            return (int) (usageStats2.getTotalTimeInForeground() - usageStats.getTotalTimeInForeground());
        }
    }

    public e(Context context, c cVar, a aVar) {
        this.h = null;
        this.e = context;
        this.h = cVar;
        this.g = aVar;
    }

    private b.c.a.b.b a(List<ApplicationInfo> list, UsageStats usageStats) {
        b.c.a.b.b bVar = new b.c.a.b.b();
        bVar.b(usageStats.getPackageName());
        for (ApplicationInfo applicationInfo : list) {
            if (applicationInfo.packageName.equalsIgnoreCase(usageStats.getPackageName())) {
                bVar.a(applicationInfo.uid);
                bVar.a((String) applicationInfo.loadLabel(this.d));
                break;
            }
        }
        bVar.a(usageStats.getTotalTimeInForeground() / 60000);
        a(bVar);
        return bVar;
    }

    private List<UsageStats> a() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) this.e.getSystemService("usagestats");
        ArrayList arrayList = new ArrayList();
        Iterator<Map.Entry<String, UsageStats>> it = usageStatsManager.queryAndAggregateUsageStats(this.c, this.f624b).entrySet().iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().getValue());
        }
        return arrayList;
    }

    private void a(b.c.a.b.b bVar) {
        long txBytes;
        long rxBytes;
        long txBytes2;
        long rxBytes2;
        long txBytes3;
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) this.e.getSystemService("netstats");
        String str = this.h.i;
        long rxBytes3 = 0;
        NetworkStats networkStatsQuerySummary = null;
        if (str != null && !str.isEmpty()) {
            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
            try {
                networkStatsQuerySummary = networkStatsManager.querySummary(0, this.h.i, this.c, this.f624b);
                rxBytes2 = 0;
                txBytes3 = 0;
                do {
                    try {
                        networkStatsQuerySummary.getNextBucket(bucket);
                        if (bVar.b() == bucket.getUid()) {
                            rxBytes2 += bucket.getRxBytes();
                            txBytes3 += bucket.getTxBytes();
                        }
                    } catch (RemoteException e) {
                        e = e;
                        e.printStackTrace();
                    }
                } while (networkStatsQuerySummary.hasNextBucket());
            } catch (RemoteException e2) {
                e = e2;
                rxBytes2 = 0;
                txBytes3 = 0;
            }
            networkStatsQuerySummary.close();
            bVar.h(txBytes3 / 1024);
            bVar.b(rxBytes2 / 1024);
            bVar.e((txBytes3 + rxBytes2) / 1024);
        }
        String str2 = this.h.s;
        if (str2 != null && !str2.isEmpty()) {
            NetworkStats.Bucket bucket2 = new NetworkStats.Bucket();
            try {
                networkStatsQuerySummary = networkStatsManager.querySummary(0, this.h.s, this.c, this.f624b);
                rxBytes = 0;
                txBytes2 = 0;
                do {
                    try {
                        networkStatsQuerySummary.getNextBucket(bucket2);
                        if (bVar.b() == bucket2.getUid()) {
                            rxBytes += bucket2.getRxBytes();
                            txBytes2 += bucket2.getTxBytes();
                        }
                    } catch (RemoteException e3) {
                        e = e3;
                        e.printStackTrace();
                    }
                } while (networkStatsQuerySummary.hasNextBucket());
            } catch (RemoteException e4) {
                e = e4;
                rxBytes = 0;
                txBytes2 = 0;
            }
            networkStatsQuerySummary.close();
            bVar.i(txBytes2 / 1024);
            bVar.c(rxBytes / 1024);
            bVar.f((txBytes2 + rxBytes) / 1024);
        }
        NetworkStats.Bucket bucket3 = new NetworkStats.Bucket();
        try {
            networkStatsQuerySummary = networkStatsManager.querySummary(1, null, this.c, this.f624b);
            txBytes = 0;
            do {
                try {
                    networkStatsQuerySummary.getNextBucket(bucket3);
                    if (bVar.b() == bucket3.getUid()) {
                        rxBytes3 += bucket3.getRxBytes();
                        txBytes += bucket3.getTxBytes();
                    }
                } catch (RemoteException e5) {
                    e = e5;
                    e.printStackTrace();
                }
            } while (networkStatsQuerySummary.hasNextBucket());
        } catch (RemoteException e6) {
            e = e6;
            txBytes = 0;
        }
        networkStatsQuerySummary.close();
        bVar.j(txBytes / 1048576);
        bVar.d(rxBytes3 / 1048576);
        bVar.g((txBytes + rxBytes3) / 1048576);
    }

    private List<b.c.a.b.b> b() {
        PackageManager packageManager = this.e.getPackageManager();
        this.d = packageManager;
        List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(0);
        List<UsageStats> listA = a();
        if (listA != null) {
            Collections.sort(listA, new b(this));
        }
        ArrayList arrayList = new ArrayList();
        Iterator<UsageStats> it = listA.iterator();
        while (it.hasNext()) {
            arrayList.add(a(installedApplications, it.next()));
        }
        return arrayList;
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        if (this.c == 0 && this.f624b == 0) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            this.f624b = jCurrentTimeMillis;
            this.c = jCurrentTimeMillis - b.c.a.b.f.z;
        }
        List<b.c.a.b.b> listB = b();
        this.f = listB;
        this.g.a(this.e, this.h, listB);
    }
}
