package com.github.mgurov.boringxd.shortages

import com.github.mgurov.boringxd.BoringTotals
import com.github.mgurov.boringxd.Xd
import com.github.mgurov.boringxd.checkTotalsDoNotDecrease

class XdShortage : Xd {
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

}

// a.k.a memory - what do we need to remember from the previous update
data class Previous constructor(
        val shipped: Int,
        val stock: Int,
        val totalCrossdocked: Int,
        val absent: Boolean //TODO: means we should indeed reset.
){
    constructor(previousStep: Step?) : this(
            shipped = previousStep?.boring?.shipped ?: 0,
            stock = previousStep?.boring?.stock ?: 0,
            totalCrossdocked = (previousStep?.delta?: 0) + (previousStep?.previous?.totalCrossdocked?: 0),
            absent = previousStep == null
    )
}

//data class Shortage(val shortageAdvice: Int) {
//    constructor(totals: BoringTotals): this(
//            shortageAdvice = totals.shortage()
//    )
//}

data class Step(
        val boring: BoringTotals,
//        val shortage: Shortage,
        val previous: Previous
) {
    val delta: Int

    init {

        val shortage = boring.rawShortage()
        require(shortage >= 0) {"Shortage should be strictly positive here but got ${shortage}"}

        if (previous.absent || shortage == 0) {
            delta = shortage
        } else {
            val deltaShipped = boring.shipped - previous.shipped;
            val deltaStock = boring.stock - previous.stock;


            //TODO: increase previous.totalCrossDock by delta

            val deltaSupply = Integer.max(0, deltaShipped + deltaStock)

            delta =  shortage - previous.totalCrossdocked + deltaSupply
        }

    }

    override fun toString(): String {
        return "$boring, previous=$previous, delta=$delta"
    }
}