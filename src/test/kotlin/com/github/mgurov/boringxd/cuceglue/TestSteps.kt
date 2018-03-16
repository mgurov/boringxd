package com.github.mgurov.boringxd.cuceglue

import com.github.mgurov.boringxd.BoringTotals
import com.github.mgurov.boringxd.deltas.XdTake2
import cucumber.api.DataTable
import cucumber.api.java8.En
import org.assertj.core.api.Assertions.assertThat

class TestSteps : En {

    val xd = XdTake2()
    var lastDelta: Int? = null

    init {
        When("^boring message is$") { boringMessage: DataTable ->
            lastDelta = xd.receive(boringMessage.asList(BoringTotals::class.java).first(), "")
        }

        Then("^the delta is (\\d+)$") { deltaExpected: Int ->
            assertThat(lastDelta ?: throw IllegalStateException("delta hasn't yet been derived"))
                    .isEqualTo(deltaExpected)
        }

        Then("^∆'s are:$") { boringMessages: DataTable ->

            val rows = boringMessages.asList(Calculus::class.java)
            val actual = rows.map {
                val delta = xd.receive(it.boringDelta, it.description?: "")
                it.copy(delta = delta)
            }
            boringMessages.diff(actual)
        }

    }

}

data class Calculus(
        var total: Int = 0,
        var shipped: Int? = 0,
        var cancelled: Int? = 0,
        var stock: Int? = 0,
        val description: String? = "",
        var delta: Int = 0
) {
    val boringDelta: BoringTotals
        get() = BoringTotals(
                total = total,
                shipped = shipped ?: 0,
                cancelled = cancelled?: 0,
                stock = stock ?: 0
        )
}