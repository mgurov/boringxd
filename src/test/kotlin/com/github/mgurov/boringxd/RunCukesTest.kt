package com.github.mgurov.boringxd

import cucumber.api.CucumberOptions
import cucumber.api.junit.Cucumber
import org.junit.runner.RunWith


@RunWith(Cucumber::class)
@CucumberOptions(plugin = arrayOf("pretty", "html:target/cucumber"))
class RunCukesTest