package com.github.mgurov.boringxd.deltasWithLostStockCap

import com.github.mgurov.boringxd.BoringTotals
import com.github.mgurov.boringxd.deltasWithLostStockCapCellStatuses.XdTake4LostStockCapCellStatuses
import org.junit.Test

import org.assertj.core.api.Assertions.*
import org.assertj.core.api.SoftAssertions.assertSoftly

class DeltaFromBoringMessageTestLostStockCap {

    @Test
    fun `normal flow`() {

        whenBoringMessage(total = 1, message= "shop order + 1")
        then(delta = 1)

        whenBoringMessage(total = 2, message= "shop order + 1")
        then(delta = 1)
    }

    @Test
    fun `cancel`() {

        whenBoringMessage(total = 1, message= "shop order + 1")
        then(delta = 1)

        whenBoringMessage(total = 1, cancelled = 1, message= "shop order - 1")
        then(delta = -1)
    }

    @Test
    fun `cancel and then order more with delivery`() {

        whenBoringMessage(total = 1, message= "shop order 1")
        then(delta = 1)

        whenBoringMessage(total = 1, cancelled = 1, message= "cancel shop order 1")
        then(delta = -1)

        whenBoringMessage(total = 2, cancelled = 1, stock = 1, message= "shop order 2 stock ready")
        then(delta = 0)
    }

    @Test
    fun `cancel and order same time when shipment arrived`() {

        whenBoringMessage(total = 1, message= "shop order 1")
        then(delta = 1, stockExcess = 0)

        whenBoringMessage(total = 2, cancelled = 1, shipped = 1, message= "cancel shop order 1")
        then(delta = 0, stockExcess = 0)
    }

    @Test
    fun `cancellation with stock decrease`() {

        whenBoringMessage(total = 10, stock = 5, message= "shop order 1")
        then(delta = 5, stockExcess = 0)

        whenBoringMessage(total = 10, stock = 2, cancelled = 3, message= "drop cancel")
        then(delta = 0, stockExcess = 0) //TODO: this one is not working now
    }

    @Test
    fun `should not cancel when in exchange for the stock`() {

        whenBoringMessage(total = 2, stock = 1, message= "shop order 1")
        then(delta = 1, stockExcess = 0)

        whenBoringMessage(total = 2, stock = 0, cancelled = 1, message= "cancel the stock one")
        then(delta = 0, stockExcess = 0)
    }

    @Test
    fun `should not cancel when in exchange for the stock 1`() {

        whenBoringMessage(total = 2, stock = 1, message= "shop order 1")
        then(delta = 1, stockExcess = 0)

        whenBoringMessage(total = 2, stock = 1, cancelled = 1, message= "cancel the purchased one")
        then(delta = -1, stockExcess = 0)

        whenBoringMessage(total = 2, stock = 0, cancelled = 1, message= "stock lost")
        then(delta = 1, stockExcess = 0)
    }

    @Test
    fun `cancellation followed by stock decrease`() {

        whenBoringMessage(total = 10, stock = 5, message= "shop order 1")
        then(delta = 5, stockExcess = 0, purchased = 5)

        whenBoringMessage(total = 10, stock = 5, cancelled = 3, message= "cancel")
        then(delta = -3, stockExcess = 0, purchased = 2)

        whenBoringMessage(total = 10, stock = 2, cancelled = 3, message= "drop")
        then(delta = 3, stockExcess = 0, purchased = 5)
    }

    @Test
    fun `cancellation followed by stock decrease smaller`() {

        whenBoringMessage(total = 2, stock = 1, message= "shop order 1")
        then(delta = 1, stockExcess = 0, purchased = 1)

        whenBoringMessage(total = 2, stock = 1, cancelled = 1, message= "cancel")
        then(delta = -1, stockExcess = 0, purchased = 0)

        whenBoringMessage(total = 2, stock = 0, cancelled = 1, message= "drop")
        then(delta = 1, stockExcess = 0, purchased = 1)
    }

    @Test
    fun `cancellation with demand increase`() {

        whenBoringMessage(total = 10, stock = 5, message= "shop order 1")
        then(delta = 5)

        whenBoringMessage(total = 12, stock = 5, cancelled = 3, message= "order and cancel")
        then(delta = -1, stockExcess = 0)
    }

    @Test
    fun `existing stock 2`() {

        whenBoringMessage(total = 1, stock = 1, message= "shop order + 1")
        then(delta = 0)

        whenBoringMessage(total = 2, stock = 2, message= "shop order + 1")
        then(delta = 0)

        whenBoringMessage(total = 3, stock = 2, message= "shop order + 1")
        then(delta = 1)
    }

    @Test
    fun `shipment via our purchase`() {

        whenBoringMessage(total = 1, message= "shop order + 1")
        then(delta = 1)

        whenBoringMessage(total = 1, stock = 1, message= "stock via fulfillment")
        then(delta = 0, stockExcess = 1)

        whenBoringMessage(total = 1, stock = 0, shipped = 1, message= "shipped")
        then(delta = 0)
    }

    @Test
    fun `stock fluctuation down`() {

        whenBoringMessage(total = 1, stock = 1, message= "shop order + 1")
        then(delta = 0)

        whenBoringMessage(total = 2, stock = 0, message= "stock lost + 1 shop order")
        then(delta = 2)
    }

    @Test
    fun `stock fluctuation down breakdown`() {

        whenBoringMessage(total = 1, stock = 1, message= "shop order + 1")
        then(delta = 0)

        whenBoringMessage(total = 1, stock = 0, message= "stock lost")
        then(delta = 1)

        whenBoringMessage(total = 2, stock = 0, message= "+ 1 shop order")
        then(delta = 1)
    }

    @Test
    fun `stock fluctuation up`() {
        whenBoringMessage(total = 1, stock = 0, message= "shop order + 1")
        then(delta = 1)

        whenBoringMessage(total = 2, stock = 1, message= "stock found! + 1 shop order")
        then(delta = 1, stockExcess = 1)
    }

    @Test
    fun `stock fluctuation up breakdown`() {
        whenBoringMessage(total = 1, stock = 0, message= "shop order + 1")
        then(delta = 1)

        whenBoringMessage(total = 1, stock = 1, message= "stock found!")
        then(delta = 0, stockExcess = 1)

        whenBoringMessage(total = 2, stock = 1, message= "+ 1 shop order")
        then(delta = 1, stockExcess = 1)
    }

    @Test
    fun `timing`() {

        whenBoringMessage(total = 1, stock = 0, message= "shop order + 1")
        then(delta = 1)

        whenBoringMessage(total = 1, stock = 1, message= "received unexpected")
        then(delta = 0, stockExcess = 1)

        whenBoringMessage(total = 1, stock = 1, message= "finally received update")
        then(delta = 0, stockExcess = 1) //TODO: why reset? should've kept

        whenBoringMessage(total = 2, stock = 1, message= "+ 1 shop order")
        then(delta = 1, stockExcess = 1)
    }

    @Test
    fun `fluctuation down remembered and taken into account later`() {

        whenBoringMessage(total = 2, stock = 0, message= "shop order + 2 no stock yet")
        then(delta = 2)

        whenBoringMessage(total = 2, stock = 1, message= "stock went up")
        then(delta = 0, stockExcess = 1)

        whenBoringMessage(total = 4, stock = 1, message= "shop order +2")
        then(delta = 2, stockExcess = 1)

        whenBoringMessage(total = 4, stock = 0, message= "stock lost -> purchase")
        then(delta = 0, stockExcess = 0)
    }

    @Test
    fun `sanity check we get lots of stuff`() {
        whenBoringMessage(total = 2, stock = 0, message= "shop order + 2 no stock yet")
        then(delta = 2)

        whenBoringMessage(total = 2, stock = 2, message= "stock went up")
        then(delta = 0, stockExcess = 2)

        whenBoringMessage(total = 15, stock = 10, message= "shop order +13")
        then(delta = 5, stockExcess = 2)

        whenBoringMessage(total = 15, stock = 15, message= "stop up again")
        then(delta = 0, stockExcess = 7)
    }

    @Test
    fun `stock lost and isn't enough anymore`() {
        whenBoringMessage(total = 2, stock = 2, message= "enough stock")
        then(delta = 0)

        whenBoringMessage(total = 2, stock = 1, message= "oops, one lost")
        then(delta = 1)
    }

    @Test
    fun `stock lost and even more shortage`() {
        whenBoringMessage(total = 5, stock = 3, message= "2 missing")
        then(delta = 2)

        whenBoringMessage(total = 5, stock = 1, message= "2 more missing")
        then(delta = 2)
    }

    @Test
    fun `should order new orders and decreased amount`() {
        whenBoringMessage(total = 2, stock = 1, message= "one cdpa")
        then(delta = 1)

        whenBoringMessage(total = 3, stock = 0, message= "one more shop order and stock lost")
        then(delta = 2)
    }

    @Test
    fun `stock lost not affecting previous shortage but does the new one`() {
        whenBoringMessage(total = 2, stock = 2, message= "no shortage")
        then(delta = 0)

        whenBoringMessage(total = 5, stock = 3, message= "shortage")
        then(delta = 2)
    }

    @Test
    fun `decrease supply over the demand doesn't affect bottomline`() {
        whenBoringMessage(total = 2, stock = 2, message= "no shortage")
        then(delta = 0)

        whenBoringMessage(total = 2, stock = 2, message= "still no shortage")
        then(delta = 0)
    }

    @Test
    fun `stock found not affecting previous shortage but does the new one`() {
        whenBoringMessage(total = 2, stock = 2, message= "no shortage")
        then(delta = 0)

        whenBoringMessage(total = 5, stock = 4, message= "shortage")
        then(delta = 1)
    }

    @Test
    fun `same stock bit more requested`() {
        whenBoringMessage(total = 2, stock = 2, message= "no shortage")
        then(delta = 0)

        whenBoringMessage(total = 5, stock = 3, message= "shortage")
        then(delta = 2)
    }

    @Test
    fun `same stock still no shortage`() {
        whenBoringMessage(total = 2, stock = 2, message= "no shortage")
        then(delta = 0)

        whenBoringMessage(total = 5, stock = 5, message= "no shortage")
        then(delta = 0)
    }

    @Test
    fun `same stock low`() {
        whenBoringMessage(total = 2, stock = 1, message= "shortage 1")
        then(delta = 1)

        whenBoringMessage(total = 5, stock = 1, message= "even more shortage")
        then(delta = 3)
    }

    @Test
    fun `growing stock low`() {

        whenBoringMessage(total = 3, stock = 1, message= "shortage")
        then(delta = 2, purchased = 2)

        whenBoringMessage(total = 5, stock = 2, message= "even more shortage")
        then(delta = 2, stockExcess = 1, purchased = 4)
    }

    @Test
    fun `stop pops boring first delayed noticed xD`() {

        whenBoringMessage(total = 1, stock = 0, message= "customer order +1 -> cdpa")
        then(delta = 1)

        whenBoringMessage(total = 1, stock = 1, message= "our stuff has been delivered but we aren't yet aware of that")
        then(delta = 0, stockExcess = 1)

        whenBoringMessage(total = 1, shipped = 0, message= "shipped")
        then(delta = 0, stockExcess = 0)

        whenBoringMessage(total = 1, shipped = 1, message= "now we've noticed")
        then(delta = 0, stockExcess = 0)

        whenBoringMessage(total = 2, shipped = 1, message= "order +1")
        then(delta = 1, stockExcess = 0)
    }

    @Test
    fun `stop pops boring first delayed noticed xD 2`() {

        whenBoringMessage(total = 1, stock = 0, message= "customer order +1 -> cdpa")
        then(delta = 1)

        whenBoringMessage(total = 2, stock = 1, message= "new order covered by stock")
        then(delta = 1, stockExcess = 1)

        whenBoringMessage(total = 2, shipped = 1, message= "now we've noticed")
        then(delta = 0, stockExcess = 0)
    }


    @Test
    fun `should not think overshipped when notified bit late at xD`() {

        whenBoringMessage(total = 1, stock = 0, message= "customer order +1 -> cdpa")
        then(delta = 1)

        whenBoringMessage(total = 2, stock = 1, message= "our first piece has been delivered but we aren't yet aware of that")
        then(delta = 1, stockExcess = 1)

        whenBoringMessage(total = 2, shipped = 1, message= "now we've noticed")
        then(delta = 0, stockExcess = 0)
    }

    // XLS

    @Test
    fun `Create create ship`() {

        //TODO: look at this carefully.
        //also what happens on consequent increases, will stock excess play the correct role or not?

        whenBoringMessage(total = 7, stock = 2, message= "New customer order 7")
        then(delta = 5)

        whenBoringMessage(total = 10, stock = 6, message= "customer order +3 stock +4 via our fulfillment")
        then(delta = 3, stockExcess = 4, purchased = 8)

        whenBoringMessage(total = 10, stock = 2, shipped = 5, message= "Shipment")
        then(delta = 0, stockExcess = 2, purchased = 5)

        whenBoringMessage(total = 10, stock = 0, shipped = 10, message= "final shipment")
        then(delta = 0, stockExcess = 0, purchased = 0)
    }

    @Test
    fun `Create cancel ship`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order 7")
        then(delta = 5)

        whenBoringMessage(total = 7, stock = 2, cancelled = 3, message= "Cancel 3 of customer order")
        then(delta = -3, stockExcess = 0)

        whenBoringMessage(total = 7, stock = 0, cancelled = 3, shipped = 4, message= "Shipment")
        then(delta = 0)
    }

    @Test
    fun `Create found_stock create ship`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order 7")
        then(delta = 5, purchased = 5)

        whenBoringMessage(total = 10, stock = 6, message= "+3 customer order")
        then(delta = 3, stockExcess = 4, purchased = 8)

        whenBoringMessage(total = 10, stock = 0, shipped = 10, message= "Shipment")
        then(delta = 0)
    }

    @Test
    fun `Create shipment timing_issue`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order 7")
        then(delta = 5)

        whenBoringMessage(total = 7, stock = 2, shipped = 2, message= "shipment bypassing stock")
        then(delta = 0, stockExcess = 2)

        whenBoringMessage(total = 10, stock = 0, shipped = 4, message= "customer order + 3 and stock shipped")
        then(delta = 3)

        whenBoringMessage(total = 10, stock = 0, shipped = 10, message= "final Shipment")
        then(delta = 0)
    }

    @Test
    fun `Received goods not registered yet`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order 7")
        then(delta = 5)

        whenBoringMessage(total = 10, stock = 6, message= "New customer order with the stock +4 unrelated to xD")
        then(delta = 3, stockExcess = 4, purchased = 8)

        whenBoringMessage(total = 10, stock = 1, shipped = 5, message= "shipment + noticed we've delivered that thing")
        then(delta = 0, stockExcess = 1, purchased = 5)

        whenBoringMessage(total = 10, stock = 0, shipped = 10, message= "final Shipment")
        then(delta = 0, stockExcess = 0, purchased = 0)
    }

    @Test
    fun `Stock decreased`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order 7")
        then(delta = 5)

        whenBoringMessage(total = 10, stock = 0, message= "customer order +3 stock fluctuation -2")
        then(delta = 5)

        whenBoringMessage(total = 10, stock = 0, shipped = 10, message= "final shipment")
        then(delta = 0)
    }

    @Test
    fun `Stock coverage grows`() {

        whenBoringMessage(total = 1, stock = 1, message= "New customer order covered by stock")
        then(delta = 0)

        whenBoringMessage(total = 2, stock = 2, message= "ditto")
        then(delta = 0)

        whenBoringMessage(total = 2, stock = 0, message= "all the stock is lost")
        then(delta = 2)
    }

    @Test
    fun `Stock coverage decreases upon shipment and cancellation`() {

        whenBoringMessage(total = 2, stock = 2, message= "New customer order covered by stock")
        then(delta = 0)

        whenBoringMessage(total = 2, stock = 0, shipped = 1, cancelled = 1, message= "all is fulfilled")
        then(delta = 0)
    }

    @Test
    fun `Stock coverage decreases upon shipment`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order covered by stock")
        then(delta = 5, stockExcess = 0)

        whenBoringMessage(total = 7, stock = 2, shipped = 1, message= "shipped")
        then(delta = 0, stockExcess = 1)

        whenBoringMessage(total = 7, stock = 1, shipped = 1, message= "shipped effect on stock")
        then(delta = 0, stockExcess = 0)

        whenBoringMessage(total = 7, stock = 0, shipped = 1, message= "stock is lost")
        then(delta = 1, stockExcess = 0)

        whenBoringMessage(total = 7, stock = 1, shipped = 1, message= "and then found")
        then(delta = 0, stockExcess = 1)
    }

    @Test
    fun `Stock coverage stays intact upon cancellation 2`() {

        whenBoringMessage(total = 2, stock = 2, message= "New customer order covered by stock")
        then(delta = 0)

        whenBoringMessage(total = 2, stock = 1, cancelled = 1, message= "cancelled")
        then(delta = 0)

        whenBoringMessage(total = 2, stock = 0, cancelled = 1, message= "decrease")
        then(delta = 1)

    }

    @Test
    fun `Don't cancel if not purchased`() {

        whenBoringMessage(total = 1, stock = 1, message= "customer order from stock")
        then(delta = 0)

        whenBoringMessage(total = 1, stock = 0, cancelled = 1, message= "cancelled")
        then(delta = 0)
    }

    @Test
    fun `shipped`() {

        whenBoringMessage(total = 1, stock = 1, message= "customer order from stock")
        then(delta = 0)

        whenBoringMessage(total = 1, stock = 0, shipped = 1, message= "shipped")
        then(delta = 0)
    }

    @Test
    fun `Stock coverage decreases upon shipment 2`() {

        whenBoringMessage(total = 2, stock = 2, message= "New customer order covered by stock")
        then(delta = 0)

        whenBoringMessage(total = 2, stock = 1, shipped = 1, message= "shipped")
        then(delta = 0)

        whenBoringMessage(total = 2, stock = 0, shipped = 1, message= "stock is lost")
        then(delta = 1)

        whenBoringMessage(total = 2, stock = 1, shipped = 1, message= "and then found")
        then(delta = 0, stockExcess = 1)
    }

    @Test
    fun `Stock coverage stays intact upon cancellation 3`() {

        whenBoringMessage(total = 2, stock = 1, message= "New customer order covered by stock")
        then(delta = 1, stockExcess = 0)

        whenBoringMessage(total = 2, stock = 1, cancelled = 1, message= "cancelled")
        then(delta = -1, stockExcess = 0)

        whenBoringMessage(total = 2, stock = 0, cancelled = 1, message= "decrease")
        then(delta = 1, stockExcess = 0)
    }

    @Test
    fun `Stock coverage stays intact upon cancellation`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order covered by stock")
        then(delta = 5, stockExcess = 0)

        whenBoringMessage(total = 7, stock = 2, cancelled = 1, message= "cancelled")
        then(delta = -1, stockExcess = 0)

        whenBoringMessage(total = 7, stock = 1, cancelled = 1, message= "stock decrease")
        then(delta = 1, stockExcess = 0)

    }

    @Test
    fun `stock increase followed by decrease`() {

        whenBoringMessage(total = 1, stock = 0, message= "New customer order")
        then(delta = 1, stockExcess = 0)

        whenBoringMessage(total = 1, stock = 1, message= "some stock")
        then(delta = 0, stockExcess = 1)

        whenBoringMessage(total = 1, stock = 0, message= "stock lost again")
        then(delta = 0, stockExcess = 0)
    }

    @Test
    fun `shipment with stock increase`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order")
        then(delta = 5, stockExcess = 0)

        whenBoringMessage(total = 7, stock = 2, shipped = 1, message= "shipped no stock changes")
        then(delta = 0, stockExcess = 1)

        whenBoringMessage(total = 7, stock = 1, shipped = 1, message= "delayed decrease ignored")
        then(delta = 0, stockExcess = 0)

        whenBoringMessage(total = 7, stock = 0, shipped = 1, message= "further decrease is indeed stock lost")
        then(delta = 1, stockExcess = 0)
    }

    @Test
    fun `customer order with stock increase`() {

        whenBoringMessage(total = 2, stock = 2, message= "customer order")
        then(delta = 0, stockExcess = 0)

        whenBoringMessage(total = 3, stock = 3, message= "+1 order covered")
        then(delta = 0, stockExcess = 0)

        whenBoringMessage(total = 3, stock = 0, message= "stock decrease")
        then(delta = 3, stockExcess = 0)
    }

    @Test
    fun `stock decrease before shipment issues delta still`() {

        whenBoringMessage(total = 7, stock = 2, message= "New customer order")
        then(delta = 5, stockExcess = 0, purchased = 5)

        whenBoringMessage(total = 7, stock = 1, shipped = 0, message= "stock decrease just before shipment")
        then(delta = 1, stockExcess = 0, purchased = 6)

        whenBoringMessage(total = 7, stock = 1, shipped = 1, message= "delayed shipment")
        then(delta = 0, stockExcess = 1, purchased = 6)

        whenBoringMessage(total = 7, stock = 0, shipped = 1, message= "stock lost already accounted earlier")
        then(delta = 0, stockExcess = 0, purchased = 6)
    }

    //TODO: decreased stock lost purchases should also reduce the stock excess

    val xd = XdTake4LostStockCapCellStatuses()

    var boringUpdate: BoringTotals? = null
    var stepName = ""
    var totalPurchased = 0

    fun whenBoringMessage(
            total: Int = 0,
            stock: Int = 0,
            shipped: Int = 0,
            cancelled: Int = 0,
            message: String = "") {

        require(total >= shipped + cancelled + stock)
        
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

    fun then(
        delta: Int,
        stockExcess: Int = 0,
        purchased: Int? = null
    ) {
        val update = boringUpdate ?: throw IllegalStateException("no boring update yet")

        assertThat(delta).`as`("no point of ever ordering more than missing, right?").isLessThanOrEqualTo(update.shortage())

        val lastDelta = xd.receive(update, stepName)
        totalPurchased += lastDelta
        //assertThat(totalPurchased).`as`("Should not order more than total orders").isLessThanOrEqualTo(update.total)
        boringUpdate = null

        assertSoftly {softly ->

            softly.assertThat(lastDelta).`as`(stepName + " – delta").isEqualTo(delta)

            val lastStep = xd.steps.last()

            softly.assertThat(lastStep.stockExcess).`as`(stepName + " – stock excess").isEqualTo(stockExcess)

            if (purchased != null) {
                softly.assertThat(lastStep.totalPurchased).`as`(stepName + " – purchased").isEqualTo(purchased)
            }
        }


    }

}