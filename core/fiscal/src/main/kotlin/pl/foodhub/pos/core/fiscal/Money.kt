package pl.foodhub.pos.core.fiscal

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Single choke point for money -> integer-grosz conversion used by both fiscal
 * drivers, so a naive truncating cast (`(value * 100).toInt()`, which silently drops
 * the last cent on values like `12.005`) can't creep into one protocol's path and not
 * the other's.
 */
fun BigDecimal.toGrosz(): Long = setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
