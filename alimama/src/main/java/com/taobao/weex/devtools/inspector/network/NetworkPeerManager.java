package com.taobao.weex.devtools.inspector.network;

import android.content.Context;
import com.taobao.weex.devtools.common.Util;
import com.taobao.weex.devtools.inspector.helper.ChromePeerManager;
import com.taobao.weex.devtools.inspector.helper.PeersRegisteredListener;
import javax.annotation.Nullable;

public class NetworkPeerManager extends ChromePeerManager {
    private static NetworkPeerManager sInstance;
    /* access modifiers changed from: private */
    public AsyncPrettyPrinterRegistry mAsyncPrettyPrinterRegistry;
    /* access modifiers changed from: private */
    public AsyncPrettyPrinterInitializer mPrettyPrinterInitializer;
    /* access modifiers changed from: private */
    public final ResponseBodyFileManager mResponseBodyFileManager;
    private final PeersRegisteredListener mTempFileCleanup = new PeersRegisteredListener() {
        /* access modifiers changed from: protected */
        public void onFirstPeerRegistered() {
            AsyncPrettyPrinterExecutorHolder.ensureInitialized();
            if (NetworkPeerManager.this.mAsyncPrettyPrinterRegistry == null && NetworkPeerManager.this.mPrettyPrinterInitializer != null) {
                AsyncPrettyPrinterRegistry unused = NetworkPeerManager.this.mAsyncPrettyPrinterRegistry = new AsyncPrettyPrinterRegistry();
                NetworkPeerManager.this.mPrettyPrinterInitializer.populatePrettyPrinters(NetworkPeerManager.this.mAsyncPrettyPrinterRegistry);
            }
            NetworkPeerManager.this.mResponseBodyFileManager.cleanupFiles();
        }

        /* access modifiers changed from: protected */
        public void onLastPeerUnregistered() {
            NetworkPeerManager.this.mResponseBodyFileManager.cleanupFiles();
            AsyncPrettyPrinterExecutorHolder.shutdown();
        }
    };

    @Nullable
    public static synchronized NetworkPeerManager getInstanceOrNull() {
        NetworkPeerManager networkPeerManager;
        synchronized (NetworkPeerManager.class) {
            networkPeerManager = sInstance;
        }
        return networkPeerManager;
    }

    public static synchronized NetworkPeerManager getOrCreateInstance(Context context) {
        NetworkPeerManager networkPeerManager;
        synchronized (NetworkPeerManager.class) {
            if (sInstance == null) {
                sInstance = new NetworkPeerManager(new ResponseBodyFileManager(context.getApplicationContext()));
            }
            networkPeerManager = sInstance;
        }
        return networkPeerManager;
    }

    public NetworkPeerManager(ResponseBodyFileManager responseBodyFileManager) {
        this.mResponseBodyFileManager = responseBodyFileManager;
        setListener(this.mTempFileCleanup);
    }

    public ResponseBodyFileManager getResponseBodyFileManager() {
        return this.mResponseBodyFileManager;
    }

    @Nullable
    public AsyncPrettyPrinterRegistry getAsyncPrettyPrinterRegistry() {
        return this.mAsyncPrettyPrinterRegistry;
    }

    public void setPrettyPrinterInitializer(AsyncPrettyPrinterInitializer asyncPrettyPrinterInitializer) {
        Util.throwIfNotNull(this.mPrettyPrinterInitializer);
        this.mPrettyPrinterInitializer = (AsyncPrettyPrinterInitializer) Util.throwIfNull(asyncPrettyPrinterInitializer);
    }
}
