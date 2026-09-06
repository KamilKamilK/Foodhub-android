package pl.foodhub.pos.core.database

import javax.inject.Inject

/**
 * Local record of what a terminal has already fiscalized -- see
 * [FiscalizationRecordEntity] for why this must survive an app kill/restart, not
 * just live in memory for the duration of one checkout.
 */
interface FiscalizationLedger {
    suspend fun findByDocumentId(documentId: String): FiscalizationRecordEntity?

    suspend fun record(record: FiscalizationRecordEntity)
}

class RoomFiscalizationLedger
    @Inject
    constructor(private val dao: FiscalizationRecordDao) : FiscalizationLedger {
        override suspend fun findByDocumentId(documentId: String): FiscalizationRecordEntity? =
            dao.findByDocumentId(documentId)

        override suspend fun record(record: FiscalizationRecordEntity) = dao.insert(record)
    }
