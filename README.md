# LazySelectionContainer

A robust and highly-customizable text selection container for Jetpack Compose `LazyList`s (like `LazyColumn`). 

Jetpack Compose's native `SelectionContainer` currently struggles when wrapping lazy layouts, often causing performance issues or failing to properly select items off-screen. `LazySelectionContainer` solves this by explicitly tracking item bounds and providing smooth text selection, drag-to-select auto-scrolling, and a customizable magnification loupe—all while preserving lazy loading performance!

## Features

- **Seamless integration** with `LazyColumn`.
- **Drag-to-Select Auto-scrolling**: Dragging your finger to the top or bottom edge smoothly auto-scrolls the list.
- **Magnifier Loupe**: Built-in native-style magnification when dragging selection handles for precise character control.
- **Customizable**: Fully customizable colors, handle shapes, and toolbar behavior.
- **State Hoisting**: Easily control or read the current selection programmatically.

---

## Installation

The library is available on Maven Central. 

### 1. Version Catalog (libs.versions.toml) - Recommended
```toml
[versions]
lazyselectioncontainer = "1.0.0"

[libraries]
lazyselectioncontainer = { module = "io.github.dp-hridayan:lazyselectioncontainer", version.ref = "lazyselectioncontainer" }
```
In your app's `build.gradle.kts`:
```kotlin
dependencies {
    implementation(libs.lazyselectioncontainer)
}
```

### 2. Kotlin DSL (`build.gradle.kts`)
```kotlin
dependencies {
    implementation("io.github.dp-hridayan:lazyselectioncontainer:1.0.0")
}
```

### 3. Groovy (`build.gradle`)
```groovy
dependencies {
    implementation 'io.github.dp-hridayan:lazyselectioncontainer:1.0.0'
}
```

---

## Quick Start

Using `LazySelectionContainer` is incredibly simple. Wrap your `LazyColumn` and apply the `lazySelectionItem` modifier to your selectable text.

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import in.hridayan.lazyselectioncontainer.LazySelectionContainer
import in.hridayan.lazyselectioncontainer.lazySelectionItem
import in.hridayan.lazyselectioncontainer.rememberSelectionState

@Composable
fun MySelectableList(items: List<String>) {
    val listState = rememberLazyListState()
    val selectionState = rememberSelectionState()

    // 1. Wrap your lazy list in the container
    LazySelectionContainer(
        modifier = Modifier.fillMaxSize(),
        items = items,
        itemToText = { it }, // Tell the container how to extract text from your item
        listState = listState,
        selectionState = selectionState,
        onCopy = { success ->
            if (success) println("Copied to clipboard!")
        }
    ) { 
        // 2. Build your lazy list as normal
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, item ->
                Text(
                    text = item,
                    // 3. Apply the modifier to your text items!
                    modifier = Modifier.lazySelectionItem(index = index)
                )
            }
        }
    }
}
```

## Customization

You can fully customize the colors, auto-scroll behavior, and handles using `LazySelectionDefaults`.

```kotlin
LazySelectionContainer(
    // ...
    colors = LazySelectionDefaults.colors(
        handleColor = Color.Red,
        selectionBackgroundColor = Color.Red.copy(alpha = 0.3f)
    ),
    autoScroll = LazySelectionDefaults.autoScroll(
        scrollThreshold = 48.dp, // Distance from edge to trigger scroll
        maxScrollSpeed = 6.dp    // Max pixels to scroll per frame
    ),
    magnifierVerticalOffset = 48.dp // Move the loupe higher/lower
) {
   // ...
}
```

## License

This project is licensed under the Apache 2.0 License.
