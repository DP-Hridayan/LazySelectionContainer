# Usage Guide

Using `LazySelectionContainer` is designed to be incredibly simple and idiomatic to Jetpack Compose. You wrap your lazy layout and explicitly mark which text items are selectable.

## Quick Start

Wrap your `LazyColumn` and apply the `lazySelectionItem` modifier to your selectable text.

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

---

## Understanding `itemToText`

Because Jetpack Compose's `LazyColumn` only renders items that are currently visible on the screen, it destroys the underlying `Text` composables for items that scroll off-screen to save memory. 

If a user selects 500 items and hits **Copy**, the library needs a way to instantly build the copied string without forcing Compose to render all 500 hidden items. This is why you must provide the raw `items` list and the `itemToText` extractor function! It allows the library to bypass the UI and extract the exact string directly from your data object.

### The Standard Approach (Recommended)

If your list displays complex data (like a chat message with a sender and a body), the most performant and recommended approach is to combine them into a single `Text` composable using `buildAnnotatedString`. You then configure `itemToText` to return both parts.

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

### The Flattened List Approach (For Complex Layouts)

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
