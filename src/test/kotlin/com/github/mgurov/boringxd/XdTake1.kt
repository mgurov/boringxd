package com.github.mgurov.boringxd

class XdTake1 : Xd {
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
            val shortage: Int = 0,
            val fulfilled: Int = 0,
            val ignoredDecrease: Int = 0
    ) {
        constructor(previousStep: Step?): this(
                previousStep?.boring?.shortage()?:0,
                previousStep?.fulfilled?:0,
                previousStep?.ignoredDecrease?:0
        )
    }

    data class Step(
            val boring: BoringTotals,
            val fulfilled: Int,
            val previous: Previous
    ) {
        val shortage = boring.shortage()
        val shortageDelta = shortage - previous.shortage
        val fulfilledDelta = fulfilled - previous.fulfilled

        val delta: Int
        val ignoredDecrease: Int

        init {
            val tempDelta = shortageDelta + fulfilledDelta
            if (tempDelta > 0) {
                val consumedByPreviousDelta = Integer.min(tempDelta, previous.ignoredDecrease)
                delta = tempDelta - consumedByPreviousDelta
                ignoredDecrease = previous.ignoredDecrease - consumedByPreviousDelta
            } else {
                delta = 0
                ignoredDecrease = previous.ignoredDecrease - tempDelta
            }
        }

        override fun toString(): String {
            return "$boring, shortage=$shortage, shortageDelta=$shortageDelta, fulfilled=$fulfilled, fulfilledDelta=$fulfilledDelta -> $delta (-$ignoredDecrease))"
        }
    }


    private fun checkNoTotalsDecrease(update: BoringTotals) {
        checkTotalsDoNotDecrease(steps.lastOrNull()?.boring, update)
    }

}