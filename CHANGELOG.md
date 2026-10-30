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
- Phase 1: the complete game engine in `:engine`, pure and deterministic. `GameEngine.reduce(state, intent)` returns typed `RuleError`s and never throws for an illegal intent.
  - Rules: vote weighting, minority and no-minority rounds, fines with caps and the all-in final round, pot split with rollover, the three contract templates, structured promises with verdicts, items, debtors and game-over ties.
  - Twists: ExactMinority, SecondChance (flip window), Insider, RobinHood and NoTwist, offered three at a time by `TwistCatalog`.
  - `toPublicView` / `toPrivateView`, which never show another player's vote before REVEAL; a `RevealSequence` for animations; public events for Keeper interjections.
  - A SplitMix64 RNG stored in the game state, and a simulator with a random policy.
  - 79 tests, including 1,000 simulated games that check coin conservation, no negative balances, no leaks and rule coverage.
- Phase 2: the look of the game, and pass-the-phone play on one device.
  - `:designsystem`: vault palette, bundled Big Shoulders Display and Instrument Sans fonts (OFL, see `docs/assets.md`), spacing, reduced-motion support, and VaultButton, PaperCard, KeeperBubble, TimerBar/TimerText, PlayerChip, Lever, VaultDial, Stamp, ContractCard and ChatBubble, each with previews at 1x and 1.3x font scale.
  - `:ai`: `QuestionBank` with 32 questions; the engine only sees their ids.
  - `:feature:round`: Question, Negotiation (lobby, promises, contracts), Vote, Flip and Reveal screens, with UI mappers from the engine's views.
  - `:feature:results`: game-over ranking.
  - `:app`: pass-the-phone play with a cover screen between private turns, driven by the real engine, with 8 tests including a full match played through UI events.
- `PublicGameView.contractFee`, so clients can warn before signing.

### Changed
- CLAUDE.md §2: filled rule gaps (vote weighting, fines for abstainers, VoteSale timing, flip window, Insider totals, rounding, no negative balances, coin conservation, question bank, game-over ties).
- CLAUDE.md §2: promises are now structured (`Promise(from, to, side)`) and judged at REVEAL as kept, broken or void; the broken-promise stamp is public, details stay private until GAME_OVER.
- CLAUDE.md §6–7: live Vault Keeper interjections from templates, triggered only by public events, rate-limited and silent during voting; new `KeeperLine` message; `MarkPromise` renamed `MakePromise`.
- CLAUDE.md §2, §3, §9: recorded the Phase 1 rule decisions (twist choice timing, money moving at REVEAL, public lock dots, Insider and flip-window totals, proposal expiry, what counts as winning, capped payments, RNG kept in game state).
