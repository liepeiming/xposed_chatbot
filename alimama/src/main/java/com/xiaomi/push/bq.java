package com.xiaomi.push;

import android.content.Context;
import android.text.TextUtils;
import com.xiaomi.channel.commonutils.logger.b;

public class bq extends bs {
    public bq(String str, String str2, String[] strArr, String str3) {
        super(str, str2, strArr, str3);
    }

    public static bq a(Context context, String str, int i) {
        b.b("delete  messages when db size is too bigger");
        String a = bw.a(context).a(str);
        if (TextUtils.isEmpty(a)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("rowDataId in (select ");
        sb.append("rowDataId from " + a);
        sb.append(" order by createTimeStamp asc");
        sb.append(" limit ?)");
        return new bq(str, sb.toString(), new String[]{String.valueOf(i)}, "a job build to delete history message");
    }

    private void a(long j) {
        if (this.f168a != null && this.f168a.length > 0) {
            this.f168a[0] = String.valueOf(j);
        }
    }

    public void a(Context context, Object obj) {
        if (obj instanceof Long) {
            long longValue = ((Long) obj).longValue();
            long a = cc.a(a());
            long j = bo.f151a;
            if (a > j) {
                double d = (double) (a - j);
                Double.isNaN(d);
                double d2 = (double) j;
                Double.isNaN(d2);
                double d3 = (double) longValue;
                Double.isNaN(d3);
                long j2 = (long) (((d * 1.2d) / d2) * d3);
                a(j2);
                bk a2 = bk.a(context);
                a2.a("begin delete " + j2 + "noUpload messages , because db size is " + a + "B");
                super.a(context, obj);
                return;
            }
            b.b("db size is suitable");
        }
    }
}
