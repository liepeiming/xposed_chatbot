package com.xiaomi.push;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import androidx.core.app.NotificationCompat;
import com.xiaomi.channel.commonutils.logger.b;
import com.xiaomi.push.service.f;

class eq implements en {
    eq() {
    }

    private void a(Service service, Intent intent) {
        String stringExtra = intent.getStringExtra("awake_info");
        if (!TextUtils.isEmpty(stringExtra)) {
            String b = ef.b(stringExtra);
            if (!TextUtils.isEmpty(b)) {
                eg.a(service.getApplicationContext(), b, 1007, "play with service successfully");
                return;
            }
        }
        eg.a(service.getApplicationContext(), NotificationCompat.CATEGORY_SERVICE, 1008, "B get a incorrect message");
    }

    private void a(Context context, String str, String str2, String str3) {
        if (context == null || TextUtils.isEmpty(str) || TextUtils.isEmpty(str2) || TextUtils.isEmpty(str3)) {
            if (TextUtils.isEmpty(str3)) {
                eg.a(context, NotificationCompat.CATEGORY_SERVICE, 1008, "argument error");
            } else {
                eg.a(context, str3, 1008, "argument error");
            }
        } else if (!f.a(context, str, str2)) {
            eg.a(context, str3, 1003, "B is not ready");
        } else {
            eg.a(context, str3, 1002, "B is ready");
            eg.a(context, str3, 1004, "A is ready");
            try {
                Intent intent = new Intent();
                intent.setAction(str2);
                intent.setPackage(str);
                intent.putExtra("awake_info", ef.a(str3));
                if (context.startService(intent) != null) {
                    eg.a(context, str3, 1005, "A is successful");
                    eg.a(context, str3, 1006, "The job is finished");
                    return;
                }
                eg.a(context, str3, 1008, "A is fail to help B's service");
            } catch (Exception e) {
                b.a((Throwable) e);
                eg.a(context, str3, 1008, "A meet a exception when help B's service");
            }
        }
    }

    public void a(Context context, Intent intent, String str) {
        if (context == null || !(context instanceof Service)) {
            eg.a(context, NotificationCompat.CATEGORY_SERVICE, 1008, "A receive incorrect message");
        } else {
            a((Service) context, intent);
        }
    }

    public void a(Context context, ej ejVar) {
        if (ejVar != null) {
            a(context, ejVar.a(), ejVar.b(), ejVar.d());
        } else {
            eg.a(context, NotificationCompat.CATEGORY_SERVICE, 1008, "A receive incorrect message");
        }
    }
}
