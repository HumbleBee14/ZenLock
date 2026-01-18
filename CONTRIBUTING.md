# Contributing to ZenLock

Thank you for your interest in contributing to ZenLock! This document provides guidelines and instructions for contributing to the project.

## Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment for all contributors. We expect:

- Professional and constructive communication
- Respect for differing viewpoints and experiences
- Acceptance of constructive criticism
- Focus on what is best for the community

## How to Contribute

### Reporting Bugs

Before submitting a bug report:

1. Check the [existing issues](https://github.com/HumbleBee14/ZenLock/issues) to avoid duplicates
2. Use the latest version of the app
3. Collect relevant information (Android version, device model, logs)

When submitting a bug report, include:

- Clear and descriptive title
- Steps to reproduce the issue
- Expected behavior vs actual behavior
- Screenshots or logs if applicable
- Device information and Android version

### Suggesting Features

Feature suggestions are welcome! Please:

1. Check existing feature requests first
2. Provide a clear use case for the feature
3. Explain how it aligns with ZenLock's focus on productivity
4. Consider implementation complexity and user experience

### Pull Request Process

#### 1. Setting Up Development Environment

```bash
# Fork and clone the repository
git clone https://github.com/HumbleBee14/ZenLock.git
cd ZenLock

# Create a feature branch
git checkout -b feature/your-feature-name
```

#### 2. Making Changes

- Write clean, readable code following the project's style
- Add comments for complex logic
- Update documentation if needed
- Test thoroughly on multiple Android versions if possible

#### 3. Code Style Guidelines

**Java Conventions**:
- Use 4 spaces for indentation
- Follow standard Java naming conventions:
  - Classes: `PascalCase`
  - Methods/variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- Maximum line length: 120 characters
- Add JavaDoc comments for public methods

**Example**:
```java
/**
 * Manages focus session scheduling and execution.
 *
 * @param context Application context
 * @param duration Session duration in milliseconds
 * @return true if session started successfully
 */
public boolean startFocusSession(Context context, long duration) {
    // Implementation
}
```

#### 4. Commit Guidelines

Write clear, descriptive commit messages:

```
feat: Add support for multi-partner accountability system
^--^  ^--------------------------------------------^
|     |
|     +-> Summary in present tense
|
+-------> Type: feat, fix, docs, style, refactor, test, chore
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, no logic change)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

#### 5. Testing

Before submitting:

- Test on at least one physical device
- Verify all permissions work correctly
- Test focus session start/stop cycles
- Verify SMS functionality if modified
- Check for memory leaks in long-running sessions

#### 6. Submitting Pull Request

1. Push your changes to your fork
   ```bash
   git push origin feature/your-feature-name
   ```

2. Open a pull request with:
   - Clear title and description
   - Reference to related issues
   - Screenshots/videos for UI changes
   - Test results and device information

3. Respond to review feedback promptly

## Development Guidelines

### Project Architecture

ZenLock follows a modular architecture:

- **Activities**: UI layer and user interaction
- **Services**: Background operations (AccessibilityService, ForegroundService)
- **Utils**: Helper classes for specific functionality
- **Fragments**: Reusable UI components

### Key Areas for Contribution

**Good First Issues**:
- UI improvements and bug fixes
- Documentation enhancements
- Adding unit tests
- Localization support

**Advanced Contributions**:
- Performance optimizations
- New focus modes or features
- Analytics integration
- Cloud sync capabilities

### Security Considerations

When contributing:

- Never log sensitive information (phone numbers, OTP codes)
- Use ProGuard rules for new sensitive classes
- Follow Android security best practices
- Request permissions appropriately

### Dependencies

Adding new dependencies:

1. Justify the need in your PR description
2. Use AndroidX libraries when possible
3. Check license compatibility (prefer Apache 2.0, MIT)
4. Update `build.gradle.kts` and documentation

## Building and Testing

### Debug Build

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumentation tests (requires connected device)
./gradlew connectedAndroidTest
```

### Code Quality

```bash
# Lint checks
./gradlew lint

# View lint report
open app/build/reports/lint-results.html
```

## Documentation

Update documentation when:

- Adding new features
- Changing existing functionality
- Adding new configuration options
- Modifying build process

## Getting Help

Need assistance?

- Ask questions in [GitHub Discussions](https://github.com/HumbleBee14/ZenLock/discussions)
- Review existing issues and PRs
- Check the [README](README.md) for project overview

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

## Recognition

Contributors are recognized in:

- GitHub contributors list
- Release notes for significant contributions
- Project documentation

Thank you for helping make ZenLock better!
