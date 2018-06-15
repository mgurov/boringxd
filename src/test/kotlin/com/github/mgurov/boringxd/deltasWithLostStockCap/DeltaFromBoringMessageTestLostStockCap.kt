package com.github.mgurov.boringxd.deltasWithLostStockCap

import com.github.mgurov.boringxd.BoringTotals
import com.github.mgurov.boringxd.deltasWithLostStockCapExcess.XdTake4LostStockCapExcess
import com.github.mgurov.boringxd.deltasWithLostStockCapRedistribution.XdTake4LostStockCapREdistribution
import org.junit.Test

import org.assertj.core.api.Assertions.*
import org.assertj.core.api.SoftAssertions.assertSoftly

class DeltaFromBoringMessageTestLostStockCap {

    @Test
    fun `normal flow`() {

        whenBoringMessage(total = 1, message= "shop order + 1")
        then(expectedDelta = 1)

        whenBoringMessage(total = 2, message= "shop order + 1")
        then(expectedDelta = 1)
    }

    @Test
    fun `cancel`() {

        whenBoringMessage(total = 1, message= "shop order + 1")
        then(expectedDelta = 1)

        whenBoringMessage(total = 1, cancelled = 1, message= "shop order - 1")
        then(expectedDelta = -1)
    }

    @Test
    fun `cancel and then order more with delivery`() {

        whenBoringMessage(total = 1, message= "shop order 1")
        then(expectedDelta = 1)

        whenBoringMessage(total = 1, cancelled = 1, message= "cancel shop order 1")
        then(expectedDelta = -1)

        whenBoringMessage(total = 2, cancelled = 1, stock = 1, message= "shop order 2 stock ready")
        then(expectedDelta = 0)
    }

    @Test
    fun `cancel and order same time when shipment arrived`() {

        whenBoringMessage(total = 1, message= "shop order 1")
        then(expectedDelta = 1)

        whenBoringMessage(total = 2, cancelled = 1, shipped = 1, message= "cancel shop order 1")
        then(expectedDelta = 0)
    }

    @Test
    fun `cancellation with stock decrease`() {

        whenBoringMessage(total = 10, stock = 5, message= "shop order 1")
        then(expectedDelta = 5, expectedStockExcess = 0)

        whenBoringMessage(total = 10, stock = 2, cancelled = 3, message= "drop cancel")
        then(expectedDelta = 0, expectedStockExcess = 0)
    }

    @Test
    fun `cancellation followed by stock decrease`() {

        whenBoringMessage(total = 10, stock = 5, message= "shop order 1")
        then(expectedDelta = 5, expectedStockExcess = 0)

        whenBoringMessage(total = 10, stock = 5, cancelled = 3, message= "cancel")
        then(expectedDelta = -3, expectedStockExcess = 3)

        whenBoringMessage(total = 10, stock = 2, cancelled = 3, message= "drop")
        then(expectedDelta = 0, expectedStockExcess = 0)
    }

    @Test
    fun `cancellation with demand increase`() {

        whenBoringMessage(total = 10, stock = 5, message= "shop order 1")
        then(expectedDelta = 5)

        whenBoringMessage(total = 12, stock = 5, cancelled = 3, message= "order and cancel")
        then(expectedDelta = -1)
    }

    @Test
    fun `existing stock 2`() {

        whenBoringMessage(total = 1, stock = 2, message= "shop order + 1")
        then(expectedDelta = 0)

        whenBoringMessage(total = 2, stock = 2, message= "shop order + 1")
        then(expectedDelta = 0)

        whenBoringMessage(total = 3, stock = 2, message= "shop order + 1")
        then(expectedDelta = 1)
    }

    @Test
    fun `shipment via our purchase`() {

        whenBoringMessage(total = 1, message= "shop order + 1")
        then(expectedDelta = 1)

        whenBoringMessage(total = 1, stock = 1, message= "stock via fulfillment")
        then(expectedDelta = 0)

        whenBoringMessage(total = 1, stock = 0, shipped = 1, message= "shipped")
        then(expectedDelta = 0)
    }

    @Test
    fun `stock fluctuation down`() {

        whenBoringMessage(total = 1, stock = 1, message= "shop order + 1")
        then(expectedDelta = 0)

        whenBoringMessage(total = 2, stock = 0, message= "stock lost + 1 shop order")
        then(expectedDelta = 2)
    }

    @Test
    fun `stock fluctuation down breakdown`() {

        whenBoringMessage(total = 1, stock = 1, message= "shop order + 1")
        then(expectedDelta = 0)

        whenBoringMessage(total = 1, stock = 0, message= "stock lost")
        then(expectedDelta = 1)

        whenBoringMessage(total = 2, stock = 0, message= "+ 1 shop order")
        then(expectedDelta = 1)
    }

    @Test
    fun `stock fluctuation up`() {
        whenBoringMessage(total = 1, stock = 0, message= "shop order + 1")
        then(expectedDelta = 1)

        whenBoringMessage(total = 2, stock = 1, message= "stock found! + 1 shop order")
        then(expectedDelta = 1) //TODO: shouldn't go up when stock found and all of our orders fulfilled
    }

    @Test
    fun `stock fluctuation up breakdown`() {
        whenBoringMessage(total = 1, stock = 0, message= "shop order + 1")
        then(expectedDelta = 1)

        whenBoringMessage(total = 1, stock = 1, message= "stock found!")
        then(expectedDelta = 0)

        whenBoringMessage(total = 2, stock = 1, message= "+ 1 shop order")
        then(expectedDelta = 1) //TODO: only if not fulfilled
    }

    @Test
    fun `timing`() {

        whenBoringMessage(total = 1, stock = 0, message= "shop order + 1")
        then(expectedDelta = 1)

        whenBoringMessage(total = 1, stock = 1, message= "received unexpected")
        then(expectedDelta = 0)

        whenBoringMessage(total = 1, stock = 1, message= "finally received update")
        then(expectedDelta = 0)

        whenBoringMessage(total = 2, stock = 1, message= "+ 1 shop order")
        then(expectedDelta = 1)
    }

    @Test
    fun `fluctuation down remembered and taken into account later`() {

        whenBoringMessage(total = 2, stock = 0, message= "shop order + 2 no stock yet")
        then(expectedDelta = 2)

        whenBoringMessage(total = 2, stock = 1, message= "stock went up")
        then(expectedDelta = 0)

        whenBoringMessage(total = 4, stock = 1, message= "shop order +2")
        then(expectedDelta = 2)
    }

    @Test
    fun `sanity check we get lots of stuff`() {
        whenBoringMessage(total = 2, stock = 0, message= "shop order + 2 no stock yet")
        then(expectedDelta = 2)

        whenBoringMessage(total = 2, stock = 10, message= "stock went up")
        then(expectedDelta = 0)

        whenBoringMessage(total = 15, stock = 10, message= "shop order +13")
        then(expectedDelta = 5)

        whenBoringMessage(total = 15, stock = 100, message= "stop up again")
        then(expectedDelta = 0)
    }

    @Test
    fun `stock lost and isn't enough anymore`() {
        whenBoringMessage(total = 2, stock = 3, message= "enough stock")
        then(expectedDelta = 0)

        whenBoringMessage(total = 2, stock = 1, message= "oops, one lost")
        then(expectedDelta = 1)
    }

    @Test
    fun `stock lost and even more shortage`() {
        whenBoringMessage(total = 5, stock = 3, message= "2 missing")
        then(expectedDelta = 2)

        whenBoringMessage(total = 5, stock = 1, message= "2 more missing")
        then(expectedDelta = 2)
    }

    @Test
    fun `should order new orders and decreased amount`() {
        whenBoringMessage(total = 2, stock = 1, message= "one cdpa")
        then(expectedDelta = 1)

        whenBoringMessage(total = 3, stock = 0, message= "one more shop order and stock lost")
        then(expectedDelta = 2)
    }

    @Test
    fun `stock lost not affecting previous shortage but does the new one`() {
        whenBoringMessage(total = 2, stock = 4, message= "no shortage")
        then(expectedDelta = 0)

        whenBoringMessage(total = 5, stock = 3, message= "shortage")
        then(expectedDelta = 2)
    }

    @Test
    fun `decrease supply over the demand doesn't affect bottomline`() {
        whenBoringMessage(total = 2, stock = 4, message= "no shortage")
        then(expectedDelta = 0)

        whenBoringMessage(total = 2, stock = 3, message= "still no shortage")
        then(expectedDelta = 0)
    }

    @Test
    fun `stock found not affecting previous shortage but does the new one`() {
        whenBoringMessage(total = 2, stock = 3, message= "no shortage")
        then(expectedDelta = 0)

        whenBoringMessage(total = 5, stock = 4, message= "shortage")
        then(expectedDelta = 1)
    }

    @Test
    fun `same stock bit more requested`() {
        whenBoringMessage(total = 2, stock = 3, message= "no shortage")
        then(expectedDelta = 0)

        whenBoringMessage(total = 5, stock = 3, message= "shortage")
        then(expectedDelta = 2)
    }

    @Test
    fun `same stock still no shortage`() {
        whenBoringMessage(total = 2, stock = 5, message= "no shortage")
        then(expectedDelta = 0)

        whenBoringMessage(total = 5, stock = 5, message= "no shortage")
        then(expectedDelta = 0)
    }

    @Test
    fun `same stock low`() {
        whenBoringMessage(total = 2, stock = 1, message= "shortage 1")
        then(expectedDelta = 1)

        whenBoringMessage(total = 5, stock = 1, message= "even more shortage")
        then(expectedDelta = 3)
    }

    @Test
    fun `growing stock low`() {
        whenBoringMessage(total = 3, stock = 1, message= "shortage")
        then(expectedDelta = 2)

        whenBoringMessage(total = 5, stock = 2, message= "even more shortage")
        then(expectedDelta = 2) //maybe the 1 was from the shortage
    }

    @Test
    fun `stop pops boring first delayed noticed xD`() {

        whenBoringMessage(total = 1, stock = 0, message= "customer order +1 -> cdpa")
        then(expectedDelta = 1)

        whenBoringMessage(total = 1, stock = 1, message= "our stuff has been delivered but we aren't yet aware of that")
        then(expectedDelta = 0)

        whenBoringMessage(total = 1, shipped = 1, message= "shipped")
        then(expectedDelta = 0)

        whenBoringMessage(total = 1, shipped = 1, message= "now we've noticed")
        then(expectedDelta = 0)

        whenBoringMessage(total = 2, shipped = 1, message= "order +1")
        then(expectedDelta = 1)
    }

    @Test
    fun `stop pops boring first delayed noticed xD 2`() {

        whenBoringMessage(total = 1, stock = 0, message= "customer order +1 -> cdpa")
        then(expectedDelta = 1)

        whenBoringMessage(total = 2, stock = 1, message= "our stuff has been delivered but we aren't yet aware of that, should order just in case")
        then(expectedDelta = 1)

        whenBoringMessage(total = 2, shipped = 1, message= "now we've noticed")
        then(expectedDelta = 0)
    }


    @Test
    fun `should not think overshipped when notified bit late at xD`() {

        whenBoringMessage(total = 1, stock = 0, message= "customer order +1 -> cdpa")
        then(expectedDelta = 1)

        whenBoringMessage(total = 2, stock = 1, message= "our first piece has been delivered but we aren't yet aware of that")
        then(expectedDelta = 1)

        whenBoringMessage(total = 2, shipped = 1, message= "now we've noticed")
        then(expectedDelta = 0)
    }

    // XLS

    @Test
    fun `Create create ship`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order 7")
        then(expectedDelta = 5)

        whenBoringMessage(total = 10, stock = 6, message= "customer order +3 stock +4 via our fulfillment")
        then(expectedDelta = 3)

        whenBoringMessage(total = 10, stock = 2, shipped = 5, message= "Shipment")
        then(expectedDelta = 0)

        whenBoringMessage(total = 10, stock = 0, shipped = 10, message= "final shipment")
        then(expectedDelta = 0)
    }

    @Test
    fun `Create cancel ship`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order 7")
        then(expectedDelta = 5)

        whenBoringMessage(total = 7, stock = 2, cancelled = 3, message= "Cancel 3 of customer order")
        then(expectedDelta = -3)

        whenBoringMessage(total = 7, stock = 0, cancelled = 3, shipped = 4, message= "Shipment")
        then(expectedDelta = 0)
    }

    @Test
    fun `Create found_stock create ship`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order 7")
        then(expectedDelta = 5)

        whenBoringMessage(total = 10, stock = 6, message= "+3 customer order")
        then(expectedDelta = 3)

        whenBoringMessage(total = 10, stock = 0, shipped = 10, message= "Shipment")
        then(expectedDelta = 0)
    }

    @Test
    fun `Create shipment timing_issue`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order 7")
        then(expectedDelta = 5)

        whenBoringMessage(total = 7, stock = 2, shipped = 2, message= "shipment bypassing stock")
        then(expectedDelta = 0)

        whenBoringMessage(total = 10, stock = 0, shipped = 4, message= "customer order + 3 and stock shipped")
        then(expectedDelta = 3)

        whenBoringMessage(total = 10, stock = 0, shipped = 10, message= "final Shipment")
        then(expectedDelta = 0)
    }

    @Test
    fun `Received goods not registered yet`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order 7")
        then(expectedDelta = 5)

        whenBoringMessage(total = 10, stock = 6, message= "New customer order with the stock +4 unrelated to xD")
        then(expectedDelta = 3)

        whenBoringMessage(total = 10, stock = 1, shipped = 5, message= "shipment + noticed we've delivered that thing")
        then(expectedDelta = 0)

        whenBoringMessage(total = 10, stock = 0, shipped = 10, message= "final Shipment")
        then(expectedDelta = 0)
    }

    @Test
    fun `Stock decreased`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order 7")
        then(expectedDelta = 5, expectedStockCoverage = 2)

        whenBoringMessage(total = 10, stock = 0, message= "customer order +3 stock fluctuation -2")
        then(expectedDelta = 5, expectedStockCoverage = 0)

        whenBoringMessage(total = 10, stock = 0, shipped = 10, message= "final shipment")
        then(expectedDelta = 0, expectedStockCoverage = 0)
    }

    @Test
    fun `Stock coverage grows`() {

        whenBoringMessage(total = 1, stock = 1, message= "New customer order covered by stock")
        then(expectedDelta = 0, expectedStockCoverage = 1)

        whenBoringMessage(total = 2, stock = 2, message= "ditto")
        then(expectedDelta = 0, expectedStockCoverage = 2)

        whenBoringMessage(total = 2, stock = 0, message= "all the stock is lost")
        then(expectedDelta = 2, expectedStockCoverage = 0)
    }

    @Test
    fun `Stock coverage decreases upon shipment and cancellation`() {

        whenBoringMessage(total = 2, stock = 2, message= "New customer order covered by stock")
        then(expectedDelta = 0, expectedStockCoverage = 2)

        whenBoringMessage(total = 2, stock = 0, shipped = 1, cancelled = 1, message= "all is fulfilled")
        then(expectedDelta = 0, expectedStockCoverage = 0)
    }

    @Test
    fun `Stock coverage stays intact upon cancellation`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order covered by stock")
        then(expectedDelta = 5, expectedStockExcess = 0)

        whenBoringMessage(total = 7, stock = 2, cancelled = 1, message= "cancelled")
        then(expectedDelta = -1, expectedStockExcess = 1)

        whenBoringMessage(total = 7, stock = 1, cancelled = 1, message= "stock decrease")
        then(expectedDelta = 0, expectedStockExcess = 0)

    }

    @Test
    fun `Stock coverage decreases upon shipment`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order covered by stock")
        then(expectedDelta = 5, expectedStockCoverage = 2, expectedStockExcess = 0)

        whenBoringMessage(total = 7, stock = 2, shipped = 1, message= "shipped")
        then(expectedDelta = 0, expectedStockCoverage = 1, expectedStockExcess = 1)

        whenBoringMessage(total = 7, stock = 1, shipped = 1, message= "shipped effect on stock")
        then(expectedDelta = 0, expectedStockCoverage = 0, expectedStockExcess = 0)

        whenBoringMessage(total = 7, stock = 0, shipped = 1, message= "stock is lost")
        then(expectedDelta = 1, expectedStockCoverage = 0, expectedStockExcess = 0)

        whenBoringMessage(total = 7, stock = 1, shipped = 1, message= "and then found")
        then(expectedDelta = 0, expectedStockCoverage = 0, expectedStockExcess = 1)
    }

    @Test
    fun `Stock coverage stays intact upon cancellation 2`() {

        whenBoringMessage(total = 2, stock = 2, message= "New customer order covered by stock")
        then(expectedDelta = 0, expectedStockCoverage = 2)

        whenBoringMessage(total = 2, stock = 1, cancelled = 1, message= "cancelled")
        then(expectedDelta = 0, expectedStockCoverage = 1)

        whenBoringMessage(total = 2, stock = 0, cancelled = 1, message= "decrease")
        then(expectedDelta = 1, expectedStockCoverage = 0)

    }

    @Test
    fun `Don't cancel if not purchased`() {

        whenBoringMessage(total = 1, stock = 1, message= "customer order from stock")
        then(expectedDelta = 0, expectedStockCoverage = 0)

        whenBoringMessage(total = 1, stock = 0, cancelled = 1, message= "cancelled")
        then(expectedDelta = 0, expectedStockCoverage = 0)
    }

    @Test
    fun `shipped`() {

        whenBoringMessage(total = 1, stock = 1, message= "customer order from stock")
        then(expectedDelta = 0, expectedStockCoverage = 0)

        whenBoringMessage(total = 1, stock = 0, shipped = 1, message= "shipped")
        then(expectedDelta = 0, expectedStockCoverage = 0)
    }

    @Test
    fun `Stock coverage decreases upon shipment 2`() {

        whenBoringMessage(total = 2, stock = 2, message= "New customer order covered by stock")
        then(expectedDelta = 0, expectedStockCoverage = 2)

        whenBoringMessage(total = 2, stock = 1, shipped = 1, message= "shipped")
        then(expectedDelta = 0, expectedStockCoverage = 1)

        whenBoringMessage(total = 2, stock = 0, shipped = 1, message= "stock is lost")
        then(expectedDelta = 1, expectedStockCoverage = 0)

        whenBoringMessage(total = 2, stock = 1, shipped = 1, message= "and then found")
        then(expectedDelta = 0, expectedStockCoverage = 0)
    }

    @Test
    fun `Stock coverage stays intact upon cancellation 3`() {

        whenBoringMessage(total = 2, stock = 1, message= "New customer order covered by stock")
        then(expectedDelta = 1, expectedStockExcess = 0)

        whenBoringMessage(total = 2, stock = 1, cancelled = 1, message= "cancelled")
        then(expectedDelta = -1, expectedStockExcess = 0)

        whenBoringMessage(total = 2, stock = 0, cancelled = 1, message= "decrease")
        then(expectedDelta = 1, expectedStockExcess = 0)
    }

    @Test
    fun `Stock coverage decreases upon shipment or cancellation when shortage`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order")
        then(expectedDelta = 5, expectedStockCoverage = 2)

        whenBoringMessage(total = 7, stock = 2, cancelled = 1, message= "cancelled")
        then(expectedDelta = -1, expectedStockCoverage = 1)

    }

    @Test
    fun `stock increase followed by decrease`() {

        whenBoringMessage(total = 1, stock = 0, message= "New customer order")
        then(expectedDelta = 1, expectedStockExcess = 0)

        whenBoringMessage(total = 1, stock = 1, message= "some stock")
        then(expectedDelta = 0, expectedStockExcess = 1)

        whenBoringMessage(total = 1, stock = 0, message= "stock lost again")
        then(expectedDelta = 0, expectedStockExcess = 0)
    }

    @Test
    fun `shipment with stock increase`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order")
        then(expectedDelta = 5, expectedStockExcess = 0)

        whenBoringMessage(total = 7, stock = 2, shipped = 1, message= "shipped no stock changes")
        then(expectedDelta = 0, expectedStockExcess = 1)

        whenBoringMessage(total = 7, stock = 1, shipped = 1, message= "delayed decrease ignored")
        then(expectedDelta = 0, expectedStockExcess = 0)

        whenBoringMessage(total = 7, stock = 0, shipped = 1, message= "further decrease is indeed stock lost")
        then(expectedDelta = 1, expectedStockExcess = 0)
    }

    @Test
    fun `customer order with stock increase`() {

        whenBoringMessage(total = 2, stock = 2, message= "customer order")
        then(expectedDelta = 0, expectedStockExcess = 0)

        whenBoringMessage(total = 3, stock = 3, message= "+1 order covered")
        then(expectedDelta = 0, expectedStockExcess = 0)

        whenBoringMessage(total = 3, stock = 0, message= "stock decrease")
        then(expectedDelta = 3, expectedStockExcess = 0)
    }

    @Test
    fun `stock decrease before shipment issues delta still`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order")
        then(expectedDelta = 5, expectedStockCoverage = 2, expectedStockExcess = 0)

        whenBoringMessage(total = 7, stock = 1, shipped = 0, message= "stock decrease just before shipment")
        then(expectedDelta = 1, expectedStockCoverage = 1, expectedStockExcess = 0)

        whenBoringMessage(total = 7, stock = 1, shipped = 1, message= "delayed shipment")
        then(expectedDelta = 0, expectedStockCoverage = 0, expectedStockExcess = 1)

        whenBoringMessage(total = 7, stock = 0, shipped = 1, message= "stock lost already accounted earlier")
        then(expectedDelta = 0, expectedStockCoverage = 0, expectedStockExcess = 0)
    }

    //TODO: decreased stock lost purchases should also reduce the stock excess

    val xd = XdTake4LostStockCapREdistribution()

    var boringUpdate: BoringTotals? = null
    var stepName = ""
    var totalPurchased = 0

    fun whenBoringMessage(
            total: Int = 0,
            stock: Int = 0,
            shipped: Int = 0,
            cancelled: Int = 0,
            message: String = "") {

        require(total >= shipped + cancelled)
        
        val cappedStock = Math.min(stock, total - shipped - cancelled)
        val stockExcess = stock - cappedStock

        val update = BoringTotals(
                total = total,
                stock = cappedStock,
                shipped = shipped,
                cancelled = cancelled,
                stockExcess = stockExcess
        )

        if (boringUpdate == null) {
            boringUpdate = update
            stepName = message
        } else {
            throw IllegalStateException("Overriding boring state $boringUpdate with $update")
        }
    }

    fun then(expectedDelta: Int,
             expectedStockCoverage: Int? = null,
             expectedStockExcess: Int? = null
    ) {
        val update = boringUpdate ?: throw IllegalStateException("no boring update yet")

        assertThat(expectedDelta).`as`("no point of ever ordering more than missing, right?").isLessThanOrEqualTo(update.shortage())

        val lastDelta = xd.receive(update, stepName)
        totalPurchased += lastDelta
        //assertThat(totalPurchased).`as`("Should not order more than total orders").isLessThanOrEqualTo(update.total)
        boringUpdate = null

        assertSoftly {softly ->
//            if (expectedStockCoverage != null) {
//                softly.assertThat(xd.steps.last().coveredByStock).`as`("covered by stock: " + stepName).isEqualTo(expectedStockCoverage)
//            }

            if (expectedStockExcess != null) {
                softly.assertThat(xd.steps.last().stockExcess).`as`("stock excess : " + stepName).isEqualTo(expectedStockExcess)
            }

            softly.assertThat(lastDelta).`as`("delta : " + stepName).isEqualTo(expectedDelta)
        }


    }

}