package com.github.mgurov.boringxd

interface Xd {
    fun fulfill(delta: Int)
    fun receive(update: BoringTotals, message: String): Int

}

fun checkTotalsDoNotDecrease(lastBoringMaybe: BoringTotals?, update: BoringTotals) {
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