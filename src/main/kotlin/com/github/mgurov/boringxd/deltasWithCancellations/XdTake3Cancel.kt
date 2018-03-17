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
            previous = Previous(steps.lastOrNull())
        )
        System.out.println("$step $message")
        steps.add(step)
        return step.delta
    }

}

// a.k.a memory - what do we need to remember from the previous update
data class Previous constructor(
        val total: Int,
        val supply: Int
) {
    constructor(previousStep: Step?) : this(
            total = previousStep?.boring?.total ?: 0,
            supply = previousStep?.boring?.supply() ?: 0
    )
}

data class Step(
    val boring: BoringTotals,
    val previous: Previous,
    val coverLostStock: Int,
    val coverNewRequests: Int,
    val delta: Int
) {
    override fun toString(): String {
        return "$boring, previous=$previous, coverLostStock=$coverLostStock, coverNewRequests=$coverNewRequests, delta=$delta"
    }
}

fun makeNextStep(
    boring: BoringTotals,
    previous: Previous
): Step {
    val coverLostStock: Int
    val coverNewRequests: Int
    val delta: Int

    val currentSupply = boring.supply()

    if (previous.supply > currentSupply && currentSupply < previous.total) {
        //stock lost and it affects previously assumed to be enough
        coverLostStock = Integer.min(previous.total, previous.supply) - currentSupply
    } else {
        coverLostStock = 0
    }

    val newRequests = boring.total - previous.total
    if (newRequests < 0) {
        throw IllegalStateException("Unexpected negative requests $newRequests")
    }

    coverNewRequests = Math.min(newRequests, boring.shortage())

    delta = coverLostStock + coverNewRequests

    return Step(
        boring = boring,
        previous = previous,
        coverLostStock = coverLostStock,
        coverNewRequests = coverNewRequests,
        delta = delta
    )
}