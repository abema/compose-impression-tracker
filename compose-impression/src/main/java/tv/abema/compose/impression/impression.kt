package tv.abema.compose.impression

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun <T : Any> Modifier.impression(
  key: T,
  onImpression: (key: T) -> Unit
) = composed {
  impression(
    key = key,
    impressionState = LocalImpressionState.current ?: run {
      rememberImpressionState()
    },
    onImpression = onImpression
  )
}

fun <T : Any> Modifier.impression(
  key: T,
  impressionState: ImpressionState,
  onImpression: (key: T) -> Unit
): Modifier = composed(
  inspectorInfo = {
    properties["key"] = key
  }
) {
  val view = LocalView.current
  LaunchedEffect(key1 = key) {
    impressionState.impressFlow.collect {
      if (key == it) {
        onImpression(key)
      }
    }
  }
  DisposableEffect(key1 = key) {
    onDispose {
      impressionState.onDispose(key)
    }
  }
  onGloballyPositioned { coordinate: LayoutCoordinates ->
    val boundsInWindow = coordinate.boundsInWindow()
    val globalRootRect = android.graphics.Rect()
    view.getGlobalVisibleRect(globalRootRect)
    impressionState.onLayoutCoordinatesChange(
      key,
      coordinate.size,
      boundsInWindow,
      globalRootRect.toComposeRect()
    )
  }
}

val LocalImpressionState = compositionLocalOf<ImpressionState?> { null }

@Composable
fun ProvideImpressionState(value: ImpressionState, content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalImpressionState provides value, content = content)
}

@Composable
fun rememberImpressionState(): ImpressionState {
  val lifecycleOwner = LocalLifecycleOwner.current
  return remember { DefaultImpressionState(lifecycleOwner.lifecycle) }
}

@Composable
fun rememberDefaultImpressionState(): DefaultImpressionState {
  val lifecycleOwner = LocalLifecycleOwner.current
  return remember { DefaultImpressionState(lifecycleOwner.lifecycle) }
}

class DefaultImpressionState(
  coroutinesLauncher: (block: suspend CoroutineScope.() -> Unit) -> Unit,
  private val impressionDurationMs: Long = DEFAULT_IMPRESSION_DURATION_MS,
  private val checkIntervalMs: Long = DEFAULT_CHECK_INTERVAL_MS,
  private val visibleRatio: Float = DEFAULT_VISIBLE_RATIO,
  private val currentTimeProducer: () -> Long = { System.currentTimeMillis() }
) : ImpressionState {
  constructor(
    lifecycle: Lifecycle,
    impressionDuration: Long = DEFAULT_IMPRESSION_DURATION_MS,
    checkInterval: Long = DEFAULT_CHECK_INTERVAL_MS,
    visibleRatio: Float = DEFAULT_VISIBLE_RATIO,
    currentTimeProducer: () -> Long = { System.currentTimeMillis() }
  ) : this(
    coroutinesLauncher = { block ->
      lifecycle.coroutineScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
          block()
        }
      }
    },
    impressionDurationMs = impressionDuration,
    checkIntervalMs = checkInterval,
    visibleRatio = visibleRatio,
    currentTimeProducer = currentTimeProducer
  )

  private val mutableSharedFlow = MutableSharedFlow<Any>()
  override val impressFlow: SharedFlow<Any> = mutableSharedFlow.asSharedFlow()

  private val mutableVisibleItems: MutableMap<Any, VisibleItem> = mutableMapOf()
  val visibleItems: Map<Any, VisibleItem> get() = mutableVisibleItems.toMap()

  private val mutableAlreadySentItems: MutableMap<Any, Impression> = mutableMapOf()
  val alreadySentItems: Map<Any, Impression> get() = mutableAlreadySentItems.toMap()

  var currentLoopCount = -1L
    private set

  data class VisibleItem(val key: Any, val startTime: Long)
  data class Impression(val key: Any, val impressionLoopCount: Long)

  init {
    coroutinesLauncher {
      while (true) {
        currentLoopCount++
        val time = currentTimeProducer()
        val impressions = mutableVisibleItems.values.toList().filter {
          it.startTime <= time - impressionDurationMs && !alreadySentItems.containsKey(it.key)
        }
        impressions.forEach { impression ->
          mutableAlreadySentItems[impression.key] = Impression(impression.key, currentLoopCount)
          mutableSharedFlow.emit(impression.key)
        }
        delay(checkIntervalMs)
      }
    }
  }

  override fun onLayoutCoordinatesChange(
    key: Any,
    size: IntSize,
    boundsRect: Rect,
    composeViewRect: Rect
  ) {
    if (mutableAlreadySentItems.contains(key)) {
      return
    }

    val componentArea = size.width * size.height
    val top = maxOf(boundsRect.top, composeViewRect.top)
    val bottom = minOf(boundsRect.bottom, composeViewRect.bottom)
    val visibleHeight = bottom - top
    if (visibleHeight < 0) return onDispose(key)

    val left = maxOf(boundsRect.left, composeViewRect.left)
    val right = minOf(boundsRect.right, composeViewRect.right)
    val visibleWidth = right - left
    if (visibleWidth < 0) return onDispose(key)

    val visibleArea = visibleWidth * visibleHeight

    if (visibleArea / componentArea >= visibleRatio) {
      mutableVisibleItems.getOrPut(key) {
        VisibleItem(key, currentTimeProducer())
      }
    } else {
      onDispose(key)
    }
  }

  fun clearSentItems() {
    mutableAlreadySentItems.clear()
  }

  fun setCurrentTimeToVisibleItemStartTime() {
    val currentTimeMs = currentTimeProducer()
    mutableVisibleItems.toMap().forEach { (key, value) ->
      mutableVisibleItems[key] = value.copy(startTime = currentTimeMs)
    }
  }

  override fun onDispose(key: Any) {
    mutableVisibleItems.remove(key)
  }

  companion object {
    const val DEFAULT_IMPRESSION_DURATION_MS: Long = 1000L
    const val DEFAULT_CHECK_INTERVAL_MS: Long = 1000L
    const val DEFAULT_VISIBLE_RATIO: Float = 0.5F
  }
}

interface ImpressionState {
  val impressFlow: SharedFlow<Any>

  fun onLayoutCoordinatesChange(
    key: Any,
    size: IntSize,
    boundsRect: Rect,
    composeViewRect: Rect
  )

  fun onDispose(key: Any)
}
