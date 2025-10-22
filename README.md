# Git Worktree Manager

A JetBrains IDE plugin for managing Git worktrees. Works with all JetBrains IDEs that support Git.

## Features

- **List & Switch**: View and switch between Git worktrees
- **Create Worktree**: Create new worktrees from branches
- **Delete Worktree**: Remove worktrees safely
- **Tool Window**: Dedicated sidebar for worktree management
- **Status Bar Widget**: Shows current worktree, click to switch
- **Smart Project Switching**: Automatically detects if worktree is already open and focuses existing window
- **Flexible Opening Options**: Choose to open worktrees in new window or replace current project

## Installation

### From Source

1. Clone the repository:
   ```bash
   git clone https://github.com/pcmid/worktree-manager.git
   cd worktree-manager
   ```

2. Build the plugin:
   ```bash
   ./gradlew buildPlugin
   ```

3. Install in IDE:
   - Go to **Settings/Preferences → Plugins**
   - Click gear icon → **Install Plugin from Disk**
   - Select `build/distributions/worktree-manager-{version}.zip`
   - Restart IDE

## Usage

### Tool Window

Access from **View → Tool Windows → Git Worktrees** (left sidebar).

- View all worktrees with branch info
- Double-click to switch worktrees
- Toolbar buttons:
  - **Refresh**: Update worktree list
  - **Create**: Create new worktree
  - **Delete**: Remove selected worktree
  - **Switch**: Open selected worktree

### Status Bar Widget

Click the worktree name in the bottom-right status bar to:
- View all worktrees in a popup menu
- Quick switch to another worktree

### Creating a Worktree

1. Click **Create** button in tool window or status bar widget
2. Enter branch name (can be existing or new)
3. Path is auto-generated based on branch name (can be customized)
4. Click **OK**
5. Choose how to open the new worktree:
   - **New Window**: Opens in a new IDE window
   - **Current Window**: Replaces current project
   - **Cancel**: Don't open, just create

**Notes:**
- Default path follows pattern: `../worktree-{branch-name}`
- Branch conflicts are checked before creation
- If worktree project is already open, it will focus that window instead of creating a new one

### Switching Worktrees

When switching to a worktree:
1. Plugin first checks if the worktree is already open in another window
2. If yes, focuses that existing window
3. If no, prompts you to choose:
   - **New Window**: Opens worktree in new window
   - **Current Window**: Closes current project and opens worktree
   - **Cancel**: Cancels the operation

## Requirements

- **IDE**: Any JetBrains IDE 2025.1+ with Git support, including:
  - IntelliJ IDEA (Ultimate, Community, Educational)
  - GoLand
  - PyCharm (Professional, Community)
  - WebStorm
  - PhpStorm
  - CLion
  - RubyMine
  - Rider
  - Android Studio
- **Git**: 2.5+ (with worktree support)
- **JDK**: 17+ (for building from source only, not needed for using the plugin)

## License

MIT License
