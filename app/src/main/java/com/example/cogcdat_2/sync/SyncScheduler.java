package com.example.cogcdat_2.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

public class SyncScheduler {
    private static final String TAG = "SyncScheduler";
    private static final int SYNC_INTERVAL_MS = 5 * 60 * 1000; // 5 минут
    private static final int DELAY_ON_START_MS = 2000; // 2 секунды после запуска

    private static SyncScheduler instance;
    private final SyncManager syncManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private Runnable syncRunnable;

    private SyncScheduler(SyncManager syncManager) {
        this.syncManager = syncManager;
    }

    public static synchronized SyncScheduler getInstance(SyncManager syncManager) {
        if (instance == null) {
            instance = new SyncScheduler(syncManager);
        }
        return instance;
    }

    /**
     * Запуск периодической синхронизации
     */
    public void startPeriodicSync() {
        if (isRunning.get()) {
            Log.d(TAG, "Periodic sync already running");
            return;
        }

        isRunning.set(true);

        syncRunnable = new Runnable() {
            @Override
            public void run() {
                if (syncManager.getSavedToken() != null) {
                    Log.d(TAG, "Performing scheduled sync");
                    syncManager.syncAll();
                }

                if (isRunning.get()) {
                    handler.postDelayed(this, SYNC_INTERVAL_MS);
                }
            }
        };

        // Первая синхронизация через DELAY_ON_START_MS
        handler.postDelayed(syncRunnable, DELAY_ON_START_MS);
        Log.d(TAG, "Periodic sync started, interval: " + SYNC_INTERVAL_MS + "ms");
    }

    /**
     * Остановка периодической синхронизации
     */
    public void stopPeriodicSync() {
        isRunning.set(false);
        if (syncRunnable != null) {
            handler.removeCallbacks(syncRunnable);
        }
        Log.d(TAG, "Periodic sync stopped");
    }

    /**
     * Синхронизация при выходе из приложения
     */
    public void syncOnAppExit() {
        if (syncManager.getSavedToken() != null) {
            Log.d(TAG, "Performing final sync before exit");
            syncManager.syncAllSync();
        }
    }
}