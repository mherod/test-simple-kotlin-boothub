/*
 * Copyright (c) 2018 the original author or authors
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */
package co.herod.testsimplekotlinboothub
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.ShouldSpec

class TestSimpleKotlinBoothubTest : ShouldSpec() {
    init {
        should("correctly compute 1 + 1") {
            1 + 1 shouldBe 2
        }
    }
}
