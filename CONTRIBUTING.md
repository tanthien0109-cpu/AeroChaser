# Contributing to AeroChaser ✈️

Thank you for your interest in contributing to AeroChaser! We welcome contributions from the aviation and developer communities to make this the best photo hub for plane spotters.

## Our Philosophy

AeroChaser is built on **Clean Architecture** principles. We prioritize:
1.  **Platform Independence**: The `domain` layer must remain pure Kotlin with no Android dependencies.
2.  **Performance**: Avoid N+1 queries in Room, use WorkManager for long-running tasks, and optimize Compose recompositions.
3.  **Aesthetics**: Follow Material 3 guidelines and maintain a premium, high-contrast look.

## How to Contribute

### 1. Development Environment
- Use **Android Studio Hedgehog** (or newer).
- Set up `local.properties` with the required API keys (Maps, OAuth, Gemini) as described in the [README](README.md).

### 2. Branching Policy
- Create a feature branch from `main`: `feature/your-feature-name`.
- Bug fixes should use: `fix/issue-description`.

### 3. Coding Standards
- **Kotlin DSL**: Use Kotlin for all Gradle files.
- **Compose**: Use standard Material 3 components. Annotate stable models if necessary for performance.
- **Dependency Injection**: Use **Koin** for all dependency management.
- **Logging**: Use `Log.d(TAG, ...)` with a consistent class-level tag. Avoid `println`.

### 4. Pull Request Process
- Ensure your code builds successfully: `./gradlew assembleDebug`.
- Run all tests: `./gradlew test` and `./gradlew connectedAndroidTest`.
- Update documentation if you add new features or change existing architecture.
- Request a review and address any feedback.

## Adding New Cloud Sources
If you wish to add a new cloud provider (e.g., Dropbox, OneDrive):
1.  Add a new implementation of `CloudPhotoSource` in the `data.cloud` package.
2.  Define the necessary API integration (REST or SDK).
3.  Update `CloudImportViewModel` and `CloudImportScreen` to include the new service tab.
4.  Ensure **Duplicate Detection** is handled correctly via `PhotoRepository.photoExists(metadata)`.

## License
By contributing, you agree that your contributions will be licensed under the project's [LICENSE](LICENSE).
