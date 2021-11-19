# 🚧🚧🚧WORK IN PROGRESS🚧🚧🚧

# Jetpack Compose Impression Tracker

Track Jetpack Compose component impressions in a simple and flexible way

```kotlin
Text(
  "text",
  Modifier.impression(
    key = item,
    impression = { key ->
      println("impressed:$key")
    })
)
```

![Jetpack Compose Impression Tracker](https://user-images.githubusercontent.com/1386930/141935431-6039b847-74aa-4407-96a4-85b2f1695159.gif)


## Flexible

You can customize `DefaultImpressionState`'s parameters.

```kotlin
val impressionState = remember {
  DefaultImpressionState(
    lifecycle = lifecycle,
    impressionDuration = 1000,
    checkInterval = 1000,
    visibleRatio = 0.5,
    clearLifecycleState = Lifecycle.State.DESTROYED,
  )
}
Text(
  "text",
  Modifier.impression(
    key = item,
    impressionState = impressionState,
    impression = { key ->
      println("impressed:$key")
    })
)
```

You can implement your impression logic.

```kotlin
val impressionState = remember {
  object: ImpressionState {
    override val impressFlow: SharedFlow<Any>
      get() = TODO("write your logic!")

    override fun onLayoutCoordinatesChange(
      key: Any,
      size: IntSize,
      boundsRect: Rect,
      composeViewRect: Rect
    ) {
      TODO("write your logic!")
    }

    override fun onDispose(key: Any) {
      TODO("write your logic!")
    }
  }
}
```

You can share impression state by CompositionLocal

```kotlin
ProvideImpressionState(rememberDefaultImpressionState()) {
  Text(
    "text",
    Modifier.
    impression(
      key = "key",
      impression = { key ->
        println("impressed:$key")
      })
  )
}
```


