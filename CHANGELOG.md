# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.1] - 2025-12-11

### Changed
- Updated GitHub Actions to use JDK 21
- Release workflow now extracts changelog from CHANGELOG.md instead of PR history

## [0.1.0] - 2025-12-11

### Added
- Force delete option when deleting worktrees (checkbox to ignore uncommitted changes)

### Changed
- Upgraded platform target from 2025.1 to 2025.3
- Updated to use unified IntelliJ Platform (intellijIdea)
- JVM target upgraded from 17 to 21
- Streamlined worktree creation flow: removed success message, directly shows open project dialog

### Fixed
- Fixed compatibility with IntelliJ IDEA 2025.3 (OpenProjectTask API changes)
- Fixed deprecated API warnings for Messages.showDialog
- Refactored StatusBarWidget to use modern TextPresentation API

## [0.0.17] - 2025-11-13

### Added
- Support for creating worktrees from remote branches
- Auto-completion now includes both local and remote branches
- Automatic tracking setup when creating local branches from remote branches

### Changed
- Add padding to worktree list in tool window for better visual appearance
- Branch listing now separates local and remote branches internally for better handling
- Worktree path suggestion now strips remote prefix from remote branch names (e.g., `origin/feature` → `feature`)

## [0.0.16] - 2025-10-24

### Added
- Auto-completion for branch selection with real-time filtering
- Immediate visible error messages (no need to hover mouse)
- Repository and license links in plugin description

### Changed
- Branch names with slashes now create subdirectory structures (e.g., `feature/auth` → `../feature/auth/`)
- Improved UI responsiveness with native IntelliJ auto-completion component
- Enhanced validation feedback with visible error labels

### Fixed
- Performance issues with branch filtering
- Validation errors only showing on hover

## [0.0.15] - 2025-10-23

### Added
- Initial release
- List and switch between Git worktrees
- Create new worktrees from existing or new branches
- Delete worktrees safely
- Tool window for easy worktree management
- Status bar widget showing current worktree
- Smart project detection (focuses existing window if already open)
- Flexible opening options (new window or current window)
- Auto-generated worktree paths based on branch names
- Branch conflict detection
