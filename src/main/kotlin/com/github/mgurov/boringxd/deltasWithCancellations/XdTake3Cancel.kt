package com.github.mgurov.boringxd.deltasWithCancellations

import com.github.mgurov.boringxd.BoringTotals
import com.github.mgurov.boringxd.Xd
import com.github.mgurov.boringxd.checkTotalsDoNotDecrease

class XdTake3Cancel : Xd {
    private val steps = mutableListOf<Step>()

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
        val supply: Int = 0,
        val cancelled: Int = 0
) {
    companion object {
        fun of(previousStep: Step?): Previous {
            if (previousStep == null) {
                return Previous()
            }
            return with(previousStep.boring) {
                Previous(
                        total = total,
                        supply = shipped + stock,
                        cancelled = cancelled
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
        val delta: Int
)

fun makeNextStep(
        update: BoringTotals,
        previous: Previous
): Step {

    val newDemand = update.total - previous.total

    val newSupply = update.shipped + update.stock

    val newDemandCoveredBySupplyAndCancel = Math.max(0, newSupply + previous.cancelled - previous.total)

    val purchaseForNewDemand = newDemand - newDemandCoveredBySupplyAndCancel

    val purchaseForStockLost = Math.max(0, previous.supply - newSupply)

    val newCancellations = update.cancelled - previous.cancelled

    val delta = purchaseForNewDemand + purchaseForStockLost - newCancellations

    return Step(
            boring = update,
            previous = previous,
            coverNewRequests = purchaseForNewDemand,
            coverLostStock = purchaseForStockLost,
            cancellationDelta = newCancellations,
            delta = delta
    )
}