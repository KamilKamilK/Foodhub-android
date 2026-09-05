package pl.foodhub.pos.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Thin WorkManager adapter -- all real logic lives in [SyncProcessor], which is a
 * plain class so it can be unit-tested directly without `androidx.work:work-testing`.
 */
@HiltWorker
class SyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val processor: SyncProcessor,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result =
            when (processor.run()) {
                SyncRunResult.Drained -> Result.success()
                SyncRunResult.RetryLater -> Result.retry()
            }
    }
