package tv.abema.compose.impression

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import io.kotest.assertions.failure
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
class DefaultImpressionStateTest {
  class CurrentTimeProducer : () -> Long {
    var currentTime = 0L
    override fun invoke(): Long {
      return currentTime
    }
  }

  @Test
  fun impressing() {
    impressionTest(impressionStateFactory = { lifecycle: Lifecycle,
      coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
      currentTimeProducer: () -> Long
      ->
      createDefaultImpressionState(lifecycle, coroutineLauncher, currentTimeProducer)
    }) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)

      advanceTimeBy(500)

      impressionState.impressingItem
        .shouldBe(mapOf(key to DefaultImpressionState.ImpressingItem(key, 0)))
      impressionState.impressedItem shouldBe emptyMap()
    }
  }

  @Test
  fun impressed() {
    impressionTest(impressionStateFactory = { lifecycle: Lifecycle,
      coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
      currentTimeProducer: () -> Long
      ->
      createDefaultImpressionState(lifecycle, coroutineLauncher, currentTimeProducer)
    }) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)

      advanceTimeBy(1000)

      impressionState.impressingItem shouldBe emptyMap()
      impressionState.impressedItem
        .shouldBe(mapOf(key to DefaultImpressionState.Impression(key, 0)))
    }
  }

  @Test
  fun notImpressWhenOut() {
    impressionTest(impressionStateFactory = { lifecycle: Lifecycle,
      coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
      currentTimeProducer: () -> Long
      ->
      createDefaultImpressionState(lifecycle, coroutineLauncher, currentTimeProducer)
    }) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)

      advanceTimeBy(500)
      val outRect = Rect(0F, 0F, 3F, 3F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), outRect, outRect)
      advanceTimeBy(500)

      impressionState.impressingItem shouldBe emptyMap()
      impressionState.impressedItem shouldBe emptyMap()
    }
  }

  @Test
  fun notImpressWhenDispose() {
    impressionTest(impressionStateFactory = { lifecycle: Lifecycle,
      coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
      currentTimeProducer: () -> Long
      ->
      createDefaultImpressionState(lifecycle, coroutineLauncher, currentTimeProducer)
    }) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)

      advanceTimeBy(500)
      impressionState.onDispose(key)
      advanceTimeBy(500)

      impressionState.impressingItem shouldBe emptyMap()
      impressionState.impressedItem shouldBe emptyMap()
    }
  }

  @Test
  fun multipleImpress() {
    impressionTest(impressionStateFactory = { lifecycle: Lifecycle,
      coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
      currentTimeProducer: () -> Long
      ->
      createDefaultImpressionState(lifecycle, coroutineLauncher, currentTimeProducer)
    }) { impressionState ->
      val key1 = "impression key1"
      val key2 = "impression key2"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key1, IntSize(10, 10), rect, rect)
      impressionState.onLayoutCoordinatesChange(key2, IntSize(10, 10), rect, rect)


      advanceTimeBy(1000)

      impressionState.impressingItem shouldBe emptyMap()
      impressionState.impressedItem
        .shouldBe(
          mapOf(
            key1 to DefaultImpressionState.Impression(key1, 0),
            key2 to DefaultImpressionState.Impression(key2, 0)
          )
        )
    }
  }

  @Test
  fun captureNextImpression() {
    impressionTest(impressionStateFactory = { lifecycle: Lifecycle,
      coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
      currentTimeProducer: () -> Long
      ->
      createDefaultImpressionState(lifecycle, coroutineLauncher, currentTimeProducer)
    }) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)
      val currentLoopCount = impressionState.currentLoopCount

      advanceTimeBy(1000)

      impressionState.impressingItem shouldBe emptyMap()
      impressionState.impressedItem.get(key)?.impressionLoopCount shouldBe currentLoopCount + 1
    }
  }


  @Test
  fun notImpressedTwice() {
    impressionTest(impressionStateFactory = { lifecycle: Lifecycle,
      coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
      currentTimeProducer: () -> Long
      ->
      createDefaultImpressionState(lifecycle, coroutineLauncher, currentTimeProducer)
    }) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)
      advanceTimeBy(1000)

      val outRect = Rect(0F, 0F, 0F, 0F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), outRect, outRect)
      advanceTimeBy(2000)

      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)
      advanceTimeBy(2000)


      impressionState.impressingItem shouldBe emptyMap()
      impressionState.impressedItem
        .shouldBe(mapOf(key to DefaultImpressionState.Impression(key, 0)))
    }
  }

  @Test
  fun impressedAndClear() {
    impressionTest(impressionStateFactory = { lifecycle: Lifecycle,
      coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
      currentTimeProducer: () -> Long
      ->
      createDefaultImpressionState(lifecycle, coroutineLauncher, currentTimeProducer)
    }) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)
      advanceTimeBy(1000)
      impressionState.impressingItem shouldBe emptyMap()
      impressionState.impressedItem
        .shouldBe(mapOf(key to DefaultImpressionState.Impression(key, 0)))
      impressionState.clear()
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)

      // found item in next loop and impress in the next loop
      advanceTimeBy(2000)

      impressionState.impressingItem shouldBe emptyMap()
      impressionState.impressedItem
        .shouldBe(mapOf(key to DefaultImpressionState.Impression(key, 2)))
    }
  }

  @Test
  fun impressedImmediateWhenImpressionDurationIsZero() {
    impressionTest(impressionStateFactory = { lifecycle: Lifecycle,
      coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
      currentTimeProducer: () -> Long
      ->
      createDefaultImpressionState(
        lifecycle = lifecycle,
        coroutineLauncher = coroutineLauncher,
        currentTimeProducer = currentTimeProducer,
        impressionDuration = 0L
      )
    }) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)

      advanceTimeBy(1)

      impressionState.impressingItem shouldBe emptyMap()
      impressionState.impressedItem
        .shouldBe(mapOf(key to DefaultImpressionState.Impression(key, 0)))
    }
  }

  @Test
  fun impressedCompleteVisible() {
    impressionTest(impressionStateFactory = { lifecycle: Lifecycle,
      coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
      currentTimeProducer: () -> Long
      ->
      createDefaultImpressionState(
        lifecycle = lifecycle,
        coroutineLauncher = coroutineLauncher,
        currentTimeProducer = currentTimeProducer,
        visibleRatio = 1F
      )
    }) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)

      advanceTimeBy(1000)

      impressionState.impressingItem shouldBe emptyMap()
      impressionState.impressedItem
        .shouldBe(mapOf(key to DefaultImpressionState.Impression(key, 0)))
    }
  }

  private fun createDefaultImpressionState(
    lifecycle: Lifecycle,
    coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
    currentTimeProducer: () -> Long,
    impressionDuration: Long = 1000,
    visibleRatio: Float = 0.5F,
  ) = DefaultImpressionState(
    lifecycle = lifecycle,
    coroutinesLauncher = coroutineLauncher,
    impressionDuration = impressionDuration,
    checkInterval = 1000,
    visibleRatio = visibleRatio,
    clearLifecycleState = null,
    currentTimeProducer = currentTimeProducer
  )

  class ImpressionTestScope(
    private val testCoroutineScope: TestCoroutineScope,
    private val currentTimeProducer: CurrentTimeProducer
  ) {
    fun advanceTimeBy(time: Long) {
      currentTimeProducer.currentTime = time
      testCoroutineScope.advanceTimeBy(time)
    }
  }

  fun impressionTest(
    impressionStateFactory: (
      lifecycle: Lifecycle,
      coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
      currentTimeProducer: () -> Long
    ) -> DefaultImpressionState,
    block: ImpressionTestScope.(impressionState: DefaultImpressionState) -> Unit
  ) {
    runBlockingTest(
      // https://github.com/Kotlin/kotlinx.coroutines/issues/1910
      Job()
    ) {
      Dispatchers.setMain(coroutineContext[CoroutineDispatcher.Key] as CoroutineDispatcher)

      pauseDispatcher()
      val testLifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
      val currentTimeProducer = CurrentTimeProducer()
      var launchedJob: Job? = null
      val coroutinesLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit =
        { block -> launchedJob = launch(block = block) }
      val lifecycle = testLifecycleOwner.lifecycle
      val impressionState =
        impressionStateFactory(lifecycle, coroutinesLauncher, currentTimeProducer)

      try {
        block(ImpressionTestScope(this, currentTimeProducer), impressionState)
      } catch (e: Exception) {
        e.printStackTrace()
        failure("fail to execute impressionTest", e)
      }

      launchedJob?.cancel()
    }
  }
}