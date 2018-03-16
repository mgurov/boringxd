package com.github.mgurov.boringxd.shortages

import com.github.mgurov.boringxd.BoringTotals
import com.github.mgurov.boringxd.Xd
import org.junit.Test

import org.assertj.core.api.Assertions.*
import org.junit.Ignore

class ShortageTest {

    @Test
    fun `normal flow`() {

        whenMessage(BoringTotals(total = 1), "shop order + 1")
        then(expectedDelta = 1)

        whenMessage(BoringTotals(total = 2), "shop order + 1")
        then(expectedDelta = 1)
    }

    @Test
    fun `existing stock 2`() {

        whenMessage(BoringTotals(total = 1, stock = 1), "shop order + 1")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 2, stock = 2), "shop order + 1")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 3, stock = 2), "shop order + 1")
        then(expectedDelta = 1)
    }

    @Test
    fun `shipment via our purchase`() {

        whenMessage(BoringTotals(total = 1), "shop order + 1")
        then(expectedDelta = 1)

        whenMessage(BoringTotals(total = 1, stock = 1), "stock via fulfillment")
        fulfill(1)
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 1, stock = 0, shipped = 1), "shipped")
        then(expectedDelta = 0)
    }

    @Test
    fun `stock fluctuation down`() {

        whenMessage(BoringTotals(total = 1, stock = 1), "shop order + 1")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 2, stock = 0), "stock lost + 1 shop order")
        then(expectedDelta = 2)
    }

    @Test
    fun `stock fluctuation down breakdown`() {

        whenMessage(BoringTotals(total = 1, stock = 1), "shop order + 1")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 1, stock = 0), "stock lost")
        then(expectedDelta = 1)

        whenMessage(BoringTotals(total = 2, stock = 0), "+ 1 shop order")
        then(expectedDelta = 1)
    }

    @Test
    fun `stock fluctuation up`() {
        whenMessage(BoringTotals(total = 1, stock = 0), "shop order + 1")
        then(expectedDelta = 1)

        whenMessage(BoringTotals(total = 2, stock = 1), "stock found! + 1 shop order")
        then(expectedDelta = 1) //TODO: shouldn't go up when stock found and all of our orders fulfilled
    }

    @Test
    @Ignore
    fun `stock fluctuation up breakdown`() {
        whenMessage(BoringTotals(total = 1, stock = 0), "shop order + 1")
        then(expectedDelta = 1)

        whenMessage(BoringTotals(total = 1, stock = 1), "stock found!")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 2, stock = 1), "+ 1 shop order")
        then(expectedDelta = 1) //TODO: only if not fulfilled
    }

    @Test
    fun `timing`() {

        whenMessage(BoringTotals(total = 1, stock = 0), "shop order + 1")
        then(expectedDelta = 1)

        whenMessage(BoringTotals(total = 1, stock = 1), "received unexpected")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 1, stock = 1), "finally received update")
        fulfill(1)
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 2, stock = 1), "+ 1 shop order")
        then(expectedDelta = 1, actualDelta = 0)
    }

    @Test
    fun `fluctuation down remembered and taken into account later`() {

        whenMessage(BoringTotals(total = 2, stock = 0), "shop order + 2 no stock yet")
        then(expectedDelta = 2)

        whenMessage(BoringTotals(total = 2, stock = 1), "stock went up")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 4, stock = 1), "shop order +2")
        then(expectedDelta = 2, actualDelta = 1)
    }

    @Test
    @Ignore
    fun `sanity check we get lots of stuff`() {
        whenMessage(BoringTotals(total = 2, stock = 0), "shop order + 2 no stock yet")
        then(expectedDelta = 2)

        whenMessage(BoringTotals(total = 2, stock = 10), "stock went up")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 15, stock = 10), "shop order +13")
        then(expectedDelta = 5)

        whenMessage(BoringTotals(total = 15, stock = 100), "stop up again")
        then(expectedDelta = 0)
    }

    @Test
    @Ignore
    fun `stock lost and isn't enough anymore`() {
        whenMessage(BoringTotals(total = 2, stock = 3), "enough stock")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 2, stock = 1), "oops, one lost")
        then(expectedDelta = 1)
    }

    @Test
    fun `stock lost and even more shortage`() {
        whenMessage(BoringTotals(total = 5, stock = 3), "2 missing")
        then(expectedDelta = 2)

        whenMessage(BoringTotals(total = 5, stock = 1), "2 more missing")
        then(expectedDelta = 2)
    }

    @Test
    fun `should order new orders and decreased amount`() {
        whenMessage(BoringTotals(total = 2, stock = 1), "one cdpa")
        then(expectedDelta = 1)

        whenMessage(BoringTotals(total = 3, stock = 0), "one more shop order and stock lost")
        then(expectedDelta = 2)
    }

    @Test
    @Ignore
    fun `stock lost not affecting previous shortage but does the new one`() {
        whenMessage(BoringTotals(total = 2, stock = 4), "no shortage")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 5, stock = 3), "shortage")
        then(expectedDelta = 2)
    }

    @Test
    fun `decrease supply over the demand doesn't affect bottomline`() {
        whenMessage(BoringTotals(total = 2, stock = 2), "no shortage")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 2, stock = 2), "still no shortage")
        then(expectedDelta = 0)
    }

    @Test
    @Ignore
    fun `stock found not affecting previous shortage but does the new one`() {
        whenMessage(BoringTotals(total = 2, stock = 3), "no shortage")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 5, stock = 4), "shortage")
        then(expectedDelta = 1)
    }

    @Test
    @Ignore
    fun `same stock bit more requested`() {
        whenMessage(BoringTotals(total = 2, stock = 3), "no shortage")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 5, stock = 3), "shortage")
        then(expectedDelta = 2)
    }

    @Test
    fun `same stock still no shortage`() {
        whenMessage(BoringTotals(total = 2, stock = 2), "no shortage")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 5, stock = 5), "no shortage")
        then(expectedDelta = 0)
    }

    @Test
    fun `same stock low`() {
        whenMessage(BoringTotals(total = 2, stock = 1), "shortage 1")
        then(expectedDelta = 1)

        whenMessage(BoringTotals(total = 5, stock = 1), "even more shortage")
        then(expectedDelta = 3)
    }

    @Test
    fun `growing stock low`() {
        whenMessage(BoringTotals(total = 3, stock = 1), "shortage")
        then(expectedDelta = 2)

        whenMessage(BoringTotals(total = 5, stock = 2), "even more shortage")
        then(expectedDelta = 2) //maybe the 1 was from the shortage
    }

    @Test
    fun `stop pops boring first delayed noticed xD`() {

        whenMessage(BoringTotals(total = 1, stock = 0), "customer order +1 -> cdpa")
        then(expectedDelta = 1)

        whenMessage(BoringTotals(total = 1, stock = 1), "our stuff has been delivered but we aren't yet aware of that")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 1, shipped = 1), "shipped")
        then(expectedDelta = 0)

        fulfill(1)
        whenMessage(BoringTotals(total = 1, shipped = 1), "now we've noticed")
        then(expectedDelta = 0)

        whenMessage(BoringTotals(total = 2, shipped = 1), "order +1")
        then(expectedDelta = 1, actualDelta = 0)
    }

    @Test
    @Ignore
    fun `stop pops boring first delayed noticed xD 2`() {

        whenMessage(BoringTotals(total = 1, stock = 0), "customer order +1 -> cdpa")
        then(expectedDelta = 1)

        whenMessage(BoringTotals(total = 2, stock = 1), "our stuff has been delivered but we aren't yet aware of that, should order just in case")
        then(expectedDelta = 1)

        fulfill(1)
        whenMessage(BoringTotals(total = 2, shipped = 1), "now we've noticed")
        then(expectedDelta = 0)
    }


    @Test
    @Ignore
    fun `should not think overshipped when notified bit late at xD`() {

        whenMessage(BoringTotals(total = 1, stock = 0), "customer order +1 -> cdpa")
        then(expectedDelta = 1)

        whenMessage(BoringTotals(total = 2, stock = 1), "our first piece has been delivered but we aren't yet aware of that")
        then(expectedDelta = 1)

        fulfill(1)
        whenMessage(BoringTotals(total = 2, shipped = 1), "now we've noticed")
        then(expectedDelta = 0)
    }

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
        then(expectedPositiveDelta = 0)

        whenMessage(BoringTotals(total = 10, stock = 0, shipped = 10), "final shipment")
        fulfill(3)
        then(expectedDelta = 0)
    }

    @Test
    @Ignore
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
        then(expectedDelta = 3, actualDelta = 1)

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

        fulfill(4)
        whenMessage(BoringTotals(total = 10, stock = 1, shipped = 5), "shipment + noticed we've delivered that thing")
        then(expectedPositiveDelta = 0)

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

    val xd = XdShortage() as Xd
    //val xd = XdTake1() as Xd

    var boringUpdate: BoringTotals? = null
    var stepName = ""
    var totalPurchased = 0

    fun whenMessage(update: BoringTotals, message: String = "") {
        if (boringUpdate == null) {
            boringUpdate = update
            stepName = message
        } else {
            throw IllegalStateException("Overriding boring state $boringUpdate with $update")
        }
    }

    private fun fulfill(delta: Int) {
    }

    fun then(expectedPositiveDelta: Int? = null,
             expectedDelta: Int? = null,
             actualDelta: Int? = null,
             skipShortageCheck: Boolean = false
    ) {
        val update = boringUpdate ?: throw IllegalStateException("no boring update yet")
        val lastDelta = xd.receive(update, stepName)
        if (!skipShortageCheck) {
            assertThat(lastDelta).`as`("no point of ever ordering more than missing, right?").isLessThanOrEqualTo(update.shortage())
        }
        totalPurchased += lastDelta
        assertThat(totalPurchased).`as`("Should not order more than total orders").isLessThanOrEqualTo(update.total)
        boringUpdate = null
        if (actualDelta != null && tolerateActual) {
            assertThat(lastDelta).`as`(stepName).isEqualTo(actualDelta)
        } else {
            if (expectedDelta != null) {
                assertThat(lastDelta).`as`(stepName).isEqualTo(expectedDelta)
            }
            if (expectedPositiveDelta != null) {
                assertThat(Integer.max(0, lastDelta)).`as`(stepName).isEqualTo(expectedPositiveDelta)
            }
        }
    }

    val tolerateActual = false

}