package com.bendingspoons.pico

import com.bendingspoons.pico.model.PicoEvent
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PicoFlowTest {

    @MockK
    lateinit var store: PicoStore

    @MockK
    lateinit var event: PicoEvent

    @Before
    fun setUp() = MockKAnnotations.init(this, relaxUnitFun = true) // turn relaxUnitFun on for all mocks

    @Test
    fun `if store is not empty flow emits next events batch`() = runBlockingTest {

        coEvery { store.nextEventsBatch() } returns listOf(event)
        val flow = PicoFlow(store, 1)
        val emittedEvents = flow.flow().toList()
        assertTrue(emittedEvents[0].size == 1)
    }

    @Test
    fun `flow emits all events available in the store`() = runBlockingTest {

        coEvery { store.nextEventsBatch() } returns listOf(event, event, event, event)
        val flow = PicoFlow(store, 1)
        val emittedEvents = flow.flow().toList()
        assertTrue(emittedEvents[0].size == 4)
    }

    @Test
    fun `if store is empty flow emits nothing`() = runBlockingTest {

        coEvery { store.nextEventsBatch() } returns listOf()
        val flow = PicoFlow(store, 1)
        val emittedEvents = flow.flow().toList()
        assertTrue(emittedEvents.isEmpty())
    }

    @Test
    // IMPORTANT: don't use runBlockingTest (that remove all delays, timeouts)
    // because this test relies on the fact the timeout is skipped by the flush event.
    fun `we can flush immediately the flow emission`() = runBlocking {

        coEvery { store.nextEventsBatch() } returns listOf(event)
        val flow = PicoFlow(store, 1)

        var emittedEvents = listOf<List<PicoEvent>>()

        val flowDeferred = async {
            emittedEvents = flow.flow().toList()
        }

        flow.flush() // if you remove this, the test will takes about 6/7 seconds to run instead

        flowDeferred.await()

        assertTrue(emittedEvents[0].size == 1)
    }

    @Test
    fun `after the flush the completable deferred is reset`() = runBlocking {

        coEvery { store.nextEventsBatch() } returns listOf(event)
        val flow = PicoFlow(store, 1)

        val deferredHash = flow.future.hashCode()

        val flowDeferred = async {
            flow.flow().toList()
        }

        flow.flush() // if you remove this, the test will takes about 6/7 seconds to run instead

        flowDeferred.await()

        val newDeferredHash = flow.future.hashCode()

        assertNotEquals(deferredHash, newDeferredHash)
    }
}