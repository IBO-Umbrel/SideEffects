package com.lessons.sideeffects

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeImageRepositoryTest {
    private val repository = FakeImageRepository()

    @Test fun pngLoadsSuccessfully() = runBlocking {
        assertEquals(ImageResult.Success("avatar.png"), repository.load("avatar.png"))
    }

    @Test fun otherExtensionsReturnError() = runBlocking {
        assertTrue(repository.load("avatar.jpg") is ImageResult.Error)
    }

    @Test(expected = TimeoutCancellationException::class)
    fun loadingCooperatesWithCancellation(): Unit = runBlocking {
        withTimeout(50) { repository.load("avatar.png") }
        Unit
    }
}
