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
- Glass vault look: frosted-glass panels (Haze blur on Android 12+, tinted fallback below) over a drifting brass-and-teal backdrop; `GlassSurface`, `VaultBackdrop`, `VaultPreviewTheme`.
- Animations: staged reveal (dial overshoot and settle with totals hidden until it stops, votes flipping over one by one, coin burst, counting payout and balances, stamps on cue), lever overshoot, button press, lock bolt, typewriter Keeper with a glowing keyhole, brass sheen, pot pulse, vault-door cover and heavy-panel screen transitions, game-over ranking rising from last place. All respect reduced motion.
- `RoundSummary.balancesBefore` and `RevealStep.Settled.previous`, so balances can count from their pre-round values.
- Phase 3: phones find each other offline and join a lobby.
  - `:protocol`: versioned envelopes, every client and host message from CLAUDE.md §7 (plus `Welcome` and `HeartbeatReply`), wire copies of the engine's views with converters both ways, and a codec that never throws. Tests cover every message type, version handling, garbage input, and every view of 25 simulated games arriving unchanged.
  - `:net`: a `Transport` interface with Nearby Connections (`P2P_STAR`) and an in-memory fake that can simulate dropped radios and silent phones. `HostSession` seats players, checks names and versions, hands out rejoin tokens, answers heartbeats and notices silence. `ClientSession` browses, joins, measures the host's clock and rejoins the same seat after a drop. 20 tests.
  - Runtime permissions per Android version (`NearbyPermissions`), with an explanation first, a "denied" state that keeps pass-the-phone available, and a route to Settings when Android won't ask again. Each permission is explained in `docs/permissions.md`.
  - `:feature:lobby`: home screen (name, host, join, pass the phone), host lobby (big room code, players, bots, start), join flow (rooms nearby, name retry, waiting room, reconnecting banner, clear endings including version mismatch).
  - Starting a networked game locks the lobby; the rounds themselves arrive in Phase 4.

### Changed
- Question bank rewritten to provoke: 61 questions about the people at the table, the game itself, dilemmas, temptation and existential doubt. Most of the old light questions are retired; their ids are not reused. Nothing had been released, so no saved match refers to them.
- The question now stays on screen during negotiation, not only on the question and vote screens.
- CLAUDE.md §2: question tone and visibility.
- CLAUDE.md §7: protocol versions, wire types, `Welcome` and `HeartbeatReply`, lobby rules, heartbeat timing and what happens to a dropped seat.
- detekt's TooManyFunctions now counts only non-private, non-override functions.
- The app opens on a home screen; pass-the-phone play is one of its three choices.
- CLAUDE.md §2: filled rule gaps (vote weighting, fines for abstainers, VoteSale timing, flip window, Insider totals, rounding, no negative balances, coin conservation, question bank, game-over ties).
- CLAUDE.md §2: promises are now structured (`Promise(from, to, side)`) and judged at REVEAL as kept, broken or void; the broken-promise stamp is public, details stay private until GAME_OVER.
- CLAUDE.md §6–7: live Vault Keeper interjections from templates, triggered only by public events, rate-limited and silent during voting; new `KeeperLine` message; `MarkPromise` renamed `MakePromise`.
- CLAUDE.md §2, §3, §9: recorded the Phase 1 rule decisions (twist choice timing, money moving at REVEAL, public lock dots, Insider and flip-window totals, proposal expiry, what counts as winning, capped payments, RNG kept in game state).
