# Contributing to AeroChaser ✈️

Thank you for your interest in contributing to AeroChaser! As a cloud-connected aviation photo hub, we maintain high standards of code quality, architecture compliance, and user experience.

---

## 1. Architectural Guidelines

AeroChaser is built using **Clean Architecture** to separate business logic from platform-specific frameworks.

### Directory Structure
- **`domain/`**: Pure Kotlin layer containing business logic (Use Cases, Domain Models, Repository Interfaces).
  - **CRITICAL REQUIREMENT:** The domain layer must have **zero Android framework dependencies or imports** (no `Context`, no Android classes). This keeps the core logic completely portable.
- **`data/`**: Android and web-specific implementations (Room Database, API network clients, WorkManager workers, Exif parsers).
- **`presentation/`**: Jetpack Compose screen components, ViewModels, and navigation.
- **`di/`**: Dependency injection wiring via Koin.

### Adding New Features
1. Define the necessary domain model in `domain/models/`.
2. Define the repository contract interface in `domain/repository/`.
3. Implement use cases in `domain/usecase/` to coordinate actions.
4. Implement the repository interface in `data/repository/` or specialized source classes.
5. Create UI screens in `presentation/` and bind them in `AppNavGraph.kt`.
6. Register all new ViewModels, Repositories, Use Cases, and Data Sources in `di/AppModule.kt`.

---

## 2. Coding Standards

### Kotlin & Gradle
- Use Kotlin DSL (`.gradle.kts`) for all build scripts.
- Use Kotlin Coroutines and Flows for asynchronous data management and reactive state propagation.
- Keep variables private and expose immutable state (e.g., expose `StateFlow<T>` instead of `MutableStateFlow<T>`).

### Jetpack Compose UI
- Use **Material 3** guidelines and components.
- Ensure all screens dynamically adapt to the application theme (light/dark mode, Material You dynamic colors).
- Use `remember` and `derivedStateOf` to prevent unnecessary recompositions in complex UIs.
- Pass specific `key` fields to lazy layout items (e.g. `items(list, key = { it.id })`) to avoid list flickering and performance stutter.
- Provide descriptive `contentDescription` attributes on all icons and interactive items for screen readers and accessibility.

### Multi-threading & Safety
- **Network Requests:** Always wrap HTTP and API network requests in `try-finally` blocks and call `disconnect()` on connection resources to prevent socket leaks.
- **Thread Visibility:** Mark mutable state shared across coroutine dispatchers (such as access tokens or API clients) with the `@Volatile` annotation.
- **Thread-Safe Helpers:** Ensure formatting or parsing classes that are not thread-safe (e.g., `SimpleDateFormat`) are wrapped in `ThreadLocal` or run inside confined thread instances.

---

## 3. Pull Request Checklist

Before submitting a pull request, please verify that:
1. All changes compile cleanly:
   ```bash
   ./gradlew assembleDebug
   ```
2. All unit tests pass:
   ```bash
   ./gradlew test
   ```
3. The domain layer remains completely free of Android framework dependencies.
4. No sensitive information, API keys, or private accounts are tracked in git (`local.properties` is utilized).
5. The `README.md` and `architecture_visualization.md` are updated if your changes add or modify core architecture features.
