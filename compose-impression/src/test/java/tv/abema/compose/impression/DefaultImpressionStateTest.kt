package tv.abema.compose.impression

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
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
    impressionTest(
      impressionStateFactory = { coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
        currentTimeProducer: () -> Long
        ->
        createDefaultImpressionState(coroutineLauncher, currentTimeProducer)
      }
    ) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)

      advanceTimeBy(500)

      impressionState.visibleItems
        .shouldBe(mapOf(key to DefaultImpressionState.VisibleItem(key, 0)))
      impressionState.alreadySentItems shouldBe emptyMap()
    }
  }

  @Test
  fun impressed() {
    impressionTest(
      impressionStateFactory = { coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
        currentTimeProducer: () -> Long
        ->
        createDefaultImpressionState(coroutineLauncher, currentTimeProducer)
      }
    ) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)

      advanceTimeBy(1000)

      impressionState.visibleItems
        .shouldBe(mapOf(key to DefaultImpressionState.VisibleItem(key, 0)))
      impressionState.alreadySentItems
        .shouldBe(mapOf(key to DefaultImpressionState.Impression(key, 0)))
    }
  }

  @Test
  fun notImpressWhenOut() {
    impressionTest(
      impressionStateFactory = { coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
        currentTimeProducer: () -> Long
        ->
        createDefaultImpressionState(coroutineLauncher, currentTimeProducer)
      }
    ) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)

      advanceTimeBy(500)
      val outRect = Rect(0F, 0F, 3F, 3F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), outRect, outRect)
      advanceTimeBy(500)

      impressionState.visibleItems shouldBe emptyMap()
      impressionState.alreadySentItems shouldBe emptyMap()
    }
  }

  @Test
  fun notImpressWhenDispose() {
    impressionTest(
      impressionStateFactory = { coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
        currentTimeProducer: () -> Long
        ->
        createDefaultImpressionState(coroutineLauncher, currentTimeProducer)
      }
    ) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)

      advanceTimeBy(500)
      impressionState.onDispose(key)
      advanceTimeBy(500)

      impressionState.visibleItems shouldBe emptyMap()
      impressionState.alreadySentItems shouldBe emptyMap()
    }
  }

  @Test
  fun multipleImpress() {
    impressionTest(
      impressionStateFactory = { coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
        currentTimeProducer: () -> Long
        ->
        createDefaultImpressionState(coroutineLauncher, currentTimeProducer)
      }
    ) { impressionState ->
      val key1 = "impression key1"
      val key2 = "impression key2"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key1, IntSize(10, 10), rect, rect)
      impressionState.onLayoutCoordinatesChange(key2, IntSize(10, 10), rect, rect)

      advanceTimeBy(1000)

      impressionState.visibleItems shouldBe mapOf(
        "impression key1" to DefaultImpressionState.VisibleItem(key = key1, startTime = 0),
        "impression key2" to DefaultImpressionState.VisibleItem(key = key2, startTime = 0)
      )
      impressionState.alreadySentItems
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
    impressionTest(
      impressionStateFactory = { coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
        currentTimeProducer: () -> Long
        ->
        createDefaultImpressionState(coroutineLauncher, currentTimeProducer)
      }
    ) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)
      val currentLoopCount = impressionState.currentLoopCount

      advanceTimeBy(1000)

      impressionState.visibleItems
        .shouldBe(mapOf(key to DefaultImpressionState.VisibleItem(key, 0)))
      impressionState.alreadySentItems[key]?.impressionLoopCount shouldBe currentLoopCount + 1
    }
  }

  @Test
  fun notImpressedTwice() {
    impressionTest(
      impressionStateFactory = { coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
        currentTimeProducer: () -> Long
        ->
        createDefaultImpressionState(coroutineLauncher, currentTimeProducer)
      }
    ) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)
      advanceTimeBy(1000)

      val outRect = Rect(0F, 0F, 0F, 0F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), outRect, outRect)
      advanceTimeBy(2000)

      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)
      advanceTimeBy(2000)

      impressionState.visibleItems
        .shouldBe(mapOf(key to DefaultImpressionState.VisibleItem(key, 0)))
      impressionState.alreadySentItems
        .shouldBe(mapOf(key to DefaultImpressionState.Impression(key, 0)))
    }
  }

  @Test
  fun impressedAndClear() {
    impressionTest(
      impressionStateFactory = { coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
        currentTimeProducer: () -> Long
        ->
        createDefaultImpressionState(coroutineLauncher, currentTimeProducer)
      }
    ) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)
      advanceTimeBy(1000)
      impressionState.visibleItems
        .shouldBe(mapOf(key to DefaultImpressionState.VisibleItem(key, 0)))
      impressionState.alreadySentItems
        .shouldBe(mapOf(key to DefaultImpressionState.Impression(key, 0)))
      impressionState.clearSentItems()
      impressionState.setCurrentTimeToVisibleItemStartTime()

      // found item in next loop and impress in the next loop
      advanceTimeBy(2000)

      impressionState.visibleItems
        .shouldBe(mapOf(key to DefaultImpressionState.VisibleItem(key, 1000)))
      impressionState.alreadySentItems
        .shouldBe(mapOf(key to DefaultImpressionState.Impression(key, 2)))
    }
  }

  @Test
  fun impressedImmediateWhenImpressionDurationIsZero() {
    impressionTest(
      impressionStateFactory = { coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
        currentTimeProducer: () -> Long
        ->
        createDefaultImpressionState(
          coroutineLauncher = coroutineLauncher,
          currentTimeProducer = currentTimeProducer,
          impressionDuration = 0L
        )
      }
    ) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)

      advanceTimeBy(1)

      impressionState.visibleItems
        .shouldBe(mapOf(key to DefaultImpressionState.VisibleItem(key, 0)))
      impressionState.alreadySentItems
        .shouldBe(mapOf(key to DefaultImpressionState.Impression(key, 0)))
    }
  }

  @Test
  fun impressedCompleteVisible() {
    impressionTest(
      impressionStateFactory = { coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
        currentTimeProducer: () -> Long
        ->
        createDefaultImpressionState(
          coroutineLauncher = coroutineLauncher,
          currentTimeProducer = currentTimeProducer,
          visibleRatio = 1F
        )
      }
    ) { impressionState ->
      val key = "impression key"

      val rect = Rect(0F, 0F, 10F, 10F)
      impressionState.onLayoutCoordinatesChange(key, IntSize(10, 10), rect, rect)

      advanceTimeBy(1000)

      impressionState.visibleItems
        .shouldBe(mapOf(key to DefaultImpressionState.VisibleItem(key, 0)))
      impressionState.alreadySentItems
        .shouldBe(mapOf(key to DefaultImpressionState.Impression(key, 0)))
    }
  }

  private fun createDefaultImpressionState(
    coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
    currentTimeProducer: () -> Long,
    impressionDuration: Long = 1000,
    visibleRatio: Float = 0.5F,
  ) = DefaultImpressionState(
    coroutinesLauncher = coroutineLauncher,
    impressionDurationMs = impressionDuration,
    checkIntervalMs = 1000,
    visibleRatio = visibleRatio,
    currentTimeProducer = currentTimeProducer
  )

  class ImpressionTestScope(
    private val testCoroutineScope: TestCoroutineScope,
    private val currentTimeProducer: CurrentTimeProducer,
  ) {
    fun advanceTimeBy(time: Long) {
      currentTimeProducer.currentTime = time
      testCoroutineScope.advanceTimeBy(time)
    }
  }

  fun impressionTest(
    impressionStateFactory: (
      coroutineLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
      currentTimeProducer: () -> Long
    ) -> DefaultImpressionState,
    block: ImpressionTestScope.(impressionState: DefaultImpressionState) -> Unit
  ) {
    runBlockingTest(
      // https://github.com/Kotlin/kotlinx.coroutines/issues/1910
      Job()
    ) {
      val dispatcher = coroutineContext[CoroutineDispatcher.Key] as CoroutineDispatcher
      Dispatchers.setMain(dispatcher)

      pauseDispatcher()
      val currentTimeProducer = CurrentTimeProducer()
      var launchedJob: Job? = null
      val coroutinesLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit =
        { block -> launchedJob = launch(block = block) }
      val impressionState =
        impressionStateFactory(coroutinesLauncher, currentTimeProducer)

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
