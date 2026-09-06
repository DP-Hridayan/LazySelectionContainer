# Customization

`LazySelectionContainer` provides a robust default configuration that mimics the native Android selection experience. However, nearly every aspect of the selection UI and behavior can be customized to match your app's branding.

You can modify these properties using `LazySelectionDefaults`.

## Configuration Options

When initializing your container, you can pass custom configurations for colors, auto-scrolling, magnification, the floating toolbar, and haptic feedback.

```kotlin
LazySelectionContainer(
    // ... required parameters ...
    
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
    ),
    
    haptics = LazySelectionDefaults.haptics(
        enabled = true,                                         // Enable/disable haptics
        hapticFeedbackType = HapticFeedbackType.TextHandleMove  // Configure the haptic type
    )
) {
   // ... content ...
}
```

### Explanation of Properties

* **Colors**: Controls the primary tint of the drag handles and the semi-transparent highlight drawn over selected text.
* **Auto-Scroll**: Defines how close the user's finger needs to be to the top or bottom of the list before it begins scrolling automatically (`scrollThreshold`), and how fast it moves (`maxScrollSpeed`).
* **Magnifier**: The loupe that appears when dragging a handle. `verticalOffset` determines how far above the user's finger the magnifier appears, ensuring it isn't blocked by their hand.
* **Toolbar**: The floating context menu (Copy, Select All). You can pin it to the drag handle or have it float centered above the current selection block.
* **Haptics**: Subtle vibrations that occur when the selection changes or handles are moved, enhancing the tactile feel of the text selection.
