package com.xiaomi.miui.pushads.sdk;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import com.xiaomi.push.cg;
import com.xiaomi.push.cj;

public class MiPushRelayTraceService extends Service {
    private static cj a;

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        a = cj.a();
    }

    public int onStartCommand(Intent intent, int i, int i2) {
        super.onStartCommand(intent, i, i2);
        if (intent == null) {
            return 0;
        }
        Bundle extras = intent.getExtras();
        int i3 = extras.getInt("intenttype");
        long j = extras.getLong("id");
        int i4 = extras.getInt("showType");
        cg cgVar = new cg();
        cgVar.f176a = j;
        cgVar.a = i4;
        cgVar.f177a = "";
        if (a == null) {
            Log.e("MiPushRelayTraceService", "log sender is null!");
            return 0;
        }
        switch (i3) {
            case 1:
                l.a("deleteT:");
                a.b(cgVar);
                break;
            case 2:
            case 3:
                l.a("clickT:");
                a.a(cgVar);
                PendingIntent pendingIntent = (PendingIntent) intent.getParcelableExtra("pendingintent");
                if (pendingIntent != null) {
                    try {
                        pendingIntent.send();
                        break;
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                break;
        }
        int i5 = extras.getInt("notifyid", 0);
        if (i5 != 0) {
            l.a("action，remove noti");
            ((NotificationManager) getSystemService("notification")).cancel(i5);
            try {
                Object systemService = getSystemService((String) Context.class.getField("STATUS_BAR_SERVICE").get((Object) null));
                systemService.getClass().getMethod("collapse", new Class[0]).invoke(systemService, new Object[0]);
                k.a("关闭status bar 成功");
            } catch (Exception unused) {
                Log.e("ads-notify-fd5dfce4", "Reflect failed");
            }
        }
        return 0;
    }
}
