# CLAUDE.md — Minority Vault

This file is the source of truth for anyone (human or AI) working in this repo. Read it fully before writing code. If a task conflicts with this file, stop and ask.

## 1. What we are building

**Minority Vault** is an offline, local-multiplayer Android party game of negotiation and betrayal for 4–8 players, each on their own phone in the same room. One rule drives everything: **only the minority wins**.

- Each round, an AI host (the **Vault Keeper**) asks a yes/no question. The content of the question doesn't matter; only which side ends up smaller.
- Players negotiate in a public lobby and private DMs, make promises and contracts, then cast a secret vote.
- The minority splits the pot. The majority pays a fine into it. Richest player after the final round wins.
- No internet is required to play. AI features degrade gracefully when offline.

Target user: friends playing together in one room. Target device: mid-range Android phones (4 GB RAM, Android 8+).

## 2. Game rules (canonical)

All numbers live in `GameConfig` and must never be hardcoded elsewhere.

| Setting | Default |
|---|---|
| Players | 4–8 (humans + AI bots) |
| Starting balance | 100 coins |
| Rounds | 10 |
| Base fine | 10 (rounds 1–3), 20 (4–6), 30 (7–9) |
| Final round | "All-in": fine = 50% of each majority player's balance |
| Question phase | 10 s |
| Negotiation phase | 150 s (configurable 60–300 s) |
| Vote phase | 20 s (unlocked players auto-abstain) |
| Contract signing fee | 5 coins, paid by the signer, goes into the pot |
| Flip window (`SecondChance` only) | 10 s |
| Keeper interjection cooldown | 20 s |

**Round flow:** `QUESTION → NEGOTIATION → VOTE → [FLIP_WINDOW] → REVEAL → SETTLEMENT → (next round | GAME_OVER)`. `FLIP_WINDOW` happens only when the round's twist is `SecondChance`.

**Questions:** the engine picks each round's question from a `QuestionBank` of stable question IDs, using the seeded RNG and never repeating within a match. The engine stores only the ID; the text lives in localised resources outside `:engine`. The Keeper does not write questions.

**Money invariants:**
- Coins are conserved exactly: the sum of all balances plus the pot never changes during a match. There are no sinks.
- No balance ever goes below 0. Every payment (fine, fee, contract transfer) is capped at what the payer has left at that moment.
- All divisions round down (floor). Leftover coins stay in the pot.

**Resolution:**
1. **Count.** Each player's effective vote is YES, NO or ABSTAIN. YES weight and NO weight are summed; a double vote adds 2, a normal vote adds 1. Abstainers add nothing.
2. **Find the minority.** The side with the smaller weight is the minority side; its players are the *minority players*. Abstainers are never minority players. If the weights are equal, or either weight is 0, there is **no minority**.
3. **Apply the twist.** The twist may change who receives the payout (see twists). It never changes the counts.
4. **Fines.** Every player who is not a minority player pays the round's fine into the pot: majority voters and abstainers always, and everyone when there is no minority. Final round fine = floor(50% × that player's balance).
5. **Payout.** The pot (now including this round's fines) is split equally between the winners, floor division, remainder stays in the pot. Normally the winners are the minority players. With no winners, the whole pot rolls over. A player's *winnings* this round = their share of the pot.
6. **Contracts.** Execute triggered contracts in creation order, each capped at the payer's current balance.
7. **Debtor status.** A player is a **Debtor** while their balance is 0. Status is recomputed after every settlement, so a Debtor who wins coins stops being one.

**Contracts:** A contract is a structured, engine-enforced rule, never free text. Supported templates:
- `ShareWinnings(from, to, percent)` — `percent` is 1–100. If `from` has winnings this round, transfer floor(winnings × percent / 100) to `to`.
- `FixedPayment(from, to, amount, condition)` — conditions: `IF_FROM_WINS`, `IF_TO_WINS`, `ALWAYS`. "Wins" means "has winnings this round".
- `VoteSale(debtor, buyer, price)` — `debtor` must be a Debtor when it is signed. From signing until the end of the round, only `buyer` may select, lock or unlock the debtor's vote (via the normal vote intents, naming the debtor); the debtor cannot. The buyer cannot use the debtor's items. If the buyer doesn't lock it, the debtor abstains. The debtor remains the player whose vote it is, so any winnings go to the debtor. `price` moves from buyer to debtor at step 6 whatever the outcome.

Signing: contracts can be proposed and signed only during NEGOTIATION. The signer (the player who accepts) pays the fee, except for `VoteSale`, where the buyer always pays. A player who can't afford the fee can't sign (`RuleError`). A player may be the debtor in at most one `VoteSale` per round.

Contracts expire at end of round.

**Promises:** ordinary chat is never judged. A **promise** is a structured message, not free text, and is never enforced. It only gets judged.
- During NEGOTIATION a player can make a promise with the chat composer's promise button: `Promise(from, to, side)`, meaning "I will vote `side` (YES or NO) this round". `to` is one player (sent as a DM) or `EVERYONE` (sent to the public lobby). Only the speaker can make a promise; nobody can create one on someone else's behalf.
- A player can hold one promise per recipient per round. A new promise to the same recipient replaces the old one. Promising YES to one player and NO to another is allowed: that is double-dealing, and one of them will break.
- At REVEAL the engine judges every promise against the promiser's final effective vote, after any flip. It is **kept** if the vote matches `side` and **broken** otherwise, including abstaining. A promise is **void** (not judged) if the promiser's vote was controlled by a `VoteSale` buyer that round.
- Visibility: a promise is shown with a promise badge to its recipient(s) only. At REVEAL a broken promise becomes public: the promiser's chip gets the red **Stamp** and a broken-promise count, but the recipient and the promise itself stay private to the two parties. At GAME_OVER every promise and contract is revealed to everyone for the betrayal graph.
- Tagged promises are more credible than chat (they carry the stamp risk), which is why a player would make one at all.

**Debtors:** still vote, can sell their vote via `VoteSale`, and cannot win the game while they are Debtors at GAME_OVER.

**Winning the game:** at GAME_OVER the winner is the non-Debtor with the highest balance; players tied on the highest balance share the win. If every player is a Debtor, nobody wins (the Keeper keeps the vault).

**Items (MVP has only Double Vote):** each player starts with 1 Double Vote, usable once per match. It is applied to the player's own vote during VOTE, before locking, and is consumed when the vote is locked at the end of VOTE. Unlocking before the timer ends returns it. Later: Peek, Shield.

**Twists:** chosen each round from `TwistCatalog`. Each twist is a typed object with validated parameters and a validity check against the current state. Examples:
- `ExactMinority(n)` — the minority players win only if there are exactly `n` of them (counting players, not vote weight). Otherwise nobody wins this round: fines are paid as normal and the pot rolls over. Valid only for 1 ≤ n < active player count / 2.
- `SecondChance` — after VOTE, a `FLIP_WINDOW` shows everyone the YES and NO weights only (never who voted what). Each player whose vote was locked YES or NO may flip it to the other side once. Abstainers cannot join. Counting then uses the flipped votes.
- `Insider(playerId)` — during VOTE, that player's private view shows the live YES and NO weights (totals only, never individual votes). The player is chosen with the seeded RNG. Nobody else is told who the Insider is.
- `RobinHood` — the payout goes to the poorest player instead of the minority players. "Poorest" means lowest balance after step 4, Debtors included; ties split the pot equally. Fines are unchanged. If there is no minority, the pot still rolls over and nobody is paid.
- `NoTwist`.

## 3. Non-negotiable architecture rules

1. **The engine is pure Kotlin.** `:engine` has zero Android dependencies, is deterministic given a seed, and is fully unit-tested. All game logic lives there.
2. **The host is authoritative.** One phone hosts and owns the true `GameState`. Clients send intents; the host validates, applies them through the engine, and broadcasts results.
3. **Clients only receive what they may see.** The host sends each client a `PublicGameView` plus that client's own `PrivatePlayerView`. Never broadcast full state. Other players' votes are never sent before REVEAL. The only exceptions are vote *totals* (never individual votes), and only where a twist allows it: the Insider's private view during VOTE, and everyone's view during FLIP_WINDOW. Promise and contract visibility follows §2; the only public trace of a private contract before GAME_OVER is the anonymous "a contract was signed" event (§6).
4. **The LLM never touches game state.** The Vault Keeper only (a) chooses from a list of valid twists the engine offers and (b) writes narration text. Its output is parsed, validated, and falls back to a deterministic choice on any failure.
5. **AI is optional.** Every AI feature has an offline fallback. The game must be fully playable with airplane mode on.
6. **No secrets in the APK.** No API keys in code, resources, or BuildConfig. Cloud LLM calls go through the proxy (see §6).
7. **Unidirectional data flow.** UI renders `StateFlow<UiState>`; user actions become intents; ViewModels never mutate state directly.

## 4. Tech stack

| Area | Choice | Why |
|---|---|---|
| Language | Kotlin 2.x | Standard for Android |
| UI | Jetpack Compose + Material 3 (custom theme) | UI-heavy game, no physics needed |
| Architecture | Single activity, MVVM + MVI-style intents, Kotlin Coroutines/Flow | Predictable state |
| DI | Hilt | Standard, testable |
| Local multiplayer | Google Nearby Connections API (`P2P_STAR`) | Offline, Bluetooth + Wi-Fi, encrypted |
| Serialization | kotlinx.serialization (JSON, versioned envelopes) | Simple, debuggable |
| Persistence | Room (match history, profiles), DataStore (settings) | Free, offline |
| Audio | SoundPool (SFX), Media3 ExoPlayer (layered music) | Low-latency SFX |
| Haptics | `VibrationEffect` via a `Haptics` wrapper | Lever/lock feel |
| On-device LLM | ML Kit GenAI Prompt API (Gemini Nano) when the device supports it | Free, offline |
| Cloud LLM | Gemini API free tier via a Cloudflare Worker proxy | Free, key stays server-side |
| Offline narration | Template library in `:ai` | Always works |
| Bots | Rule-based personalities first, optional LiteRT policy later | Ship first, learn later |
| Crash reporting | Firebase Crashlytics (Spark plan) | Free |
| Testing | JUnit Jupiter (JUnit 6, the successor to JUnit 5) + Kotest assertions, Turbine, Compose UI tests | Android modules will need the `android-junit5` plugin when they get unit tests |
| Lint/format | ktlint + detekt | |
| CI/CD | GitHub Actions | Free for public repos |

Always use the **latest stable** versions at setup time via the Gradle version catalog (`gradle/libs.versions.toml`). Do not pin to versions from memory without checking.

`minSdk = 26`, `targetSdk`/`compileSdk` = latest stable.

## 5. Module layout

```
:app            Activity, navigation, Hilt setup, wiring
:engine         Pure Kotlin. GameState, GameConfig, rules, twists, contracts, resolution, RNG
:protocol       Pure Kotlin. Message envelopes, ClientIntent, HostEvent, views, versioning
:net            Nearby Connections transport, host/client sessions, reconnection
:ai             VaultKeeper (provider chain), bot personalities, templates, player profiling
:data           Room DB, DAOs, DataStore, repositories
:designsystem   Theme, colors, typography, reusable components (Lever, VaultDial, PaperCard, Stamp)
:audio          SoundPool/ExoPlayer wrappers, music layer controller, haptics
:feature:*      lobby, round (question/negotiation/vote/reveal), results, history, settings, tutorial
proxy/          Cloudflare Worker (TypeScript) for cloud LLM calls
```

Dependency direction: `feature → (designsystem, ai, net, data, audio) → protocol → engine`. `engine` depends on nothing.

## 6. AI design

**Vault Keeper provider chain** (`VaultKeeper` interface, first available wins):
1. `NanoKeeper` — ML Kit Prompt API on supported devices. Check availability at runtime.
2. `CloudKeeper` — calls `proxy/` (Cloudflare Worker) which holds the Gemini API key as a secret and rate-limits per install ID. Timeout 4 s.
3. `TemplateKeeper` — deterministic templates with slot filling (player names, balances, betrayals). Always available.

Keeper calls only happen on the host, only between phases (never blocking a timer), and are prefetched during the previous phase. Output schema:
```json
{ "twistId": "ExactMinority", "line": "string ≤ 160 chars" }
```
`twistId` must be one of the options the engine offered, else fall back. Profanity/safety filter on `line`, else fall back.

**Interjections:** the Keeper also comments live, so it isn't silent while people scheme. Interjections:
- come only from `TemplateKeeper` (instant, offline, never an LLM call), so they can never delay a timer;
- are triggered only by public events from the engine: a public promise is made, a contract is signed (announced anonymously, with no parties or terms, e.g. "Somewhere, ink is drying."), a public chat burst, the negotiation timer reaching 30 s, a player becoming a Debtor, and at REVEAL a broken promise (naming the promiser, which is public by then);
- never mention anything private: no DM content, contract parties or terms, private promises or votes;
- are rate-limited by the interjection cooldown in `GameConfig`, and are never sent during VOTE or FLIP_WINDOW (near silence, §8);
- are sent to all clients as a `KeeperLine` message and shown in the Keeper's speech bubble.

**Bots:** `BotPlayer` with personality (`LOYAL`, `GREEDY`, `PARANOID`, `VENGEFUL`, `CHAOTIC`). Decisions come from code (game-theory heuristics using public info + its own deals). Chat lines come from templates, optionally rephrased by the Keeper provider. Bots must never see private info they couldn't legitimately know.

**Player profiling:** after each match, compute per-player stats (promises made/broken, contracts signed, minority rate, risk score) and assign a style label. Store locally in Room. Used for the post-match betrayal graph and to personalise Keeper taunts.

## 7. Networking protocol

- Envelope: `{ v: protocolVersion, seq: Long, type: String, payload: {...} }`. Reject mismatched major versions with a clear error screen.
- Client → host: `Join`, `Ready`, `ChatPublic`, `ChatDm`, `MakePromise(to, side)`, `ContractPropose`, `ContractRespond`, `VoteSelect`, `VoteLock`, `VoteUnlock`, `VoteFlip`, `UseItem`, `Heartbeat`. Vote intents carry an optional `forPlayer` used only by a `VoteSale` buyer. Engine intents use the same names as these messages.
- Host → client: `LobbyUpdate`, `PhaseStarted(phase, endsAt)`, `PublicView`, `PrivateView`, `ChatMessage` (may carry a promise), `ContractUpdate`, `KeeperLine`, `RevealSequence` (includes promise verdicts), `RoundSettled`, `GameOver`, `Error`.
- Timers use host time; clients get `endsAt` plus a clock offset measured at join.
- Reconnection: a dropped client may rejoin with its `playerToken` within 60 s; host resends full views. If the host drops, the game pauses on all clients with a clear message (host migration is out of scope for v1).
- DMs route through the host. The host does not display DMs on its own screen unless the host player is a participant.

## 8. Visual and audio identity

- **Palette (banknote and vault):** ink green `#0F2B27` base, panels `#173B35` / `#1E4740`, aged paper `#EAE3CF` for anything official (questions, contracts), brass `#C9A055` for money and primary actions, stamp red `#D2584A` for time pressure and betrayal only, steel `#93A09B` for mechanical parts.
- **Type:** a condensed display face (e.g. Big Shoulders Display) for numbers and headlines; a clean sans (e.g. Instrument Sans) for UI text. Bundle fonts locally; check licences (OFL is fine).
- **Signature elements:** the vote **Lever**, the reveal **VaultDial**, **PaperCard** for questions/contracts, red **Stamp** for broken promises.
- **Sound:** dial clicks, vote-lock clunk (+ haptic), countdown tick with heartbeat in the last 10 s, reveal boom, coin cascade, betrayal sting, pen scratch on signing. Music layers add tension as the negotiation timer drains; near silence during the vote.
- Copy is sentence case, plain, and in the Vault Keeper's dry, theatrical voice for host lines only.

## 9. Coding conventions

- Kotlin official style, enforced by ktlint. detekt must pass with no new issues.
- Immutable data classes for state; `sealed interface` for intents/events/phases.
- No `!!` outside tests. No `GlobalScope`. Inject dispatchers.
- Engine functions are pure: `(GameState, Intent) -> Result<GameState, RuleError>` style. Randomness only via injected seeded RNG.
- Every engine rule and every twist has unit tests, including edge cases (ties, empty sides, all abstain, debtors, contracts chaining, rounding remainders).
- Compose: stateless composables with previews; state hoisted to ViewModels. Respect reduced-motion and font scaling. Minimum touch target 48 dp. Content descriptions on icon-only controls.
- Strings in `strings.xml` (plan for Hindi/regional localisation later).
- Log with a tagged logger wrapper; never log DMs, contracts, or votes in release builds.

## 10. Commands

```bash
./gradlew assembleDebug                 # build debug APK
./gradlew :engine:test                  # engine unit tests (run constantly)
./gradlew test                          # all unit tests
./gradlew connectedDebugAndroidTest     # instrumented tests (device needed)
./gradlew ktlintCheck detekt            # style and static analysis
./gradlew assembleRelease               # signed release (needs keystore env vars)
cd proxy && npx wrangler dev            # run the LLM proxy locally
cd proxy && npx wrangler deploy         # deploy the proxy
```

## 11. Definition of done (every task)

- Builds, all tests pass, ktlint and detekt clean.
- New engine logic has tests. New UI has a Compose preview.
- Works with airplane mode on (AI features fall back).
- No secrets committed. No new permissions without a note in `docs/permissions.md`.
- `CHANGELOG.md` updated. If a decision changed, update this file.

## 12. Free deployment plan

**Costs: ₹0.** Everything below is free for this project's scale.

| Need | Free option |
|---|---|
| Source + CI | GitHub (public repo) + GitHub Actions |
| APK hosting | GitHub Releases (primary), itch.io (secondary, nice game page) |
| Tester builds | Firebase App Distribution |
| Crash reports | Firebase Crashlytics |
| Cloud LLM | Gemini API free tier, behind a Cloudflare Workers free-plan proxy |
| On-device LLM | ML Kit Prompt API (no per-call cost) |

**Release pipeline:** push a tag `vX.Y.Z` → GitHub Actions runs tests and lint → builds a signed release APK (keystore stored as base64 in GitHub Secrets) → attaches it to a GitHub Release → optionally uploads to Firebase App Distribution and itch.io (via butler).

**Android developer verification (important):** Google now requires apps installed on certified Android devices to be registered by a verified developer, rolling out country by country from September 2026 and globally from 2027. A free **limited distribution** account (for students and hobbyists, up to 20 devices, no ID or fee) covers testing with friends. A full public release outside Play needs a full Android Developer Console account (one-time $25). Re-check the current rules before each release.

**Google Play:** not free (one-time $25 registration). Optional later step if the game grows.

## 13. Out of scope for v1

Online/internet multiplayer, host migration, accounts/login, in-app purchases, ads, iOS, cross-match persistent economy. Note ideas in `docs/ideas.md` instead of building them.
