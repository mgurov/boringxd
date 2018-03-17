package com.github.mgurov.boringxd

interface Xd {
    fun receive(update: BoringTotals, message: String): Int
}

fun checkTotalsDoNotDecrease(lastBoringMaybe: BoringTotals?, update: BoringTotals) {

    require(update.total >= update.stock + update.shipped + update.cancelled) {
        "supply cannot be higher than demand"
    }

    val lastStep = lastBoringMaybe ?: BoringTotals()
    checkNoDecrease("total", lastStep.total, update.total)
    checkNoDecrease("shipped", lastStep.shipped, update.shipped)
    checkNoDecrease("cancelled", lastStep.cancelled, update.cancelled)
}

fun checkNoDecrease(field: String, from: Int, to: Int) {
    if (from > to) {
        throw IllegalArgumentException("Decreasing $field $from -> $to")
    }
}