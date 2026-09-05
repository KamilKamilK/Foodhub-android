package pl.foodhub.pos.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Requests a [SyncWorker] run, gated on connectivity. `KEEP` collapses repeated
 * enqueues (one per queued operation) into a single pending work request instead of
 * stacking redundant ones; WorkManager holds the request until a network is available
 * and retries with backoff on [androidx.work.ListenableWorker.Result.retry]
 * (ANDROID_POS_ARCHITECTURE.md section 9 point 3) -- no manual connectivity polling.
 */
class SyncScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun scheduleSync() {
            val request =
                OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS,
                    )
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(SYNC_WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }

        companion object {
            const val SYNC_WORK_NAME = "pos-sync"
        }
    }
