package com.github.mgurov.boringxd.deltasWithLostStockCapExcess

import com.github.mgurov.boringxd.BoringTotals
import com.github.mgurov.boringxd.Xd
import com.github.mgurov.boringxd.checkTotalsDoNotDecrease

class XdTake4LostStockCapExcess : Xd {
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

    val newDemand = update.total - previous.total

    val newSupply = update.shipped + update.stock

    val newDemandCoveredBySupplyAndCancel = Math.max(0, newSupply + previous.cancelled - previous.total)

    val purchaseForNewDemand = newDemand - newDemandCoveredBySupplyAndCancel

    val newSupplyOverOldDemandDistribution = SupplyDistribution(previous.total)
        .addCancelled(update.cancelled)
        .addShipped(update.shipped)
        .addStock(update.stock)

    val excessDelta = ( //newSupplyOverOldDemandDistribution.cancelled - previous.cancelled
            + newSupplyOverOldDemandDistribution.shipped - previous.shipped
            + newSupplyOverOldDemandDistribution.stock - previous.stock)

    val stockLostFound = excessDelta + previous.stockExcess

    val (purchaseForStockLost, stockExcess) = if (stockLostFound > 0) {
        Pair(0, stockLostFound)
    } else {
        Pair(-stockLostFound, 0)
    }

    val newCancellations = (update.cancelled - previous.cancelled) min (previous.purchased)

    val delta = purchaseForNewDemand + purchaseForStockLost - newCancellations

    return Step(
        boring = update,
        previous = previous,
        coverNewRequests = purchaseForNewDemand,
        coverLostStock = purchaseForStockLost,
        cancellationDelta = newCancellations,
        delta = delta,
        stockExcess = stockExcess,
        totalPurchased = previous.purchased + delta
    )
}

data class SupplyDistribution(
    val total: Int = 0,
    val stock: Int = 0,
    val shipped: Int = 0,
    val cancelled: Int = 0
) {
    fun totalSupply() = stock + shipped + cancelled

    fun capValue(value: Int): Int {
        return (total - totalSupply()) min value
    }

    fun addStock(value: Int): SupplyDistribution {
        return copy(stock = stock + capValue(value))
    }

    fun addShipped(value: Int): SupplyDistribution {
        return copy(shipped = shipped + capValue(value))
    }

    fun addCancelled(value: Int): SupplyDistribution {
        return copy(cancelled = cancelled + capValue(value))
    }
}

private infix fun Int.max(i: Int): Int {
    return Math.max(this, i)
}

private infix fun Int.min(i: Int): Int {
    return Math.min(this, i)
}
