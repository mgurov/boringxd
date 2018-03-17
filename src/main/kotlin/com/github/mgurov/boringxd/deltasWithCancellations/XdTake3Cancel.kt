package com.github.mgurov.boringxd.deltasWithCancellations

import com.github.mgurov.boringxd.BoringTotals
import com.github.mgurov.boringxd.Xd
import com.github.mgurov.boringxd.checkTotalsDoNotDecrease

class XdTake3Cancel : Xd {
    private val steps = mutableListOf<Step>()

    override fun receive(update: BoringTotals, message: String): Int {
        checkTotalsDoNotDecrease(steps.lastOrNull()?.boring, update)

        val step = makeNextStep(
                boring = update,
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
        boring: BoringTotals,
        previous: Previous
): Step {

    val newSupply = boring.shipped + boring.stock

    val totalDemandDelta = boring.total - previous.total
    val coverNewRequests = if (totalDemandDelta > 0) {
        val supplyCoveringNewDemand = Math.max(0, newSupply + boring.cancelled - previous.total)
        totalDemandDelta - supplyCoveringNewDemand
    } else {
        0
    }

    val coverLostStock = Math.max(0, previous.supply - newSupply)

    val cancellationDelta = boring.cancelled - previous.cancelled

    val delta = coverNewRequests + coverLostStock - cancellationDelta

    return Step(
            boring = boring,
            previous = previous,
            coverNewRequests = coverNewRequests,
            coverLostStock = coverLostStock,
            cancellationDelta = cancellationDelta,
            delta = delta
    )
}