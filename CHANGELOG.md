# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Add padding to worktree list in tool window for better visual appearance

## [0.0.16] - 2025-01-24

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

## [0.0.15] - 2025-01-23

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
