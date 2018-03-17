package com.github.mgurov.boringxd

data class BoringTotals(
        val total: Int = 0,
        val shipped: Int = 0,
        val cancelled: Int = 0,
        val stock: Int = 0,
        val stockExcess: Int = 0
) {
    fun shortage(): Int {
        return Integer.max(rawShortage(), 0)
    }

    fun rawShortage(): Int {
        return total - virtualSupply()
    }

    override fun toString(): String {
        return "boring(total=$total, stock=$stock, shipped=$shipped, cancelled=$cancelled)"
    }

    fun virtualSupply(): Int {
        return shipped + cancelled + stock
    }

}