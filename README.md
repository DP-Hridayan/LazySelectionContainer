# LazySelectionContainer

A robust and highly-customizable text selection container for Jetpack Compose `LazyList`s (like `LazyColumn`). 

Jetpack Compose's native `SelectionContainer` currently struggles when wrapping lazy layouts, often causing performance issues or failing to properly select items off-screen. `LazySelectionContainer` solves this by explicitly tracking item bounds and providing smooth text selection, drag-to-select auto-scrolling, and a customizable magnification loupe—all while preserving lazy loading performance.

---

### 📖 [Read the Full Documentation](https://dp-hridayan.github.io/LazySelectionContainer/)

For comprehensive installation instructions, advanced usage examples, and complete customization guides, please visit the official documentation site:

**👉 https://dp-hridayan.github.io/LazySelectionContainer**

---

## Quick Start

Wrap your `LazyColumn` and apply the `lazySelectionItem` modifier to your selectable text.

```kotlin
import in.hridayan.lazyselectioncontainer.lazySelectionItem
import in.hridayan.lazyselectioncontainer.rememberLazySelectionState
import in.hridayan.lazyselectioncontainer.rememberLazySelectionTextLayout

@Composable
fun MySelectableList(items: List<String>) {
    val listState = rememberLazyListState()

    LazySelectionContainer(
        modifier = Modifier.fillMaxSize(),
        items = items,
        itemToText = { it }, 
        listState = listState,
    ) { 
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(items) { index, item ->
                Text(
                    text = item,
                    onTextLayout = rememberLazySelectionTextLayout(index),
                    modifier = Modifier.lazySelectionItem(index)
                )
            }
        }
    }
}
```

## License

This project is licensed under the Apache 2.0 License.
