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
lazyselectioncontainer = "1.3.0"

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
    implementation("io.github.dp-hridayan:lazyselectioncontainer:1.3.0")
}
```

### 3. Groovy (`build.gradle`)
```groovy
dependencies {
    implementation 'io.github.dp-hridayan:lazyselectioncontainer:1.3.0'
}
```

---

## Quick Start

Using `LazySelectionContainer` is incredibly simple. Wrap your `LazyColumn` and apply the `lazySelectionItem` modifier to your selectable text.

```kotlin
import in.hridayan.lazyselectioncontainer.lazySelectionItem
import in.hridayan.lazyselectioncontainer.rememberLazySelectionState
import in.hridayan.lazyselectioncontainer.rememberLazySelectionTextLayout

@Composable
fun MySelectableList(items: List<String>) {
    val listState = rememberLazyListState()
    val selectionState = rememberLazySelectionState()

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
                // 3. Register the text layout result
                val textLayoutResult = rememberLazySelectionTextLayout(index)

                Text(
                    text = item,
                    onTextLayout = textLayoutResult,
                    // 4. Apply the modifier to your text items!
                    modifier = Modifier.lazySelectionItem(index)
                )
            }
        }
    }
}
```

## Understanding `itemToText`

Because Jetpack Compose's `LazyColumn` only renders items that are currently visible on the screen, it destroys the underlying `Text` composables for items that scroll off-screen to save memory. 

If a user selects 500 items and hits **Copy**, the library needs a way to instantly build the copied string without forcing Compose to render all 500 hidden items. This is why you must provide the raw `items` list and the `itemToText` extractor function! It allows the library to bypass the UI and extract the exact string directly from your data object.

### Example 1: The Standard Approach (Recommended)
If your list displays complex data (like a chat message with a sender and a body), the most performant and recommended approach is to combine them into a single `Text` composable using `buildAnnotatedString`. You then configure `itemToText` to return both parts!

```kotlin
data class ChatMessage(val sender: String, val body: String)

@Composable
fun ChatList(messages: List<ChatMessage>) {
    val listState = rememberLazyListState()
    
    LazySelectionContainer(
        items = messages,
        // Tell the container exactly how to format the copied text
        itemToText = { "${it.sender}\n${it.body}" }, 
        listState = listState,
    ) { 
        LazyColumn(state = listState) {
            itemsIndexed(messages) { index, message ->
                val combinedText = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(message.sender + "\n") }
                    append(message.body)
                }

                Text(
                    text = combinedText,
                    onTextLayout = rememberLazySelectionTextLayout(index),
                    modifier = Modifier.lazySelectionItem(index)
                )
            }
        }
    }
}
```

### Example 2: The Flattened List Approach (For Complex Layouts)
If you *must* use completely separate `Text` composables for the sender and body (due to complex layout requirements), you cannot use a single `lazySelectionItem(index)` because they would fight over the same index. Instead, you must **flatten** your data list so every selectable element gets a unique index.

```kotlin
@Composable
fun ComplexChatList(messages: List<ChatMessage>) {
    val listState = rememberLazyListState()

    // Flatten the messages so Sender and Body are completely separate items with unique indices!
    val flattenedItems = remember(messages) {
        messages.flatMap { listOf(it.sender, it.body) }
    }

    LazySelectionContainer(
        items = flattenedItems, // Pass the flattened list!
        itemToText = { it },    // Since it's already a string, just return it
        listState = listState,
    ) { 
        LazyColumn(state = listState) {
            itemsIndexed(flattenedItems) { index, text ->
                val isSender = index % 2 == 0
                
                Text(
                    text = text,
                    style = if (isSender) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    onTextLayout = rememberLazySelectionTextLayout(index),
                    modifier = Modifier.lazySelectionItem(index)
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
        dragHandleColor = Color.Red,
        highlightColor = Color.Red.copy(alpha = 0.3f)
    ),
    autoScroll = LazySelectionDefaults.autoScroll(
        scrollThreshold = 48.dp, // Distance from edge to trigger scroll
        maxScrollSpeed = 6.dp    // Max pixels to scroll per frame
    ),
    magnifier = LazySelectionDefaults.magnifier(
        verticalOffset = 64.dp, // Move the loupe higher/lower
        cornerRadius = 24.dp    // Change the shape of the magnifier
    ),
    toolbar = LazySelectionDefaults.toolbar(
        behavior = LazySelectionToolbarBehavior.FloatingAboveSelection, // Or PinnedToDragHandle
        shape = RoundedCornerShape(50) // Change the shape of the toolbar popup
    )
) {
   // ...
}
```

## License

This project is licensed under the Apache 2.0 License.
