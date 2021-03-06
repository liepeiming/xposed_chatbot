package com.facebook.datasource;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.taobao.weex.ui.component.WXBasicComponentType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class IncreasingQualityDataSourceSupplier<T> implements Supplier<DataSource<T>> {
    /* access modifiers changed from: private */
    public final List<Supplier<DataSource<T>>> mDataSourceSuppliers;

    private IncreasingQualityDataSourceSupplier(List<Supplier<DataSource<T>>> list) {
        Preconditions.checkArgument(!list.isEmpty(), "List of suppliers is empty!");
        this.mDataSourceSuppliers = list;
    }

    public static <T> IncreasingQualityDataSourceSupplier<T> create(List<Supplier<DataSource<T>>> list) {
        return new IncreasingQualityDataSourceSupplier<>(list);
    }

    public DataSource<T> get() {
        return new IncreasingQualityDataSource();
    }

    public int hashCode() {
        return this.mDataSourceSuppliers.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof IncreasingQualityDataSourceSupplier)) {
            return false;
        }
        return Objects.equal(this.mDataSourceSuppliers, ((IncreasingQualityDataSourceSupplier) obj).mDataSourceSuppliers);
    }

    public String toString() {
        return Objects.toStringHelper((Object) this).add(WXBasicComponentType.LIST, (Object) this.mDataSourceSuppliers).toString();
    }

    @ThreadSafe
    private class IncreasingQualityDataSource extends AbstractDataSource<T> {
        @GuardedBy("IncreasingQualityDataSource.this")
        @Nullable
        private ArrayList<DataSource<T>> mDataSources;
        @Nullable
        private Throwable mDelayedError;
        private final AtomicInteger mFinishedDataSources = new AtomicInteger(0);
        @GuardedBy("IncreasingQualityDataSource.this")
        private int mIndexOfDataSourceWithResult;
        private final int mNumberOfDataSources;

        public IncreasingQualityDataSource() {
            int i = 0;
            int size = IncreasingQualityDataSourceSupplier.this.mDataSourceSuppliers.size();
            this.mNumberOfDataSources = size;
            this.mIndexOfDataSourceWithResult = size;
            this.mDataSources = new ArrayList<>(size);
            while (i < size) {
                DataSource dataSource = (DataSource) ((Supplier) IncreasingQualityDataSourceSupplier.this.mDataSourceSuppliers.get(i)).get();
                this.mDataSources.add(dataSource);
                dataSource.subscribe(new InternalDataSubscriber(i), CallerThreadExecutor.getInstance());
                if (!dataSource.hasResult()) {
                    i++;
                } else {
                    return;
                }
            }
        }

        @Nullable
        private synchronized DataSource<T> getDataSource(int i) {
            return (this.mDataSources == null || i >= this.mDataSources.size()) ? null : this.mDataSources.get(i);
        }

        @Nullable
        private synchronized DataSource<T> getAndClearDataSource(int i) {
            DataSource<T> dataSource;
            dataSource = null;
            if (this.mDataSources != null && i < this.mDataSources.size()) {
                dataSource = this.mDataSources.set(i, (Object) null);
            }
            return dataSource;
        }

        @Nullable
        private synchronized DataSource<T> getDataSourceWithResult() {
            return getDataSource(this.mIndexOfDataSourceWithResult);
        }

        @Nullable
        public synchronized T getResult() {
            DataSource dataSourceWithResult;
            dataSourceWithResult = getDataSourceWithResult();
            return dataSourceWithResult != null ? dataSourceWithResult.getResult() : null;
        }

        public synchronized boolean hasResult() {
            DataSource dataSourceWithResult;
            dataSourceWithResult = getDataSourceWithResult();
            return dataSourceWithResult != null && dataSourceWithResult.hasResult();
        }

        /* JADX WARNING: Code restructure failed: missing block: B:10:0x0016, code lost:
            if (r1 >= r0.size()) goto L_0x0024;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:11:0x0018, code lost:
            closeSafely(r0.get(r1));
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:20:?, code lost:
            return true;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:21:?, code lost:
            return true;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:8:0x0010, code lost:
            if (r0 == null) goto L_?;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean close() {
            /*
                r3 = this;
                monitor-enter(r3)
                boolean r0 = super.close()     // Catch:{ all -> 0x0026 }
                r1 = 0
                if (r0 != 0) goto L_0x000a
                monitor-exit(r3)     // Catch:{ all -> 0x0026 }
                return r1
            L_0x000a:
                java.util.ArrayList<com.facebook.datasource.DataSource<T>> r0 = r3.mDataSources     // Catch:{ all -> 0x0026 }
                r2 = 0
                r3.mDataSources = r2     // Catch:{ all -> 0x0026 }
                monitor-exit(r3)     // Catch:{ all -> 0x0026 }
                if (r0 == 0) goto L_0x0024
            L_0x0012:
                int r2 = r0.size()
                if (r1 >= r2) goto L_0x0024
                java.lang.Object r2 = r0.get(r1)
                com.facebook.datasource.DataSource r2 = (com.facebook.datasource.DataSource) r2
                r3.closeSafely(r2)
                int r1 = r1 + 1
                goto L_0x0012
            L_0x0024:
                r0 = 1
                return r0
            L_0x0026:
                r0 = move-exception
                monitor-exit(r3)     // Catch:{ all -> 0x0026 }
                throw r0
            */
            throw new UnsupportedOperationException("Method not decompiled: com.facebook.datasource.IncreasingQualityDataSourceSupplier.IncreasingQualityDataSource.close():boolean");
        }

        /* access modifiers changed from: private */
        public void onDataSourceNewResult(int i, DataSource<T> dataSource) {
            maybeSetIndexOfDataSourceWithResult(i, dataSource, dataSource.isFinished());
            if (dataSource == getDataSourceWithResult()) {
                setResult(null, i == 0 && dataSource.isFinished());
            }
            maybeSetFailure();
        }

        /* access modifiers changed from: private */
        public void onDataSourceFailed(int i, DataSource<T> dataSource) {
            closeSafely(tryGetAndClearDataSource(i, dataSource));
            if (i == 0) {
                this.mDelayedError = dataSource.getFailureCause();
            }
            maybeSetFailure();
        }

        private void maybeSetFailure() {
            if (this.mFinishedDataSources.incrementAndGet() == this.mNumberOfDataSources && this.mDelayedError != null) {
                setFailure(this.mDelayedError);
            }
        }

        /* JADX WARNING: Code restructure failed: missing block: B:16:0x0022, code lost:
            if (r0 <= r4) goto L_0x002e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:17:0x0024, code lost:
            closeSafely(getAndClearDataSource(r0));
            r0 = r0 - 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:18:0x002e, code lost:
            return;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void maybeSetIndexOfDataSourceWithResult(int r4, com.facebook.datasource.DataSource<T> r5, boolean r6) {
            /*
                r3 = this;
                monitor-enter(r3)
                int r0 = r3.mIndexOfDataSourceWithResult     // Catch:{ all -> 0x0031 }
                int r1 = r3.mIndexOfDataSourceWithResult     // Catch:{ all -> 0x0031 }
                com.facebook.datasource.DataSource r2 = r3.getDataSource(r4)     // Catch:{ all -> 0x0031 }
                if (r5 != r2) goto L_0x002f
                int r5 = r3.mIndexOfDataSourceWithResult     // Catch:{ all -> 0x0031 }
                if (r4 != r5) goto L_0x0010
                goto L_0x002f
            L_0x0010:
                com.facebook.datasource.DataSource r5 = r3.getDataSourceWithResult()     // Catch:{ all -> 0x0031 }
                if (r5 == 0) goto L_0x001f
                if (r6 == 0) goto L_0x001d
                int r5 = r3.mIndexOfDataSourceWithResult     // Catch:{ all -> 0x0031 }
                if (r4 >= r5) goto L_0x001d
                goto L_0x001f
            L_0x001d:
                r4 = r1
                goto L_0x0021
            L_0x001f:
                r3.mIndexOfDataSourceWithResult = r4     // Catch:{ all -> 0x0031 }
            L_0x0021:
                monitor-exit(r3)     // Catch:{ all -> 0x0031 }
            L_0x0022:
                if (r0 <= r4) goto L_0x002e
                com.facebook.datasource.DataSource r5 = r3.getAndClearDataSource(r0)
                r3.closeSafely(r5)
                int r0 = r0 + -1
                goto L_0x0022
            L_0x002e:
                return
            L_0x002f:
                monitor-exit(r3)     // Catch:{ all -> 0x0031 }
                return
            L_0x0031:
                r4 = move-exception
                monitor-exit(r3)     // Catch:{ all -> 0x0031 }
                throw r4
            */
            throw new UnsupportedOperationException("Method not decompiled: com.facebook.datasource.IncreasingQualityDataSourceSupplier.IncreasingQualityDataSource.maybeSetIndexOfDataSourceWithResult(int, com.facebook.datasource.DataSource, boolean):void");
        }

        @Nullable
        private synchronized DataSource<T> tryGetAndClearDataSource(int i, DataSource<T> dataSource) {
            if (dataSource == getDataSourceWithResult()) {
                return null;
            }
            if (dataSource != getDataSource(i)) {
                return dataSource;
            }
            return getAndClearDataSource(i);
        }

        private void closeSafely(DataSource<T> dataSource) {
            if (dataSource != null) {
                dataSource.close();
            }
        }

        private class InternalDataSubscriber implements DataSubscriber<T> {
            private int mIndex;

            public void onCancellation(DataSource<T> dataSource) {
            }

            public InternalDataSubscriber(int i) {
                this.mIndex = i;
            }

            public void onNewResult(DataSource<T> dataSource) {
                if (dataSource.hasResult()) {
                    IncreasingQualityDataSource.this.onDataSourceNewResult(this.mIndex, dataSource);
                } else if (dataSource.isFinished()) {
                    IncreasingQualityDataSource.this.onDataSourceFailed(this.mIndex, dataSource);
                }
            }

            public void onFailure(DataSource<T> dataSource) {
                IncreasingQualityDataSource.this.onDataSourceFailed(this.mIndex, dataSource);
            }

            public void onProgressUpdate(DataSource<T> dataSource) {
                if (this.mIndex == 0) {
                    IncreasingQualityDataSource.this.setProgress(dataSource.getProgress());
                }
            }
        }
    }
}
