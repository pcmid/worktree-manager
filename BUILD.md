# Building the Plugin

## Prerequisites

- **JDK 17+** (check with `java -version`)
- **Git** installed and in PATH

**Note**: You don't need to install Gradle separately. The project includes a Gradle wrapper that will automatically download the correct version (8.4) when you run the build.

## Build

### Using Gradle Wrapper (Recommended)

```bash
./gradlew buildPlugin
```

Output: `build/distributions/worktree-manager-{version}.zip`

### Clean Build

```bash
./gradlew clean buildPlugin
```

## Install

1. Open JetBrains IDE
2. Go to **Settings/Preferences → Plugins**
3. Click gear icon ⚙️ → **Install Plugin from Disk**
4. Select the ZIP from `build/distributions/`
5. Click **OK** and restart IDE

## Development

### Verify Plugin

Check compatibility and plugin configuration:

```bash
./gradlew verifyPlugin
```

## Common Issues

### "Unsupported class file version"
Use JDK 17 or higher:
```bash
java -version
```

### Build fails
Clean and rebuild:
```bash
./gradlew clean buildPlugin
```

### Gradle wrapper permission denied (Unix/Mac)
Make the wrapper executable:
```bash
chmod +x gradlew
```

## Gradle Tasks

Common Gradle tasks you can run:

```bash
./gradlew tasks              # List all available tasks
./gradlew buildPlugin        # Build the plugin ZIP
./gradlew clean              # Clean build artifacts
./gradlew compileKotlin      # Compile Kotlin sources
./gradlew verifyPlugin       # Verify plugin structure
```
