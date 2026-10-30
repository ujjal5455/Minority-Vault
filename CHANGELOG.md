# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Phase 0: multi-module Android project (`:app`, `:engine`, `:protocol`, `:net`, `:ai`, `:data`, `:designsystem`, `:audio`, six `:feature:*` modules).
- Gradle version catalog with current stable versions.
- Build-time check of the module dependency direction from CLAUDE.md §5.
- Test proving `:engine` imports nothing from Android.
- ktlint and detekt for every module; GitHub Actions CI.
- Placeholder launch screen with the vault palette.

### Changed
- CLAUDE.md §2: filled rule gaps (vote weighting, fines for abstainers, VoteSale timing, flip window, Insider totals, rounding, no negative balances, coin conservation, question bank, game-over ties).
