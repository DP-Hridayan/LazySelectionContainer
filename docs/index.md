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

=== "Version Catalog (Recommended)"

    ```toml
    [versions]
    lazyselectioncontainer = "{{ library_version() }}"

    [libraries]
    lazyselectioncontainer = { module = "io.github.dp-hridayan:lazyselectioncontainer", version.ref = "lazyselectioncontainer" }
    ```
    In your app's `build.gradle.kts`:
    ```kotlin
    dependencies {
        implementation(libs.lazyselectioncontainer)
    }
    ```

=== "Kotlin DSL"

    ```kotlin
    dependencies {
        implementation("io.github.dp-hridayan:lazyselectioncontainer:{{ library_version() }}")
    }
    ```

=== "Groovy"

    ```groovy
    dependencies {
        implementation 'io.github.dp-hridayan:lazyselectioncontainer:{{ library_version() }}'
    }
    ```
