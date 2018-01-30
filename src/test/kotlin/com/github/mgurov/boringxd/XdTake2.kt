package com.github.mgurov.boringxd

class XdTake2 : Xd {
    private val steps = mutableListOf<Step>()

    override fun receive(update: BoringTotals, message: String): Int {
        checkTotalsDoNotDecrease(steps.lastOrNull()?.boring, update)

        val step = Step(
                boring = update,
                previous = Previous(steps.lastOrNull())
        )
        System.out.println("$step $message")
        steps.add(step)
        return step.delta
    }

    override fun fulfill(delta: Int) {
        if (delta < 0) {
            throw IllegalArgumentException("delta should be positive but got $delta")
        }
        //NB: this implementation doesn't care about the status of xd's
    }

}

// a.k.a memory - what do we need to remember from the previous update
data class Previous constructor(
        val total: Int,
        val supply: Int,
        val swallowedSupplyFluctuation: Int
) {
    constructor(previousStep: Step?) : this(
            total = previousStep?.boring?.total ?: 0,
            supply = previousStep?.boring?.supply() ?: 0,
            swallowedSupplyFluctuation = previousStep?.swallowedSupplyFluctuation ?: 0
    )
}

data class Step(
        val boring: BoringTotals,
        val previous: Previous
) {
    val coverLostStock: Int
    val coverNewRequests: Int
    val delta: Int
    val swallowedSupplyFluctuation: Int

    init {

        val currentSupply = boring.supply()


        val prevSupplyCapped = Integer.min(previous.total, previous.supply)
        val newSupplyCapped = Integer.min(previous.total, currentSupply)
        val supplyCappedDelta = newSupplyCapped - prevSupplyCapped
        val newAmortizationDelta = supplyCappedDelta
        var currentTotalAmmortization = previous.swallowedSupplyFluctuation + newAmortizationDelta


        val previouslyFulfilledLevel = Integer.min(previous.total, previous.supply)
        if (previous.supply > currentSupply) {
            if (currentSupply < previous.total) {
                val dropInSupply = previouslyFulfilledLevel - currentSupply
                val amortizedDrop = if (currentTotalAmmortization > 0) {
                    Integer.min(dropInSupply, currentTotalAmmortization)
                } else {
                    0
                }

                //stock lost and it affects previously assumed to be enough
                coverLostStock = dropInSupply - amortizedDrop
                currentTotalAmmortization = -amortizedDrop
            } else {
                coverLostStock = 0
            }
        } else {
            coverLostStock = 0
        }
        swallowedSupplyFluctuation = currentTotalAmmortization

        val newRequests = boring.total - previous.total
        if (newRequests < 0) {
            throw IllegalStateException("Unexpected negative requests $newRequests")
        }

        coverNewRequests = Math.min(newRequests, boring.shortage())

        delta = coverLostStock + coverNewRequests
    }

    override fun toString(): String {
        return "$boring, previous=$previous, coverLostStock=$coverLostStock, coverNewRequests=$coverNewRequests, delta=$delta"
    }
}
