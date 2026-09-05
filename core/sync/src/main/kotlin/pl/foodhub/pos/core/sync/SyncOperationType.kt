package pl.foodhub.pos.core.sync

/** Every write the offline queue can carry. Stored as [TransactionQueue]'s opaque `type`. */
enum class SyncOperationType {
    OCCUPY_TABLE,
    RELEASE_TABLE,
    CREATE_ORDER,
    ADD_ORDER_LINE,
    CONFIRM_ORDER,
    FINALIZE_ORDER,
    ISSUE_RECEIPT,
    ISSUE_INVOICE,
}
