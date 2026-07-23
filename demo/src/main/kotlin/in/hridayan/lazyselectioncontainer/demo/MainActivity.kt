package `in`.hridayan.lazyselectioncontainer.demo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import `in`.hridayan.lazyselectioncontainer.LazySelectionContainer
import `in`.hridayan.lazyselectioncontainer.lazySelectionItem
import `in`.hridayan.lazyselectioncontainer.rememberLazySelectionTextLayout

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DemoScreen()
                }
            }
        }
    }
}

@Composable
fun DemoScreen() {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val dummyItems = remember {
        List(100) { index ->
            "This is dummy message #$index. It contains some selectable text to test the LazySelectionContainer functionality. Long press to start selecting!"
        }
    }

    Scaffold { padding ->
        LazySelectionContainer(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            items = dummyItems,
            itemToText = { it },
            listState = listState,
            onCopy = { success ->
                if (success) {
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(dummyItems) { index, item ->

                    val textLayoutResult = rememberLazySelectionTextLayout(index)

                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyLarge,
                        onTextLayout = textLayoutResult,
                        modifier = Modifier
                            .padding(16.dp)
                            .lazySelectionItem(index = index)
                    )
                }
            }
        }
    }
}
