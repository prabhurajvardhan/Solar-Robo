package com.solarrobo.feature.auth.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    @Test
    fun createsUniqueSaltAndVerifiesOnlyMatchingPin() {
        val hasher = PinHasher()
        val first = hasher.create("123456")
        val second = hasher.create("123456")
        assertNotEquals(first.saltBase64, second.saltBase64)
        assertTrue(hasher.verify("123456", first.saltBase64, first.hashBase64))
        assertFalse(hasher.verify("654321", first.saltBase64, first.hashBase64))
    }

    @Test
    fun usesRequiredStrength() {
        assertTrue(PinHasher.ITERATIONS >= 120_000)
        assertTrue(PinHasher.OUTPUT_BYTES * 8 >= 256)
    }
}
