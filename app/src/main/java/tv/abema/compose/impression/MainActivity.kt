package tv.abema.compose.impression

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tv.abema.compose.impression.ui.theme.ComposeimpressionTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(FrameLayout(this).apply {
      layoutParams = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      ).apply {
        (this as ViewGroup.MarginLayoutParams).topMargin = 1000
      }
      val composeView = ComposeView(this@MainActivity)
      composeView.setContent {
        ComposeimpressionTheme {
          // A surface container using the 'background' color from the theme
          Surface(color = MaterialTheme.colors.background) {
            List()
          }
        }
      }
      addView(composeView)
    })
  }
}

@Composable
fun List() {
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
  LazyColumn(Modifier.padding(bottom = 30.dp)) {
    itemsIndexed((0..1000).toList()) { index, item ->
      // for recompose (this is not needed for production app)
      impressionState.impressFlow.collectAsState(initial = "").value
      val isImpressed = impressionState.impressedItem.contains(item)
      Text(
        "$index isImpressed:$isImpressed",
        Modifier.
        impression(
          key = item,
          impressionState = impressionState,
          impression = { key ->
            println("impressed:$key")
          })
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
  ComposeimpressionTheme {
    List()
  }
}
