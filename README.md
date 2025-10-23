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

## Best Practices

### Recommended Repository Structure

For optimal worktree management, we recommend using a bare repository with separate worktree directories:

```
~/projects/
└── my-project/
    ├── .bare/           # Bare repository (shared Git objects)
    ├── master/          # Main branch worktree
    ├── develop/         # Development branch worktree
    ├── feature-auth/    # Feature branch worktree
    └── bugfix-123/      # Bugfix branch worktree
```

### Initial Setup

**1. Convert existing repository to bare + worktree structure:**

```bash
# Navigate to your project parent directory
cd ~/projects

# Clone as bare repository
git clone --bare git@github.com:user/repo.git my-project/.bare

# Navigate to project directory
cd my-project

# Configure bare repository
echo "gitdir: ./.bare" > .git

# Create main worktree
git worktree add master master
```

**2. Create additional worktrees:**

```bash
# Create worktree for existing branch
git worktree add develop develop

# Create worktree with new branch
git worktree add feature-auth -b feature-auth
```

### Workflow with This Plugin

**1. Open the main worktree in your IDE:**

```bash
# Open IntelliJ IDEA (or your JetBrains IDE)
idea ~/projects/my-project/master
```

**2. Use the plugin to manage worktrees:**

- View all worktrees in **View → Tool Windows → Git Worktrees**
- Create new worktrees for features/bugfixes with auto-generated paths
- Switch between worktrees without closing your current work
- Each worktree opens in a separate IDE window for parallel development

**3. Parallel development example:**

```
Window 1: master/        → Code review, testing
Window 2: feature-auth/  → Implementing authentication
Window 3: bugfix-123/    → Fixing production bug
```

### Benefits

- **Isolated environments**: Each branch has its own working directory
- **No context switching**: No need to stash/commit incomplete work
- **Parallel builds**: Run different build configurations simultaneously
- **Faster branch switching**: No checkout delays or dirty working tree issues
- **Single `.git` storage**: Shared objects save disk space

### Tips

- Use consistent naming: branch name = worktree directory name
- Keep worktree directories at the same level (siblings)
- The plugin automatically detects already-open worktrees and focuses the existing window
- Delete worktrees when branches are merged to keep directory clean

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
