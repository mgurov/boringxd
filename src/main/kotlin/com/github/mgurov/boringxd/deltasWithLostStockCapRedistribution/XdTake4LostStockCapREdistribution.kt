package com.github.mgurov.boringxd.deltasWithLostStockCapRedistribution

import com.github.mgurov.boringxd.BoringTotals
import com.github.mgurov.boringxd.Xd
import com.github.mgurov.boringxd.checkTotalsDoNotDecrease
import kotlin.reflect.KMutableProperty1

class XdTake4LostStockCapREdistribution : Xd {
    val steps = mutableListOf<Step>()

    override fun receive(update: BoringTotals, message: String): Int {
        checkTotalsDoNotDecrease(steps.lastOrNull()?.boring, update)

        val step = makeNextStep(
                update = update,
                previous = Previous.of(steps.lastOrNull())
        )
        System.out.println("$step $message")
        steps.add(step)
        return step.delta
    }

}

// a.k.a memory - what do we need to remember from the previous update
data class Previous constructor(
        val total: Int = 0,
        val stock: Int = 0,
        val shipped: Int = 0,
        val cancelled: Int = 0,
        val purchased: Int = 0,
        val stockExcess: Int = 0
) {
    companion object {
        fun of(previousStep: Step?): Previous {
            if (previousStep == null) {
                return Previous()
            }
            return with(previousStep.boring) {
                Previous(
                        total = total,
                        shipped = shipped,
                        stock = stock,
                        cancelled = cancelled,
                        purchased = previousStep.totalPurchased,
                        stockExcess = previousStep.stockExcess
                )
            }
        }
    }
}

data class Step(
        val boring: BoringTotals,
        val previous: Previous,
        val coverNewRequests: Int,
        val coverLostStock: Int,
        val cancellationDelta: Int,
        val delta: Int,
        val totalPurchased: Int,
        val stockExcess: Int
)

fun makeNextStep(
        update: BoringTotals,
        previous: Previous
): Step {

    val redistribution = OldNewSupplyDistribution(previous.total, update.total)

    redistribution.addShipped(update.shipped)
    redistribution.addCancelled(update.cancelled)
    redistribution.addStock(update.stock)

    val previousSupplyChange = redistribution.old.totalSupply() - previous.stock - previous.cancelled - previous.shipped

    val stockLost : Int
    val newStockExcess : Int
    if (previousSupplyChange < 0) {
        val p = -previousSupplyChange deductMin previous.stockExcess
        stockLost = p.first
        newStockExcess = p.second
    } else {
        stockLost = 0
        val coveringStock = 0 max redistribution.old.stock - previous.stockExcess
        newStockExcess = coveringStock min previousSupplyChange
    }

    val oldCancellationIncrease = redistribution.old.cancelled - previous.cancelled

    val cancelForPrevious = previous.purchased min oldCancellationIncrease

    val uncoveredNewDemand = redistribution.new.uncoveredDemand()

    val delta = uncoveredNewDemand + stockLost - cancelForPrevious


    return Step(
        boring = update,
        previous = previous,
        coverNewRequests = uncoveredNewDemand,
        coverLostStock = stockLost,
        cancellationDelta = cancelForPrevious,
        delta = delta,
        stockExcess = newStockExcess,
        totalPurchased = previous.purchased + delta
    )
}

private infix fun Int.deductMin(other: Int): Pair<Int, Int> {
    require(this >= 0)
    require(other >= 0)
    val deduction = this min other
    return this - deduction to other - deduction
}

data class OldNewSupplyDistribution(
    val old:SupplyDistribution,
    val new:SupplyDistribution
) {
    constructor(
        oldTotal: Int,
        newTotal: Int
    ): this(
        old = SupplyDistribution(oldTotal),
        new = SupplyDistribution(newTotal - oldTotal)
    )

    fun addStock(value: Int) {
        check(0 == new.addStock(old.addStock(value)))
    }

    fun addShipped(value: Int) {
        check(0 == new.addShipped(old.addShipped(value)))
    }

    fun addCancelled(value: Int) {
        check(0 == new.addCancelled(old.addCancelled(value)))
    }

}

data class SupplyDistribution(
    val total: Int,
    var stock: Int = 0,
    var shipped: Int = 0,
    var cancelled: Int = 0
) {
    fun totalSupply() = stock + shipped + cancelled

    fun uncoveredDemand() = total - totalSupply()

    fun capValue(value: Int, target: KMutableProperty1<SupplyDistribution, Int>): Int {
        val change = uncoveredDemand() min value
        target.set(this, target.get(this) + change)
        return value - change
    }

    fun addStock(value: Int): Int {
        return capValue(value, SupplyDistribution::stock)
    }

    fun addShipped(value: Int): Int {
        return capValue(value, SupplyDistribution::shipped)
    }

    fun addCancelled(value: Int): Int {
        return capValue(value, SupplyDistribution::cancelled)
    }
}

private infix fun Int.max(i: Int): Int {
    return Math.max(this, i)
}

private infix fun Int.min(i: Int): Int {
    return Math.min(this, i)
}
