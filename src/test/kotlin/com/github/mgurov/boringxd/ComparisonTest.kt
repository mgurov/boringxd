package com.github.mgurov.boringxd

import com.github.mgurov.boringxd.deltasWithCancellations.XdTake3Cancel
import com.github.mgurov.boringxd.shortages.XdShortage
import org.junit.Test

import org.assertj.core.api.Assertions.*

class ComparisonTest {

    // XLS

    @Test
    fun `Create create ship`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        //TODO: why updated xls orders 2 on shipped = 6?
        whenMessage(BoringTotals(total = 10, stock = 7), "customer order +3 stock +4 via our fulfillment")
        then(expectedDelta = 3)

        whenMessage(BoringTotals(total = 10, stock = 2, shipped = 5), "Shipment")
        then(expectedDelta = 0, shouldBeFixedShortage = -5)

        whenMessage(BoringTotals(total = 10, stock = 0, shipped = 10), "final shipment")
        then(expectedDelta = 0)
    }

    @Test
    fun `Create cancel ship`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 7, stock = 2, cancelled = 3), "Cancel 3 of customer order")
        then(expectedDelta = -3)

        whenMessage(BoringTotals(total = 9, stock = 2, shipped = 3, cancelled = 3), "order 2 + shipment")
        then(expectedDelta = 1, shouldBeFixedShortage = 2)

        whenMessage(BoringTotals(total = 10, stock = 0,  shipped = 5, cancelled = 3), "order 3")
        then(expectedDelta = 1, shouldBeFixedShortage = -2)

        //TODO: check we reject negative shortage from boring
        //TODO: excel has stock=4, but that should not even be sent to us by shortage as the supply is higher than demand
        whenMessage(BoringTotals(total = 12, stock = 3,  shipped = 6, cancelled = 3), "order 4 (geleverde PO?)")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 13, stock = 3,  shipped = 6, cancelled = 3), "order 5 (NOS)")
        then(expectedDelta = 1, shouldBeFixedShortage = -1)
    }

    @Test
    fun `Create found_stock create ship`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "order 1")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 10, stock = 6), "order 2")
        then(expectedDelta = 3)

        whenMessage(BoringTotals(total = 12, stock = 0, shipped = 9), "order 3")
        then(expectedDelta = 2, shouldBeFixedShortage = -2)

        whenMessage(BoringTotals(total = 14, stock = 1, shipped = 10), "order 4")
        then(expectedDelta = 2, shouldBeFixedShortage = -1)

        whenMessage(BoringTotals(total = 14, stock = 0, shipped = 10), "NOS check 1")
        then(expectedDelta = 1, shouldBeFixedShortage = -1)

        whenMessage(BoringTotals(total = 14, stock = 4, shipped = 10), "NOS check 2")
        then(expectedDelta = 0)
    }

    @Test
    fun `Create shipment timing_issue`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "order 1")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 10, stock = 0, shipped = 2), "order 2 (& stock countdown)")
        then(expectedDelta = 3)
    }

    @Test
    fun `Create shipment timing_issue - order 2 (& stock countdown) alt (shipm 6)`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "order 1")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 8, stock = 0, shipped = 6), "- order 2 (& stock countdown) alt (shipm 6)")
        then(expectedDelta = 1)
    }

    @Test
    fun `Create shipment timing_issue - order 2 (partial stock countdown)`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "order 1")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 10, stock = 1, shipped = 2), "- order 2 (partial stock countdown)")
        then(expectedDelta = 3)
    }

    @Test
    fun `Create shipment timing_issue - order 2 (no stock countdown)`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "order 1")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 10, stock = 2, shipped = 2), "- order 2 (no stock countdown)")
        then(expectedDelta = 3)
    }

    @Test
    fun `Received goods not registered yet + NOS checks`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 10, stock = 6), "order 2")
        then(expectedDelta = 3)

        whenMessage(BoringTotals(total = 11, stock = 0, shipped = 7), "order 3")
        then(expectedDelta = 1, shouldBeFixedShortage = -3)

        whenMessage(BoringTotals(total = 11, stock = 4, shipped = 7), "stock check I")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 11, stock = 3, shipped = 7), "stock check II")
        then(expectedDelta = 1, shouldBeFixedShortage = -4)

    }

    @Test
    fun `Received goods not registered yet`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 10, stock = 6), "order 2")
        then(expectedDelta = 3)

        //TODO: excel seems to be sensitive to shipped, e.g. 7 becomes delta 2 where should stay 1
        whenMessage(BoringTotals(total = 11, stock = 0, shipped = 6), "order 3")
        then(expectedDelta = 1, shouldBeFixedShortage = -3)
    }

    @Test
    fun `Stock decreased`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 10, stock = 0), "customer order +3 stock fluctuation -2")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 10, stock = 0, shipped = 10), "final shipment")
        then(expectedDelta = 0)
    }

    @Test
    fun `Stock decreased due to shipment`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 10, stock = 0, shipped = 2), "customer order +3 stock fluctuation -2 upon shipment")
        then(expectedDelta = 3)

        whenMessage(BoringTotals(total = 10, stock = 0, shipped = 10), "final shipment")
        then(expectedDelta = 0)
    }

    @Test
    fun `Create shipment timing_issue old xls`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 7, stock = 2, shipped = 2), "shipment bypassing stock")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 10, stock = 0, shipped = 4), "customer order + 3 and stock shipped")
        then(expectedDelta = 3, shouldBeFixedShortage = 1)

        whenMessage(BoringTotals(total = 10, stock = 0, shipped = 10), "final Shipment")
        then(expectedDelta = 0)
    }

    @Test
    fun `Received goods not registered yet - old xls`() {

        whenMessage(BoringTotals(total = 7, stock = 2), "New customer order 7")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 10, stock = 6), "New customer order with the stock +4 unrelated to xD")
        then(expectedDelta = 3)

        whenMessage(BoringTotals(total = 10, stock = 1, shipped = 5), "shipment + noticed we've delivered that thing")
        then(expectedDelta = 0, shouldBeFixedShortage = -4)

        whenMessage(BoringTotals(total = 10, stock = 0, shipped = 10), "final Shipment")
        then(expectedDelta = 0)
    }

    //extra

    @Test
    fun `cancel`() {

        whenBoringMessage(total = 1, message= "shop order + 1")
        then(expectedDelta = 1)

        whenBoringMessage(total = 1, cancelled = 1, message= "shop order - 1")
        then(expectedDelta = -1, shouldBeFixedShortage = 0)
    }

    @Test
    fun `cancel and then order more with delivery`() {

        whenBoringMessage(total = 1, message= "shop order 1")
        then(expectedDelta = 1)

        whenBoringMessage(total = 1, cancelled = 1, message= "cancel shop order 1")
        then(expectedDelta = -1, shouldBeFixedShortage = 0)

        whenBoringMessage(total = 2, cancelled = 1, stock = 1, message= "shop order 2 stock ready")
        then(expectedDelta = 0)
    }


    val deltas = XdTake3Cancel() as Xd
    val shortages = XdShortage() as Xd

    var boringUpdate: BoringTotals? = null
    var stepName = ""

    fun whenBoringMessage(
            total: Int = 0,
            stock: Int = 0,
            shipped: Int = 0,
            cancelled: Int = 0,
            message: String = "") {
        whenMessage(
                BoringTotals(
                        total = total,
                        stock =  stock,
                        shipped = shipped,
                        cancelled = cancelled
                ),
                message
        )
    }

    fun whenMessage(update: BoringTotals, message: String = "") {
        if (boringUpdate == null) {
            boringUpdate = update
            stepName = message
        } else {
            throw IllegalStateException("Overriding boring state $boringUpdate with $update")
        }
    }

    fun then(expectedDelta: Int, shouldBeFixedShortage: Int? = null) {
        val update = boringUpdate ?: throw IllegalStateException("no boring update yet")
        assertThat(expectedDelta).`as`("no point of ever ordering more than missing, right?").isLessThanOrEqualTo(update.shortage())
        val lastDelta = deltas.receive(update, stepName)
        val lastShortage = shortages.receive(update, stepName)

        totalPurchasedDeltas += lastDelta
        assertThat(totalPurchasedDeltas).`as`("Should not order more than total orders deltas").isLessThanOrEqualTo(update.total)

        totalPurchasedShortages += lastShortage
        assertThat(totalPurchasedShortages).`as`("Should not order more than total orders shortages").isLessThanOrEqualTo(update.total)

        boringUpdate = null //reset

        assertThat(lastDelta).`as`(stepName + " - deltas").isEqualTo(expectedDelta)
        if (shouldBeFixedShortage != null) {
            assertThat(lastShortage).`as`(stepName + " - shortages should be fixed eventually").isEqualTo(shouldBeFixedShortage)
        } else {
            assertThat(lastShortage).`as`(stepName + " - shortages").isEqualTo(expectedDelta)
        }
    }

    var totalPurchasedShortages = 0
    var totalPurchasedDeltas = 0
}