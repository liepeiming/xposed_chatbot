package anetwork.channel.download;

import android.content.Context;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.SparseArray;
import anet.channel.util.ALog;
import anet.channel.util.StringUtils;
import anetwork.channel.Header;
import anetwork.channel.aidl.Connection;
import anetwork.channel.http.NetworkSdkSetting;
import com.taobao.android.dinamicx.template.utils.DXTemplateNamePathUtil;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadManager {
    static final String DOWNLOAD_FOLDER = "downloads";
    public static final int ERROR_DOWNLOAD_CANCELLED = -105;
    public static final int ERROR_EXCEPTION_HAPPEN = -104;
    public static final int ERROR_FILE_FOLDER_INVALID = -101;
    public static final int ERROR_FILE_RENAME_FAILED = -106;
    public static final int ERROR_IO_EXCEPTION = -103;
    public static final int ERROR_REQUEST_FAIL = -102;
    public static final int ERROR_URL_INVALID = -100;
    public static final String TAG = "anet.DownloadManager";
    Context context;
    ThreadPoolExecutor executor;
    AtomicInteger taskIdGen;
    SparseArray<DownloadTask> taskMap;

    public interface DownloadListener {
        void onFail(int i, int i2, String str);

        void onProgress(int i, long j, long j2);

        void onSuccess(int i, String str);
    }

    public static DownloadManager getInstance() {
        return ClassHolder.instance;
    }

    private DownloadManager() {
        this.taskMap = new SparseArray<>(6);
        this.taskIdGen = new AtomicInteger(0);
        this.executor = new ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS, new LinkedBlockingDeque());
        this.context = null;
        this.context = NetworkSdkSetting.getContext();
        this.executor.allowCoreThreadTimeOut(true);
        prepareDownloadFolder();
    }

    private static class ClassHolder {
        static DownloadManager instance = new DownloadManager();

        private ClassHolder() {
        }
    }

    public int enqueue(String str, String str2, DownloadListener downloadListener) {
        return enqueue(str, (String) null, str2, downloadListener);
    }

    public int enqueue(String str, String str2, String str3, DownloadListener downloadListener) {
        int i = 0;
        if (ALog.isPrintLog(2)) {
            ALog.i(TAG, "enqueue", (String) null, "folder", str2, "filename", str3, "url", str);
        }
        if (this.context == null) {
            ALog.e(TAG, "network sdk not initialized.", (String) null, new Object[0]);
            return -1;
        }
        try {
            URL url = new URL(str);
            if (TextUtils.isEmpty(str2) || prepareFolder(str2)) {
                synchronized (this.taskMap) {
                    int size = this.taskMap.size();
                    while (true) {
                        if (i >= size) {
                            break;
                        }
                        DownloadTask valueAt = this.taskMap.valueAt(i);
                        if (!url.equals(valueAt.url)) {
                            i++;
                        } else if (valueAt.attachListener(downloadListener)) {
                            int i2 = valueAt.taskId;
                            return i2;
                        }
                    }
                    DownloadTask downloadTask = new DownloadTask(url, str2, str3, downloadListener);
                    this.taskMap.put(downloadTask.taskId, downloadTask);
                    this.executor.submit(downloadTask);
                    int i3 = downloadTask.taskId;
                    return i3;
                }
            }
            ALog.e(TAG, "file folder invalid.", (String) null, new Object[0]);
            if (downloadListener != null) {
                downloadListener.onFail(-1, -101, "file folder path invalid");
            }
            return -1;
        } catch (MalformedURLException e) {
            ALog.e(TAG, "url invalid.", (String) null, e, new Object[0]);
            if (downloadListener != null) {
                downloadListener.onFail(-1, -100, "url invalid");
            }
            return -1;
        }
    }

    public void cancel(int i) {
        synchronized (this.taskMap) {
            DownloadTask downloadTask = this.taskMap.get(i);
            if (downloadTask != null) {
                if (ALog.isPrintLog(2)) {
                    ALog.i(TAG, "try cancel task" + i + " url=" + downloadTask.url.toString(), (String) null, new Object[0]);
                }
                this.taskMap.remove(i);
                downloadTask.cancel();
            }
        }
    }

    class DownloadTask implements Runnable {
        private volatile Connection conn = null;
        private final String filePath;
        private final AtomicBoolean isCancelled = new AtomicBoolean(false);
        private final AtomicBoolean isFinish = new AtomicBoolean(false);
        private final CopyOnWriteArrayList<DownloadListener> listenerList;
        final int taskId;
        final URL url;
        private boolean useExternalCache = true;

        DownloadTask(URL url2, String str, String str2, DownloadListener downloadListener) {
            this.taskId = DownloadManager.this.taskIdGen.getAndIncrement();
            this.url = url2;
            str2 = TextUtils.isEmpty(str2) ? parseFileNameForURL(url2) : str2;
            if (TextUtils.isEmpty(str)) {
                this.filePath = DownloadManager.this.getDownloadFilePath(str2);
            } else {
                if (str.endsWith("/")) {
                    this.filePath = str + str2;
                } else {
                    this.filePath = str + DXTemplateNamePathUtil.DIR + str2;
                }
                if (str.startsWith("/data/user") || str.startsWith("/data/data")) {
                    this.useExternalCache = false;
                }
            }
            this.listenerList = new CopyOnWriteArrayList<>();
            this.listenerList.add(downloadListener);
        }

        public boolean attachListener(DownloadListener downloadListener) {
            if (this.isFinish.get()) {
                return false;
            }
            this.listenerList.add(downloadListener);
            return true;
        }

        public void cancel() {
            this.isCancelled.set(true);
            notifyFail(-105, "download canceled.");
            if (this.conn != null) {
                try {
                    this.conn.cancel();
                } catch (RemoteException unused) {
                }
            }
        }

        /*  JADX ERROR: IndexOutOfBoundsException in pass: RegionMakerVisitor
            java.lang.IndexOutOfBoundsException: Index: 0, Size: 0
            	at java.util.ArrayList.rangeCheck(Unknown Source)
            	at java.util.ArrayList.get(Unknown Source)
            	at jadx.core.dex.nodes.InsnNode.getArg(InsnNode.java:101)
            	at jadx.core.dex.visitors.regions.RegionMaker.traverseMonitorExits(RegionMaker.java:611)
            	at jadx.core.dex.visitors.regions.RegionMaker.traverseMonitorExits(RegionMaker.java:619)
            	at jadx.core.dex.visitors.regions.RegionMaker.traverseMonitorExits(RegionMaker.java:619)
            	at jadx.core.dex.visitors.regions.RegionMaker.traverseMonitorExits(RegionMaker.java:619)
            	at jadx.core.dex.visitors.regions.RegionMaker.processMonitorEnter(RegionMaker.java:561)
            	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:133)
            	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:86)
            	at jadx.core.dex.visitors.regions.RegionMaker.processExcHandler(RegionMaker.java:1043)
            	at jadx.core.dex.visitors.regions.RegionMaker.processTryCatchBlocks(RegionMaker.java:975)
            	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:52)
            */
        /* JADX WARNING: Removed duplicated region for block: B:185:0x023b A[SYNTHETIC, Splitter:B:185:0x023b] */
        /* JADX WARNING: Removed duplicated region for block: B:189:0x0240 A[SYNTHETIC, Splitter:B:189:0x0240] */
        /* JADX WARNING: Removed duplicated region for block: B:193:0x0245 A[SYNTHETIC, Splitter:B:193:0x0245] */
        public void run() {
            /*
                r15 = this;
                java.util.concurrent.atomic.AtomicBoolean r0 = r15.isCancelled
                boolean r0 = r0.get()
                if (r0 == 0) goto L_0x0009
                return
            L_0x0009:
                r0 = 0
                r1 = 0
                anetwork.channel.download.DownloadManager r2 = anetwork.channel.download.DownloadManager.this     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                java.net.URL r3 = r15.url     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                java.lang.String r3 = r3.toString()     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                boolean r4 = r15.useExternalCache     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                java.io.File r2 = r2.getTempFile(r3, r4)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                boolean r3 = r2.exists()     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                anetwork.channel.entity.RequestImpl r4 = new anetwork.channel.entity.RequestImpl     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                java.net.URL r5 = r15.url     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                r4.<init>((java.net.URL) r5)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                r4.setRetryTime(r0)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                r5 = 1
                r4.setFollowRedirects(r5)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                if (r3 == 0) goto L_0x004c
                java.lang.String r5 = "Range"
                java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                r6.<init>()     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                java.lang.String r7 = "bytes="
                r6.append(r7)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                long r7 = r2.length()     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                r6.append(r7)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                java.lang.String r7 = "-"
                r6.append(r7)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                java.lang.String r6 = r6.toString()     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                r4.addHeader(r5, r6)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
            L_0x004c:
                anetwork.channel.degrade.DegradableNetwork r5 = new anetwork.channel.degrade.DegradableNetwork     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                anetwork.channel.download.DownloadManager r6 = anetwork.channel.download.DownloadManager.this     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                android.content.Context r6 = r6.context     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                r5.<init>(r6)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                anetwork.channel.aidl.Connection r6 = r5.getConnection(r4, r1)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                r15.conn = r6     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                anetwork.channel.aidl.Connection r6 = r15.conn     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                int r6 = r6.getStatusCode()     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                if (r6 <= 0) goto L_0x01f6
                r7 = 416(0x1a0, float:5.83E-43)
                r8 = 200(0xc8, float:2.8E-43)
                if (r6 == r8) goto L_0x0071
                r9 = 206(0xce, float:2.89E-43)
                if (r6 == r9) goto L_0x0071
                if (r6 == r7) goto L_0x0071
                goto L_0x01f6
            L_0x0071:
                if (r3 == 0) goto L_0x00a1
                if (r6 != r7) goto L_0x009e
                java.util.List r3 = r4.getHeaders()     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                r15.removeRangeHeader(r3)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                java.util.concurrent.atomic.AtomicBoolean r3 = r15.isCancelled     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                boolean r3 = r3.get()     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                if (r3 == 0) goto L_0x0097
                anetwork.channel.download.DownloadManager r0 = anetwork.channel.download.DownloadManager.this
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r3 = r0.taskMap
                monitor-enter(r3)
                anetwork.channel.download.DownloadManager r0 = anetwork.channel.download.DownloadManager.this     // Catch:{ all -> 0x0094 }
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r0 = r0.taskMap     // Catch:{ all -> 0x0094 }
                int r1 = r15.taskId     // Catch:{ all -> 0x0094 }
                r0.remove(r1)     // Catch:{ all -> 0x0094 }
                monitor-exit(r3)     // Catch:{ all -> 0x0094 }
                return
            L_0x0094:
                r0 = move-exception
                monitor-exit(r3)     // Catch:{ all -> 0x0094 }
                throw r0
            L_0x0097:
                anetwork.channel.aidl.Connection r3 = r5.getConnection(r4, r1)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                r15.conn = r3     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                r3 = 0
            L_0x009e:
                if (r6 != r8) goto L_0x00a1
                r3 = 0
            L_0x00a1:
                java.util.concurrent.atomic.AtomicBoolean r4 = r15.isCancelled     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                boolean r4 = r4.get()     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                if (r4 == 0) goto L_0x00bc
                anetwork.channel.download.DownloadManager r0 = anetwork.channel.download.DownloadManager.this
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r4 = r0.taskMap
                monitor-enter(r4)
                anetwork.channel.download.DownloadManager r0 = anetwork.channel.download.DownloadManager.this     // Catch:{ all -> 0x00b9 }
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r0 = r0.taskMap     // Catch:{ all -> 0x00b9 }
                int r1 = r15.taskId     // Catch:{ all -> 0x00b9 }
                r0.remove(r1)     // Catch:{ all -> 0x00b9 }
                monitor-exit(r4)     // Catch:{ all -> 0x00b9 }
                return
            L_0x00b9:
                r0 = move-exception
                monitor-exit(r4)     // Catch:{ all -> 0x00b9 }
                throw r0
            L_0x00bc:
                r4 = 0
                if (r3 != 0) goto L_0x00cc
                java.io.BufferedOutputStream r3 = new java.io.BufferedOutputStream     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                java.io.FileOutputStream r7 = new java.io.FileOutputStream     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                r7.<init>(r2)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                r3.<init>(r7)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                r7 = r1
                goto L_0x00ea
            L_0x00cc:
                java.io.RandomAccessFile r3 = new java.io.RandomAccessFile     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                java.lang.String r4 = "rw"
                r3.<init>(r2, r4)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                long r4 = r3.length()     // Catch:{ Exception -> 0x01f1, all -> 0x01ec }
                r3.seek(r4)     // Catch:{ Exception -> 0x01f1, all -> 0x01ec }
                java.io.BufferedOutputStream r7 = new java.io.BufferedOutputStream     // Catch:{ Exception -> 0x01f1, all -> 0x01ec }
                java.nio.channels.FileChannel r8 = r3.getChannel()     // Catch:{ Exception -> 0x01f1, all -> 0x01ec }
                java.io.OutputStream r8 = java.nio.channels.Channels.newOutputStream(r8)     // Catch:{ Exception -> 0x01f1, all -> 0x01ec }
                r7.<init>(r8)     // Catch:{ Exception -> 0x01f1, all -> 0x01ec }
                r14 = r7
                r7 = r3
                r3 = r14
            L_0x00ea:
                anetwork.channel.aidl.Connection r8 = r15.conn     // Catch:{ Exception -> 0x01e9, all -> 0x01e5 }
                java.util.Map r8 = r8.getConnHeadFields()     // Catch:{ Exception -> 0x01e9, all -> 0x01e5 }
                long r8 = r15.parseContentLength(r6, r8, r4)     // Catch:{ Exception -> 0x01e9, all -> 0x01e5 }
                anetwork.channel.aidl.Connection r6 = r15.conn     // Catch:{ Exception -> 0x01e9, all -> 0x01e5 }
                anetwork.channel.aidl.ParcelableInputStream r6 = r6.getInputStream()     // Catch:{ Exception -> 0x01e9, all -> 0x01e5 }
                if (r6 != 0) goto L_0x0126
                r2 = -103(0xffffffffffffff99, float:NaN)
                java.lang.String r4 = "input stream is null."
                r15.notifyFail(r2, r4)     // Catch:{ Exception -> 0x0123 }
                r3.close()     // Catch:{ Exception -> 0x0106 }
            L_0x0106:
                if (r7 == 0) goto L_0x010b
                r7.close()     // Catch:{ Exception -> 0x010b }
            L_0x010b:
                if (r6 == 0) goto L_0x0110
                r6.close()     // Catch:{ Exception -> 0x0110 }
            L_0x0110:
                anetwork.channel.download.DownloadManager r0 = anetwork.channel.download.DownloadManager.this
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r0 = r0.taskMap
                monitor-enter(r0)
                anetwork.channel.download.DownloadManager r1 = anetwork.channel.download.DownloadManager.this     // Catch:{ all -> 0x0120 }
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r1 = r1.taskMap     // Catch:{ all -> 0x0120 }
                int r2 = r15.taskId     // Catch:{ all -> 0x0120 }
                r1.remove(r2)     // Catch:{ all -> 0x0120 }
                monitor-exit(r0)     // Catch:{ all -> 0x0120 }
                return
            L_0x0120:
                r1 = move-exception
                monitor-exit(r0)     // Catch:{ all -> 0x0120 }
                throw r1
            L_0x0123:
                r2 = move-exception
                goto L_0x0227
            L_0x0126:
                r10 = 2048(0x800, float:2.87E-42)
                byte[] r10 = new byte[r10]     // Catch:{ Exception -> 0x0123 }
                r11 = 0
            L_0x012b:
                int r12 = r6.read(r10)     // Catch:{ Exception -> 0x0123 }
                r13 = -1
                if (r12 == r13) goto L_0x0169
                java.util.concurrent.atomic.AtomicBoolean r13 = r15.isCancelled     // Catch:{ Exception -> 0x0123 }
                boolean r13 = r13.get()     // Catch:{ Exception -> 0x0123 }
                if (r13 == 0) goto L_0x015f
                anetwork.channel.aidl.Connection r2 = r15.conn     // Catch:{ Exception -> 0x0123 }
                r2.cancel()     // Catch:{ Exception -> 0x0123 }
                r3.close()     // Catch:{ Exception -> 0x0142 }
            L_0x0142:
                if (r7 == 0) goto L_0x0147
                r7.close()     // Catch:{ Exception -> 0x0147 }
            L_0x0147:
                if (r6 == 0) goto L_0x014c
                r6.close()     // Catch:{ Exception -> 0x014c }
            L_0x014c:
                anetwork.channel.download.DownloadManager r0 = anetwork.channel.download.DownloadManager.this
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r0 = r0.taskMap
                monitor-enter(r0)
                anetwork.channel.download.DownloadManager r1 = anetwork.channel.download.DownloadManager.this     // Catch:{ all -> 0x015c }
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r1 = r1.taskMap     // Catch:{ all -> 0x015c }
                int r2 = r15.taskId     // Catch:{ all -> 0x015c }
                r1.remove(r2)     // Catch:{ all -> 0x015c }
                monitor-exit(r0)     // Catch:{ all -> 0x015c }
                return
            L_0x015c:
                r1 = move-exception
                monitor-exit(r0)     // Catch:{ all -> 0x015c }
                throw r1
            L_0x015f:
                int r11 = r11 + r12
                r3.write(r10, r0, r12)     // Catch:{ Exception -> 0x0123 }
                long r12 = (long) r11     // Catch:{ Exception -> 0x0123 }
                long r12 = r12 + r4
                r15.notifyProgress(r12, r8)     // Catch:{ Exception -> 0x0123 }
                goto L_0x012b
            L_0x0169:
                r3.flush()     // Catch:{ Exception -> 0x0123 }
                java.util.concurrent.atomic.AtomicBoolean r4 = r15.isCancelled     // Catch:{ Exception -> 0x0123 }
                boolean r4 = r4.get()     // Catch:{ Exception -> 0x0123 }
                if (r4 == 0) goto L_0x0194
                r3.close()     // Catch:{ Exception -> 0x0177 }
            L_0x0177:
                if (r7 == 0) goto L_0x017c
                r7.close()     // Catch:{ Exception -> 0x017c }
            L_0x017c:
                if (r6 == 0) goto L_0x0181
                r6.close()     // Catch:{ Exception -> 0x0181 }
            L_0x0181:
                anetwork.channel.download.DownloadManager r0 = anetwork.channel.download.DownloadManager.this
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r4 = r0.taskMap
                monitor-enter(r4)
                anetwork.channel.download.DownloadManager r0 = anetwork.channel.download.DownloadManager.this     // Catch:{ all -> 0x0191 }
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r0 = r0.taskMap     // Catch:{ all -> 0x0191 }
                int r1 = r15.taskId     // Catch:{ all -> 0x0191 }
                r0.remove(r1)     // Catch:{ all -> 0x0191 }
                monitor-exit(r4)     // Catch:{ all -> 0x0191 }
                return
            L_0x0191:
                r0 = move-exception
                monitor-exit(r4)     // Catch:{ all -> 0x0191 }
                throw r0
            L_0x0194:
                java.io.File r4 = new java.io.File     // Catch:{ Exception -> 0x0123 }
                java.lang.String r5 = r15.filePath     // Catch:{ Exception -> 0x0123 }
                r4.<init>(r5)     // Catch:{ Exception -> 0x0123 }
                boolean r2 = r2.renameTo(r4)     // Catch:{ Exception -> 0x0123 }
                if (r2 == 0) goto L_0x01a7
                java.lang.String r2 = r15.filePath     // Catch:{ Exception -> 0x0123 }
                r15.notifySuccess(r2)     // Catch:{ Exception -> 0x0123 }
                goto L_0x01c4
            L_0x01a7:
                r2 = -106(0xffffffffffffff96, float:NaN)
                java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0123 }
                r4.<init>()     // Catch:{ Exception -> 0x0123 }
                java.lang.String r5 = "file rename to "
                r4.append(r5)     // Catch:{ Exception -> 0x0123 }
                java.lang.String r5 = r15.filePath     // Catch:{ Exception -> 0x0123 }
                r4.append(r5)     // Catch:{ Exception -> 0x0123 }
                java.lang.String r5 = " failed"
                r4.append(r5)     // Catch:{ Exception -> 0x0123 }
                java.lang.String r4 = r4.toString()     // Catch:{ Exception -> 0x0123 }
                r15.notifyFail(r2, r4)     // Catch:{ Exception -> 0x0123 }
            L_0x01c4:
                r3.close()     // Catch:{ Exception -> 0x01c7 }
            L_0x01c7:
                if (r7 == 0) goto L_0x01cc
                r7.close()     // Catch:{ Exception -> 0x01cc }
            L_0x01cc:
                if (r6 == 0) goto L_0x01d1
                r6.close()     // Catch:{ Exception -> 0x01d1 }
            L_0x01d1:
                anetwork.channel.download.DownloadManager r0 = anetwork.channel.download.DownloadManager.this
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r0 = r0.taskMap
                monitor-enter(r0)
                anetwork.channel.download.DownloadManager r1 = anetwork.channel.download.DownloadManager.this     // Catch:{ all -> 0x01e2 }
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r1 = r1.taskMap     // Catch:{ all -> 0x01e2 }
                int r2 = r15.taskId     // Catch:{ all -> 0x01e2 }
                r1.remove(r2)     // Catch:{ all -> 0x01e2 }
                monitor-exit(r0)     // Catch:{ all -> 0x01e2 }
                goto L_0x0257
            L_0x01e2:
                r1 = move-exception
                monitor-exit(r0)     // Catch:{ all -> 0x01e2 }
                throw r1
            L_0x01e5:
                r0 = move-exception
                r6 = r1
                goto L_0x025c
            L_0x01e9:
                r2 = move-exception
                r6 = r1
                goto L_0x0227
            L_0x01ec:
                r0 = move-exception
                r6 = r1
                r7 = r3
                goto L_0x025d
            L_0x01f1:
                r2 = move-exception
                r6 = r1
                r7 = r3
                r3 = r6
                goto L_0x0227
            L_0x01f6:
                r2 = -102(0xffffffffffffff9a, float:NaN)
                java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                r3.<init>()     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                java.lang.String r4 = "ResponseCode:"
                r3.append(r4)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                r3.append(r6)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                java.lang.String r3 = r3.toString()     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                r15.notifyFail(r2, r3)     // Catch:{ Exception -> 0x0223, all -> 0x021f }
                anetwork.channel.download.DownloadManager r0 = anetwork.channel.download.DownloadManager.this
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r0 = r0.taskMap
                monitor-enter(r0)
                anetwork.channel.download.DownloadManager r1 = anetwork.channel.download.DownloadManager.this     // Catch:{ all -> 0x021c }
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r1 = r1.taskMap     // Catch:{ all -> 0x021c }
                int r2 = r15.taskId     // Catch:{ all -> 0x021c }
                r1.remove(r2)     // Catch:{ all -> 0x021c }
                monitor-exit(r0)     // Catch:{ all -> 0x021c }
                return
            L_0x021c:
                r1 = move-exception
                monitor-exit(r0)     // Catch:{ all -> 0x021c }
                throw r1
            L_0x021f:
                r0 = move-exception
                r6 = r1
                r7 = r6
                goto L_0x025d
            L_0x0223:
                r2 = move-exception
                r3 = r1
                r6 = r3
                r7 = r6
            L_0x0227:
                java.lang.String r4 = "anet.DownloadManager"
                java.lang.String r5 = "file download failed!"
                java.lang.Object[] r0 = new java.lang.Object[r0]     // Catch:{ all -> 0x025b }
                anet.channel.util.ALog.e(r4, r5, r1, r2, r0)     // Catch:{ all -> 0x025b }
                r0 = -104(0xffffffffffffff98, float:NaN)
                java.lang.String r1 = r2.toString()     // Catch:{ all -> 0x025b }
                r15.notifyFail(r0, r1)     // Catch:{ all -> 0x025b }
                if (r3 == 0) goto L_0x023e
                r3.close()     // Catch:{ Exception -> 0x023e }
            L_0x023e:
                if (r7 == 0) goto L_0x0243
                r7.close()     // Catch:{ Exception -> 0x0243 }
            L_0x0243:
                if (r6 == 0) goto L_0x0248
                r6.close()     // Catch:{ Exception -> 0x0248 }
            L_0x0248:
                anetwork.channel.download.DownloadManager r0 = anetwork.channel.download.DownloadManager.this
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r0 = r0.taskMap
                monitor-enter(r0)
                anetwork.channel.download.DownloadManager r1 = anetwork.channel.download.DownloadManager.this     // Catch:{ all -> 0x0258 }
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r1 = r1.taskMap     // Catch:{ all -> 0x0258 }
                int r2 = r15.taskId     // Catch:{ all -> 0x0258 }
                r1.remove(r2)     // Catch:{ all -> 0x0258 }
                monitor-exit(r0)     // Catch:{ all -> 0x0258 }
            L_0x0257:
                return
            L_0x0258:
                r1 = move-exception
                monitor-exit(r0)     // Catch:{ all -> 0x0258 }
                throw r1
            L_0x025b:
                r0 = move-exception
            L_0x025c:
                r1 = r3
            L_0x025d:
                if (r1 == 0) goto L_0x0262
                r1.close()     // Catch:{ Exception -> 0x0262 }
            L_0x0262:
                if (r7 == 0) goto L_0x0267
                r7.close()     // Catch:{ Exception -> 0x0267 }
            L_0x0267:
                if (r6 == 0) goto L_0x026c
                r6.close()     // Catch:{ Exception -> 0x026c }
            L_0x026c:
                anetwork.channel.download.DownloadManager r1 = anetwork.channel.download.DownloadManager.this
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r1 = r1.taskMap
                monitor-enter(r1)
                anetwork.channel.download.DownloadManager r2 = anetwork.channel.download.DownloadManager.this     // Catch:{ all -> 0x027c }
                android.util.SparseArray<anetwork.channel.download.DownloadManager$DownloadTask> r2 = r2.taskMap     // Catch:{ all -> 0x027c }
                int r3 = r15.taskId     // Catch:{ all -> 0x027c }
                r2.remove(r3)     // Catch:{ all -> 0x027c }
                monitor-exit(r1)     // Catch:{ all -> 0x027c }
                throw r0
            L_0x027c:
                r0 = move-exception
                monitor-exit(r1)     // Catch:{ all -> 0x027c }
                throw r0
            */
            throw new UnsupportedOperationException("Method not decompiled: anetwork.channel.download.DownloadManager.DownloadTask.run():void");
        }

        private void notifySuccess(String str) {
            if (this.isFinish.compareAndSet(false, true)) {
                Iterator<DownloadListener> it = this.listenerList.iterator();
                while (it.hasNext()) {
                    it.next().onSuccess(this.taskId, str);
                }
            }
        }

        private void notifyFail(int i, String str) {
            if (this.isFinish.compareAndSet(false, true)) {
                Iterator<DownloadListener> it = this.listenerList.iterator();
                while (it.hasNext()) {
                    it.next().onFail(this.taskId, i, str);
                }
            }
        }

        private void notifyProgress(long j, long j2) {
            if (!this.isFinish.get()) {
                Iterator<DownloadListener> it = this.listenerList.iterator();
                while (it.hasNext()) {
                    it.next().onProgress(this.taskId, j, j2);
                }
            }
        }

        /* JADX WARNING: Code restructure failed: missing block: B:9:0x001e, code lost:
            r0 = r6.lastIndexOf(47);
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private long parseContentLength(int r6, java.util.Map<java.lang.String, java.util.List<java.lang.String>> r7, long r8) {
            /*
                r5 = this;
                r0 = 200(0xc8, float:2.8E-43)
                r1 = 0
                if (r6 != r0) goto L_0x0012
                java.lang.String r6 = "Content-Length"
                java.lang.String r6 = anet.channel.util.HttpHelper.getSingleHeaderFieldByKey(r7, r6)     // Catch:{ Exception -> 0x0046 }
                long r6 = java.lang.Long.parseLong(r6)     // Catch:{ Exception -> 0x0046 }
                r1 = r6
                goto L_0x0046
            L_0x0012:
                r0 = 206(0xce, float:2.89E-43)
                if (r6 != r0) goto L_0x0046
                java.lang.String r6 = "Content-Range"
                java.lang.String r6 = anet.channel.util.HttpHelper.getSingleHeaderFieldByKey(r7, r6)     // Catch:{ Exception -> 0x0046 }
                if (r6 == 0) goto L_0x0032
                r0 = 47
                int r0 = r6.lastIndexOf(r0)     // Catch:{ Exception -> 0x0046 }
                r3 = -1
                if (r0 == r3) goto L_0x0032
                int r0 = r0 + 1
                java.lang.String r6 = r6.substring(r0)     // Catch:{ Exception -> 0x0046 }
                long r3 = java.lang.Long.parseLong(r6)     // Catch:{ Exception -> 0x0046 }
                goto L_0x0033
            L_0x0032:
                r3 = r1
            L_0x0033:
                int r6 = (r3 > r1 ? 1 : (r3 == r1 ? 0 : -1))
                if (r6 != 0) goto L_0x0045
                java.lang.String r6 = "Content-Length"
                java.lang.String r6 = anet.channel.util.HttpHelper.getSingleHeaderFieldByKey(r7, r6)     // Catch:{ Exception -> 0x0045 }
                long r6 = java.lang.Long.parseLong(r6)     // Catch:{ Exception -> 0x0045 }
                r0 = 0
                long r1 = r6 + r8
                goto L_0x0046
            L_0x0045:
                r1 = r3
            L_0x0046:
                return r1
            */
            throw new UnsupportedOperationException("Method not decompiled: anetwork.channel.download.DownloadManager.DownloadTask.parseContentLength(int, java.util.Map, long):long");
        }

        private void removeRangeHeader(List<Header> list) {
            if (list != null) {
                ListIterator<Header> listIterator = list.listIterator();
                while (listIterator.hasNext()) {
                    if ("Range".equalsIgnoreCase(listIterator.next().getName())) {
                        listIterator.remove();
                        return;
                    }
                }
            }
        }

        private String parseFileNameForURL(URL url2) {
            String path = url2.getPath();
            int lastIndexOf = path.lastIndexOf(47);
            String substring = lastIndexOf != -1 ? path.substring(lastIndexOf + 1, path.length()) : null;
            if (!TextUtils.isEmpty(substring)) {
                return substring;
            }
            String md5ToHex = StringUtils.md5ToHex(url2.toString());
            return md5ToHex == null ? url2.getFile() : md5ToHex;
        }
    }

    private void prepareDownloadFolder() {
        if (this.context != null) {
            File file = new File(this.context.getExternalFilesDir((String) null), DOWNLOAD_FOLDER);
            if (!file.exists()) {
                file.mkdir();
            }
        }
    }

    private boolean prepareFolder(String str) {
        if (this.context != null) {
            try {
                File file = new File(str);
                if (!file.exists()) {
                    return file.mkdir();
                }
                return true;
            } catch (Exception unused) {
                ALog.e(TAG, "create folder failed", (String) null, "folder", str);
            }
        }
        return false;
    }

    /* access modifiers changed from: private */
    public String getDownloadFilePath(String str) {
        StringBuilder sb = new StringBuilder(32);
        sb.append(this.context.getExternalFilesDir((String) null));
        sb.append("/");
        sb.append(DOWNLOAD_FOLDER);
        sb.append("/");
        sb.append(str);
        return sb.toString();
    }

    /* access modifiers changed from: private */
    public File getTempFile(String str, boolean z) {
        String md5ToHex = StringUtils.md5ToHex(str);
        if (md5ToHex != null) {
            str = md5ToHex;
        }
        if (z) {
            return new File(this.context.getExternalCacheDir(), str);
        }
        return new File(this.context.getCacheDir(), str);
    }
}
