package com.github.mgurov.boringxd.deltasWithLostStockCap

import com.github.mgurov.boringxd.BoringTotals
import com.github.mgurov.boringxd.Xd
import com.github.mgurov.boringxd.checkTotalsDoNotDecrease

class XdTake4LostStockCapCovered : Xd {
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
        val shipped: Int = 0,
        val stock: Int = 0,
        val cancelled: Int = 0,
        val coveredByStock: Int = 0
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
                        coveredByStock = previousStep.coveredByStock
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
        val coveredByStock: Int = 0
)

fun makeNextStep(
        update: BoringTotals,
        previous: Previous
): Step {

    val newDemand = update.total - previous.total

    val newSupply = update.shipped + update.stock

    val newDemandCoveredByFulfillment = Math.max(0, update.shipped + previous.cancelled - previous.total)

    val newDemandCoveredBySupplyAndCancel = Math.max(0, newSupply + previous.cancelled - previous.total)

    val newDemandCoveredyByStock = newDemandCoveredBySupplyAndCancel - newDemandCoveredByFulfillment

    val purchaseForNewDemand = newDemand - newDemandCoveredBySupplyAndCancel

    val purchaseForStockLost = Math.min(previous.coveredByStock, Math.max(0, previous.stock + previous.shipped - newSupply))

    val newCancellations = update.cancelled - previous.cancelled

    val delta = purchaseForNewDemand + purchaseForStockLost - newCancellations

    //should it be deducted from new or old? Me don't know.
    val increaseShipmentAndCancellations = update.cancelled + update.shipped - previous.cancelled - previous.shipped

    val oldStockDemandAdjusted = Math.max(0, previous.coveredByStock - increaseShipmentAndCancellations - purchaseForStockLost)

    val newCoveredByStock = Math.min(newDemandCoveredyByStock + oldStockDemandAdjusted, update.stock)


    return Step(
            boring = update,
            previous = previous,
            coverNewRequests = purchaseForNewDemand,
            coverLostStock = purchaseForStockLost,
            cancellationDelta = newCancellations,
            delta = delta,
            coveredByStock = newCoveredByStock
    )
}