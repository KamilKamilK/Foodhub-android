package pl.foodhub.pos.core.printing

import kotlinx.serialization.Serializable

/**
 * A checkout line as needed for printing -- reused directly by core:sync as the
 * queue's print-operation payload shape (core:sync -> core:printing), so there is no
 * separate wire DTO to keep in sync with this one.
 */
@Serializable
data class PrintableLine(
    val productName: String,
    val quantity: Int,
    val orderDirectionId: Long?,
    val unitPriceAmount: Long,
)
