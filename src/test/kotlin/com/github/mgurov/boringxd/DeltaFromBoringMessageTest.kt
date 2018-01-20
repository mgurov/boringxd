package com.github.mgurov.boringxd

import org.junit.Test

import org.assertj.core.api.Assertions.*
import java.lang.Integer.max

class DeltaFromBoringMessageTest {

    @Test
    fun `normal flow`() {

        next(BoringTotals(total = 1), "shop order + 1")
        then(expectedDelta = 1)

        next(BoringTotals(total = 2), "shop order + 1")
        then(expectedDelta = 1)
    }

    @Test
    fun `existing stock 2`() {

        next(BoringTotals(total = 1, stock = 2), "shop order + 1")
        then(expectedDelta = 0)

        next(BoringTotals(total = 2, stock = 2), "shop order + 1")
        then(expectedDelta = 0)

        next(BoringTotals(total = 3, stock = 2), "shop order + 1")
        then(expectedDelta = 1)
    }

    @Test
    fun `shipment via our purchase`() {

        next(BoringTotals(total = 1), "shop order + 1")
        then(expectedDelta = 1)

        next(BoringTotals(total = 1, stock = 1), "stock via fulfillment")
        fulfill(1)
        then(expectedDelta = 0)

        next(BoringTotals(total = 1, stock = 0, shipped = 1), "shipped")
        then(expectedDelta = 0)
    }

    @Test
    fun `stock fluctuation down`() {

        next(BoringTotals(total = 1, stock = 1), "shop order + 1")
        then(expectedDelta = 0)

        next(BoringTotals(total = 2, stock = 0), "stock lost + 1 shop order")
        then(expectedDelta = 2)
    }

    @Test
    fun `stock fluctuation down breakdown`() {

        next(BoringTotals(total = 1, stock = 1), "shop order + 1")
        then(expectedDelta = 0)

        next(BoringTotals(total = 1, stock = 0), "stock lost")
        then(expectedDelta = 1)

        next(BoringTotals(total = 2, stock = 0), "+ 1 shop order")
        then(expectedDelta = 1)
    }

    @Test
    fun `stock fluctuation up`() {
        next(BoringTotals(total = 1, stock = 0), "shop order + 1")
        then(expectedDelta = 1)

        next(BoringTotals(total = 2, stock = 1), "stock found! + 1 shop order")
        then(expectedDelta = 0)
    }

    @Test
    fun `stock fluctuation up breakdown`() {
        next(BoringTotals(total = 1, stock = 0), "shop order + 1")
        then(expectedDelta = 1)

        next(BoringTotals(total = 1, stock = 1), "stock found!")
        then(expectedDelta = 0)

        next(BoringTotals(total = 2, stock = 1), "+ 1 shop order")
        then(expectedDelta = 0)
    }

    @Test
    fun `timing`() {

        next(BoringTotals(total = 1, stock = 0), "shop order + 1")
        then(expectedDelta = 1)

        next(BoringTotals(total = 1, stock = 1), "received unexpected")
        then(expectedDelta = 0)

        next(BoringTotals(total = 1, stock = 1), "finally received update")
        fulfill(1)
        then(expectedDelta = 0)

        next(BoringTotals(total = 2, stock = 1), "+ 1 shop order")
        then(expectedDelta = 1)
    }

    @Test
    fun `fluctuation down remembered and taken into account later`() {

        next(BoringTotals(total = 2, stock = 0), "shop order + 1 no stock yet")
        then(expectedDelta = 2)

        next(BoringTotals(total = 2, stock = 1), "stock went up")
        then(expectedDelta = 0)

        next(BoringTotals(total = 4, stock = 1), "shop order +2")
        then(expectedDelta = 1)
    }

    // XLS

    @Test
    fun `Create create ship`() {

        next(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        next(BoringTotals(total = 10, stock = 6), "customer order +3 stock +4 via our fulfillment")
        fulfill(4)
        then(expectedDelta = 3)

        next(BoringTotals(total = 10, stock = 2, shipped = 5), "Shipment")
        fulfill(1)
        then(expectedDelta = 0)

        next(BoringTotals(total = 10, stock = 0, shipped = 10), "final shipment")
        fulfill(3)
        then(expectedDelta = 0)
    }

    @Test
    fun `Create cancel ship`() {

        next(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        next(BoringTotals(total = 7, stock = 2, cancelled = 3), "Cancel 3 of customer order")
        then(expectedDelta = 0) //TODO: -3 w/cancellation

        next(BoringTotals(total = 7, stock = 0, cancelled = 3, shipped = 4), "Shipment")
        fulfill(2)
        then(expectedDelta = 0)
    }

    @Test
    fun `Create found_stock create ship`() {

        next(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        next(BoringTotals(total = 10, stock = 6), "+3 customer order")
        fulfill(1)
        then(expectedDelta = 0)

        next(BoringTotals(total = 10, stock = 0, shipped = 10), "Shipment")
        fulfill(4)
        then(expectedDelta = 0)
    }

    @Test
    fun `Create shipment timing_issue`() {

        next(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        next(BoringTotals(total = 7, stock = 2, shipped = 2), "shipment bypassing stock")
        fulfill(2)
        then(expectedDelta = 0)

        next(BoringTotals(total = 10, stock = 0, shipped = 4), "customer order + 3 and stock shipped")
        then(expectedDelta = 3)

        next(BoringTotals(total = 10, stock = 0, shipped = 10), "final Shipment")
        fulfill(6)
        then(expectedDelta = 0)
    }

    @Test
    fun `Received goods not registered yet`() {

        next(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        next(BoringTotals(total = 10, stock = 6), "New customer order with the stock +4 unrelated to xD")
        then(expectedDelta = 0)

        fulfill(4)
        next(BoringTotals(total = 10, stock = 1, shipped = 5), "shipment + notided we've delivered that thing")
        then(expectedDelta = 0)

        fulfill(4)
        next(BoringTotals(total = 10, stock = 0, shipped = 10), "final Shipment")
        then(expectedDelta = 0)
    }


    @Test
    fun `stop pops boring first delayed noticed xD`() {

        next(BoringTotals(total = 1, stock = 0), "customer order +1 -> cdpa")
        then(expectedDelta = 1)

        next(BoringTotals(total = 1, stock = 1), "our stuff has been delivered but we aren't yet aware of that")
        then(expectedDelta = 0)

        next(BoringTotals(total = 1, shipped = 1), "shipped")
        then(expectedDelta = 0)

        fulfill(1)
        next(BoringTotals(total = 1, shipped = 1), "now we've noticed")
        then(expectedDelta = 0)

        next(BoringTotals(total = 2, shipped = 1), "order +1")
        then(expectedDelta = 1)
    }

    @Test
    fun `stop pops boring first delayed noticed xD 2`() {

        next(BoringTotals(total = 1, stock = 0), "customer order +1 -> cdpa")
        then(expectedDelta = 1)

        next(BoringTotals(total = 2, stock = 1), "our stuff has been delivered but we aren't yet aware of that, should order just in case")
        then(expectedDelta = 1)

//        next(BoringTotals(total = 1, shipped = 1), "shipped")
//        then(expectedDelta = 0)

        fulfill(1)
        next(BoringTotals(total = 2, shipped = 1), "now we've noticed")
        then(expectedDelta = 0)

//        next(BoringTotals(total = 2, shipped = 1), "order +1")
//        then(expectedDelta = 1)
    }


    @Test
    fun `should not think overshipped when notified bit late at xD`() {

        next(BoringTotals(total = 1, stock = 0), "customer order +1 -> cdpa")
        then(expectedDelta = 1)

        next(BoringTotals(total = 2, stock = 1), "our first piece has been delivered but we aren't yet aware of that")
        then(expectedDelta = 0)

//        next(BoringTotals(total = 1, shipped = 1), "shipped")
//        then(expectedDelta = 0)

        fulfill(1)
        next(BoringTotals(total = 2, shipped = 1), "now we've noticed")
        then(expectedDelta = 0)

//        next(BoringTotals(total = 2, shipped = 1), "order +1")
//        then(expectedDelta = 1)
    }

    @Test
    fun `Stock decreased`() {

        next(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        next(BoringTotals(total = 10, stock = 0), "customer order +3 stock fluctuation -2")
        then(expectedDelta = 5)

        fulfill(10)
        next(BoringTotals(total = 10, stock = 0, shipped = 10), "final shipment")
        then(expectedDelta = 0)
    }

    class Xd {
        val steps = mutableListOf<Step>()
        var fulfilled = 0

        fun receive(update: BoringTotals, message: String = ""): Int {
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
            val lastStep = if (steps.isEmpty()) {BoringTotals()} else {steps.last().boring}
            checkNoDecrease("total", lastStep.total, update.total)
            checkNoDecrease("shipped", lastStep.shipped, update.shipped)
            checkNoDecrease("cancelled", lastStep.cancelled, update.cancelled)
        }

        private fun checkNoDecrease(field: String, from: Int, to: Int) {
            if (from > to) {
                throw IllegalArgumentException("Decreasing $field $from -> $to")
            }
        }

        fun fulfill(i: Int) {
            checkNoDecrease("xd-fulfillment", fulfilled, i)
            fulfilled = i
        }

    }

    val xd = Xd()

    var boringUpdate: BoringTotals? = null
    var stepName = ""

    fun next(update: BoringTotals, message: String = "") {
        if (boringUpdate == null) {
            boringUpdate = update
            stepName = message
        } else {
            throw IllegalStateException("Overriding boring state $boringUpdate with $update")
        }
    }

    private fun fulfill(delta: Int) {
        xd.fulfill( delta + xd.fulfilled)
    }

    fun then(expectedDelta: Int) {
        val lastDelta = xd.receive(boringUpdate?: throw IllegalStateException("no boring update yet"), stepName)
        boringUpdate = null
        assertThat(lastDelta).`as`(stepName).isEqualTo(expectedDelta)
    }

    data class BoringTotals (
            val stock: Int = 0,
            val total: Int = 0,
            val shipped: Int = 0,
            val cancelled: Int = 0
    ) {
        fun shortage(): Int {
            return max(total - shipped - cancelled - stock, 0)
        }

        override fun toString(): String {
            return "boring(total=$total, stock=$stock, shipped=$shipped, cancelled=$cancelled)"
        }


    }

}