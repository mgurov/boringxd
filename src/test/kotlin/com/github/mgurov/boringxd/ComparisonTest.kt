package com.github.mgurov.boringxd

import com.github.mgurov.boringxd.deltas.XdTake2
import com.github.mgurov.boringxd.shortages.XdShortage
import org.junit.Test

import org.assertj.core.api.Assertions.*

class ComparisonTest {

    // XLS

    @Test
    fun `Create create ship`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 10, stock = 6), "customer order +3 stock +4 via our fulfillment")
        fulfill(4)
        then(expectedDelta = 3)

        whenMessage(BoringTotals(total = 10, stock = 2, shipped = 5), "Shipment")
        fulfill(1)
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 10, stock = 0, shipped = 10), "final shipment")
        fulfill(3)
        then(expectedDelta = 0)
    }

    @Test
    fun `Create cancel ship`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 7, stock = 2, cancelled = 3), "Cancel 3 of customer order")
        then(expectedDelta = 0) //TODO: -3 w/cancellation

        whenMessage(BoringTotals(total = 7, stock = 0, cancelled = 3, shipped = 4), "Shipment")
        fulfill(2)
        then(expectedDelta = 0)
    }

    @Test
    fun `Create found_stock create ship`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 10, stock = 6), "+3 customer order")
        fulfill(1)
        then(expectedDelta = 3)

        whenMessage(BoringTotals(total = 10, stock = 0, shipped = 10), "Shipment")
        fulfill(4)
        then(expectedDelta = 0)
    }

    @Test
    fun `Create shipment timing_issue`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 7, stock = 2, shipped = 2), "shipment bypassing stock")
        fulfill(2)
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 10, stock = 0, shipped = 4), "customer order + 3 and stock shipped")
        then(expectedDelta = 3)

        whenMessage(BoringTotals(total = 10, stock = 0, shipped = 10), "final Shipment")
        fulfill(6)
        then(expectedDelta = 0)
    }

    @Test
    fun `Received goods not registered yet`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 10, stock = 6), "New customer order with the stock +4 unrelated to xD")
        then(expectedDelta = 3)

        whenMessage(BoringTotals(total = 10, stock = 1, shipped = 5), "shipment + noticed we've delivered that thing")
        then(expectedDelta = 0)

        fulfill(4)
        whenMessage(BoringTotals(total = 10, stock = 0, shipped = 10), "final Shipment")
        then(expectedDelta = 0)
    }

    @Test
    fun `Stock decreased`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 10, stock = 0), "customer order +3 stock fluctuation -2")
        then(expectedDelta = 5)

        fulfill(10)
        whenMessage(BoringTotals(total = 10, stock = 0, shipped = 10), "final shipment")
        then(expectedDelta = 0)
    }

    val deltas = XdTake2() as Xd
    val shortages = XdShortage() as Xd

    var boringUpdate: BoringTotals? = null
    var stepName = ""

    fun whenMessage(update: BoringTotals, message: String = "") {
        if (boringUpdate == null) {
            boringUpdate = update
            stepName = message
        } else {
            throw IllegalStateException("Overriding boring state $boringUpdate with $update")
        }
    }

    private fun fulfill(ignore: Int) {
    }

    fun then(expectedDelta: Int) {
        val update = boringUpdate ?: throw IllegalStateException("no boring update yet")
        assertThat(expectedDelta).`as`("no point of ever ordering more than missing, right?").isLessThanOrEqualTo(update.shortage())
        val lastDelta = deltas.receive(update, stepName)
        val lastShortage = shortages.receive(update, stepName)

/*TODO: do we need these?
        totalPurchased += lastDelta
        assertThat(totalPurchased).`as`("Should not order more than total orders").isLessThanOrEqualTo(update.total)
*/
        boringUpdate = null
        assertThat(lastDelta).`as`(stepName + " - deltas").isEqualTo(expectedDelta)
        assertThat(lastShortage).`as`(stepName + " - shortages").isEqualTo(expectedDelta)
    }

    var totalPurchased = 0
}