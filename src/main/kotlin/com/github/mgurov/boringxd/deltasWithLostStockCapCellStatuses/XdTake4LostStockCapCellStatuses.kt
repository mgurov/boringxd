package com.github.mgurov.boringxd.deltasWithLostStockCapCellStatuses

import com.github.mgurov.boringxd.BoringTotals
import com.github.mgurov.boringxd.Xd
import com.github.mgurov.boringxd.checkTotalsDoNotDecrease

class XdTake4LostStockCapCellStatuses : Xd {
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

    val oldState = OldStateBuckets(
        shipped = TargetBucket(maxValue = previous.shipped),
        cancelled = TargetBucket(maxValue = previous.cancelled),
        stock = TargetBucket(maxValue = previous.stock - previous.stockExcess),
        purchased = TargetBucket(maxValue = previous.purchased)
    )

    check(oldState.open() == previous.total)

    val customerOrdersIncrease = update.total - previous.total

    val newShortage = update.shortage() min customerOrdersIncrease //it's sacred, should always set to the positive delta

    val newState = NewStateBuckets(
        shipped = SourceBucket(currentValue = update.shipped),
        cancelled = SourceBucket(currentValue = update.cancelled),
        stock = SourceBucket(currentValue = update.stock),
        shortage = SourceBucket(currentValue = update.shortage() - newShortage)
    )

    //state as is
    newState.shipped `→` oldState.shipped
    check(oldState.shipped.open() == 0)
    newState.cancelled `→` oldState.cancelled
    check(oldState.cancelled.open() == 0)

    newState.shipped `→` oldState.stock //stuff might've finally be shipped

    newState.stock `→` oldState.stock
    newState.shortage `→` oldState.purchased

    //redistribution
    newState.shipped `→` oldState.stock //shipped previously on stock
    val shipmentOfThePreviousPurchases = newState.shipped `→` oldState.purchased //shipped previously purchased

    newState.cancelled `→` oldState.stock //annihilation what was previously thought to be on stock isn't needed anymore
    val cancelledPreviousPurchase = newState.cancelled `→` oldState.purchased

    val stockExcess = newState.stock `→` oldState.purchased
    val stockLost = newState.shortage `→` oldState.stock

    //the remnants are the new demand
    check(oldState.open() == 0) {
        "old ${oldState} not fully consumed by new ${newState}"
    }

    check(customerOrdersIncrease == newState.unconsumed() + newShortage) {
        "unconsumed new is not what has changed ${customerOrdersIncrease} <> ${newState}"
    }

    val delta = newShortage + stockLost - cancelledPreviousPurchase // 🥁

    val newOpenPurchased = previous.purchased + delta - shipmentOfThePreviousPurchases

    return Step(
        boring = update,
        previous = previous,
        coverNewRequests = newShortage,
        coverLostStock = stockLost,
        cancellationDelta = cancelledPreviousPurchase,
        delta = delta,
        stockExcess = stockExcess,
        totalPurchased = newOpenPurchased
    )
}


data class OldStateBuckets(
    val shipped: TargetBucket,
    val cancelled: TargetBucket,
    val stock: TargetBucket,
    val purchased: TargetBucket
) {
    fun open(): Int = shipped.open() + cancelled.open() + stock.open() + purchased.open()
}

data class NewStateBuckets(
    val shipped: SourceBucket,
    val cancelled: SourceBucket,
    val stock: SourceBucket,
    val shortage: SourceBucket
) {
    fun unconsumed() = shipped.currentValue + cancelled.currentValue + stock.currentValue + shortage.currentValue
}

data class SourceBucket(
    var currentValue: Int = 0
) {
    fun pipeTo(target: TargetBucket): Int {
        val transferQuantity = target.open() min currentValue
        target.currentValue += transferQuantity
        this.currentValue -= transferQuantity
        return transferQuantity
    }

    infix fun `→`(target: TargetBucket) = pipeTo(target)

    override fun toString(): String {
        return "$currentValue"
    }


}

data class TargetBucket(
    var currentValue: Int = 0,
    var maxValue: Int
) {
    fun open(): Int {
        return maxValue - currentValue
    }

    override fun toString(): String {
        return "$currentValue" + if (open() > 0) "($maxValue)" else ""
    }


}

private infix fun Int.min(i: Int): Int {
    return Math.min(this, i)
}
