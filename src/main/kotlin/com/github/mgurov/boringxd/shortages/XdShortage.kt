package com.github.mgurov.boringxd.shortages

import com.github.mgurov.boringxd.BoringTotals
import com.github.mgurov.boringxd.Xd
import com.github.mgurov.boringxd.checkTotalsDoNotDecrease

class XdShortage : Xd {
    private val steps = mutableListOf<ShortagesStep>()

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
    val maxGeleverd: Int = 0,
    val shipped: Int = 0,
    val stock: Int = 0,
    val purchaseTotal: Int = 0
){
    companion object {
        fun of(previousStep: ShortagesStep?): Previous {
            if (null == previousStep) {
                return Previous()
            }

            return Previous(
                maxGeleverd = previousStep.maxGeleverd,
                shipped = previousStep.boring.shipped,
                stock = previousStep.boring.stock,
                purchaseTotal = previousStep.delta + previousStep.previous.purchaseTotal
            )

        }
    }
}

data class ShortagesStep(
        val boring: BoringTotals,
        val previous: Previous
) {
    val shortage = boring.rawShortage() //J
    val purchaseTotal = previous.purchaseTotal //K
    val stockIncrease = boring.stock - previous.stock //L
    val shipmentIncrease = boring.shipped - previous.shipped //M
    val formulaStockFactor = Math.max(0, stockIncrease + shipmentIncrease) //N
    val blahSum = formulaStockFactor + previous.maxGeleverd // Pprev
    val maxGeleverd = Math.min( purchaseTotal, blahSum ) //P
    val delta = shortage - (purchaseTotal - maxGeleverd) //Q
    init {
        require(shortage >= 0) { "Shortage should be strictly positive here but got ${shortage}" }
    }
}

fun makeNextStep(
    boring: BoringTotals,
    previous: Previous
) : ShortagesStep {
    return ShortagesStep(
        boring = boring,
        previous = previous
    )
}
