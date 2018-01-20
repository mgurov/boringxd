package com.github.mgurov.boringxd

class XdTake2 : Xd {
    private val steps = mutableListOf<Step>()
    private var fulfilled = 0

    override fun receive(update: BoringTotals, message: String): Int {
        checkNoTotalsDecrease(update)

        val step = Step(
                boring = update,
                fulfilled = fulfilled,
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
        fulfilled += delta
    }


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
            val fulfilled: Int,
            val previous: Previous
    ) {
        val coverLostStock: Int
        val coverNewRequests: Int
        val delta: Int

        init {
            coverLostStock = Math.max(previous.supply - boring.supply(), 0)

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


    private fun checkNoTotalsDecrease(update: BoringTotals) {
        checkTotalsDoNotDecrease(steps.lastOrNull()?.boring, update)
    }

}