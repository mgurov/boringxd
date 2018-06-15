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

    redistribution.addCancelled(update.cancelled)
    redistribution.addShipped(update.shipped)
    redistribution.addStock(update.stock)

    val uncoveredNewDemand = redistribution.new.uncoveredDemand()

    val previousSupplyChange = redistribution.old.totalSupply() - previous.stock - previous.cancelled - previous.shipped

    var (stockExcess, purchaseForStockDecrease) = if (previousSupplyChange > 0) {
        Pair(previousSupplyChange, 0)
    } else {
        Pair(0, -previousSupplyChange)
    }


    val oldStockChange = redistribution.old.stock - previous.stock
    var oldCancellationIncrease = redistribution.old.cancelled - previous.cancelled

    if (oldStockChange < 0 && oldCancellationIncrease > 0) {
        val cancellationToStockAmmortization = -oldStockChange min oldCancellationIncrease
        oldCancellationIncrease -= cancellationToStockAmmortization
    }

    if (stockExcess > 0 && oldCancellationIncrease > 0) {
        val cancellationToSupplyAmmortization = stockExcess min oldCancellationIncrease
        //oldCancellationIncrease -= cancellationToSupplyAmmortization
        stockExcess -= cancellationToSupplyAmmortization
    }

    stockExcess += previous.stockExcess


    val cancelForPrevious = previous.purchased min oldCancellationIncrease

    val lostStockAmortization = purchaseForStockDecrease min stockExcess

    stockExcess -= lostStockAmortization
    purchaseForStockDecrease -= lostStockAmortization



    val delta = uncoveredNewDemand + purchaseForStockDecrease - cancelForPrevious


    return Step(
        boring = update,
        previous = previous,
        coverNewRequests = uncoveredNewDemand,
        coverLostStock = purchaseForStockDecrease,
        cancellationDelta = cancelForPrevious,
        delta = delta,
        stockExcess = stockExcess,
        totalPurchased = previous.purchased + delta
    )
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
