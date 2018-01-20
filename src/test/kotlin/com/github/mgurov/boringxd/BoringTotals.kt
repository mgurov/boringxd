package com.github.mgurov.boringxd

data class BoringTotals(
        val stock: Int = 0,
        val total: Int = 0,
        val shipped: Int = 0,
        val cancelled: Int = 0
) {
    fun shortage(): Int {
        return Integer.max(total - supply(), 0)
    }

    override fun toString(): String {
        return "boring(total=$total, stock=$stock, shipped=$shipped, cancelled=$cancelled)"
    }

    fun supply(): Int {
        return shipped + cancelled + stock
    }

}