package com.taobao.android.dinamicx;

import android.content.Context;
import anetwork.channel.Response;
import anetwork.channel.degrade.DegradableNetwork;
import anetwork.channel.entity.RequestImpl;
import anetwork.channel.util.RequestConstant;
import com.taobao.android.AliMonitorInterface;
import com.taobao.android.AliMonitorServiceFetcher;
import com.taobao.android.dinamicx.template.download.IDXDownloader;

public class DXHttpLoader implements IDXDownloader {
    public byte[] download(String str) {
        RequestImpl requestImpl = new RequestImpl(str);
        requestImpl.setExtProperty(RequestConstant.CHECK_CONTENT_LENGTH, "true");
        Response syncSend = new DegradableNetwork((Context) null).syncSend(requestImpl, (Object) null);
        if (syncSend.getStatusCode() == 200 && syncSend.getBytedata() != null && syncSend.getBytedata().length > 0) {
            return syncSend.getBytedata();
        }
        AliMonitorInterface monitorService = AliMonitorServiceFetcher.getMonitorService();
        if (monitorService != null) {
            monitorService.counter_commit("DinamicX", "DownloadTemplateError", "templateUrl=" + str + ",errorCode=" + syncSend.getStatusCode(), 1.0d);
        }
        return null;
    }
}
