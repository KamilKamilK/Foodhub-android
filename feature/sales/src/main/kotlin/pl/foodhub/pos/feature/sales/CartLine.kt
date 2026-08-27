package pl.foodhub.pos.feature.sales

import pl.foodhub.pos.core.common.Money

data class CartLine(
    val productId: String,
    val productName: String,
    val unitPriceGross: Money,
    val quantity: Int,
) {
    val lineGross: Money get() = unitPriceGross * quantity
}

fun List<CartLine>.total(): Money = fold(Money.ZERO) { acc, line -> acc + line.lineGross }
