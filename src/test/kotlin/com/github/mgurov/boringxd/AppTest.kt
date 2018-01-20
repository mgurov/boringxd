package com.github.mgurov.boringxd

import junit.framework.Test
import junit.framework.TestCase
import junit.framework.TestSuite

/**
 * Unit test for simple App.
 */
class AppTest
/**
 * Create the test case
 *
 * @param testName name of the test case
 */
(testName: String) : TestCase(testName) {

    /**
     * Rigourous Test :-)
     */
    fun testApp() {
        TestCase.assertTrue(true)
    }

    companion object {

        /**
         * @return the suite of tests being tested
         */
        fun suite(): Test {
            return TestSuite(AppTest::class.java)
        }
    }
}
