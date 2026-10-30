# prompt.md — Building Minority Vault, phase by phase

## How to use this file

1. Put `CLAUDE.md` in the repo root first. Every phase assumes it has been read.
2. Paste **one phase at a time** into Claude Code (or your AI coding tool). Don't paste the whole file.
3. After each phase: run the acceptance checks yourself, play-test on real phones where noted, then commit with the phase tag (e.g. `git commit -m "phase-03: nearby lobby"`).
4. If a phase reveals a design problem, update `CLAUDE.md` before moving on.
5. Each phase ends with "Stop and report". Don't let the AI run ahead into the next phase.

Rough timeline for one student working part-time: phases 0–4 ≈ 4–5 weeks (playable game), phases 5–8 ≈ 3–4 weeks (the AI and polish that make it impressive), phases 9–11 ≈ 2 weeks.

---

## Phase 0 — Project setup

```
Read CLAUDE.md fully before doing anything.

Goal: create the empty multi-module Android project described in CLAUDE.md §5, with tooling, so that later phases only add code.

Tasks:
1. Create a new Android project "Minority Vault", package com.example.minorityvault (I'll rename later), single activity, Jetpack Compose, Material 3.
2. Set up a Gradle version catalog with the latest stable versions of: Kotlin, AGP, Compose BOM, Hilt, kotlinx.serialization, kotlinx.coroutines, Room, DataStore, Media3, Nearby Connections (play-services-nearby), JUnit 5, Kotest assertions, Turbine. Check current stable versions; don't guess.
3. Create modules: :app, :engine, :protocol, :net, :ai, :data, :designsystem, :audio, :feature:lobby, :feature:round, :feature:results, :feature:history, :feature:settings, :feature:tutorial. :engine and :protocol must be pure Kotlin/JVM modules with no Android dependencies.
4. Enforce the dependency direction from CLAUDE.md §5.
5. Add ktlint and detekt with a baseline config. Add a GitHub Actions workflow that runs `./gradlew ktlintCheck detekt test assembleDebug` on every push and PR.
6. Add .gitignore (keystores, local.properties, google-services.json excluded), README.md with build steps, CHANGELOG.md, docs/permissions.md, docs/ideas.md.
7. The app should launch to a placeholder "Minority Vault" screen.

Acceptance:
- `./gradlew ktlintCheck detekt test assembleDebug` passes locally.
- The debug APK installs and opens on a real phone.
- `:engine` has no Android imports (add a Gradle check or test that fails if it does).

Stop and report: list of modules, versions chosen, anything you weren't sure about.
```

---

## Phase 1 — Game engine (pure Kotlin)

```
Read CLAUDE.md §2, §3 and §9.

Goal: implement the complete game rules in :engine as pure, deterministic, fully tested Kotlin. No UI, no networking.

Tasks:
1. Model: GameConfig, PlayerId, Player (name, balance, isBot, isDebtor, items), Phase (sealed), Round, Vote (YES/NO/ABSTAIN, doubled flag), Contract (sealed: ShareWinnings, FixedPayment, VoteSale), Promise, Twist (sealed), GameState.
2. Intents (sealed interface), named to match the protocol in CLAUDE.md §7: StartGame, AdvancePhase, ContractPropose, ContractRespond, VoteSelect, VoteLock, VoteUnlock, VoteFlip, UseItem, MarkPromise, TimerExpired. Also a QuestionBank of question IDs (text stays out of :engine).
3. A single reducer: reduce(state, intent, rng): Result<GameState, RuleError>. Illegal intents return typed errors, never throw.
4. Resolution exactly as in CLAUDE.md §2: counting, no-minority rollover, twist application, fines with caps, pot split with remainder rollover, contracts in creation order, debtor status.
5. TwistCatalog: ExactMinority(n), SecondChance, Insider(playerId), RobinHood, NoTwist. Each twist exposes: id, validity check for the current state, and how it modifies resolution. Add offerTwists(state, rng): List<Twist> returning 3 valid options for the Keeper to pick from.
6. Views: toPublicView(state) and toPrivateView(state, playerId). Votes of other players must be absent until REVEAL. Write tests proving no leaks.
7. A RoundSummary/RevealSequence data object describing what happened, in order (for animations and narration later).
8. A simulator: simulateGame(config, players, policies, seed) that plays full games with simple random policies. Use it in a property-style test that runs 1,000 seeded games and asserts invariants: balances + pot conserved exactly (CLAUDE.md §2 has no sinks), no negative balance or pot, no votes leaked in views, game always ends, no exceptions.

Acceptance:
- `./gradlew :engine:test` passes with tests covering every rule and twist, ties, empty sides, all-abstain, debtors (including recovery and all-debtor game over), chained contracts, VoteSale, the flip window, Insider totals, RobinHood ties, rounding.
- Same seed + same intents = identical final state.

Stop and report: test count, any rule ambiguity you hit and how you resolved it (and propose CLAUDE.md edits).
```

---

## Phase 2 — Design system and screens (single device, fake data)

```
Read CLAUDE.md §8.

Goal: build the full look of the game with Compose, driven by fake state, playable as a single-device "hot seat" demo against the real engine.

Tasks:
1. :designsystem — theme (colors, typography with bundled fonts), spacing, shapes. Components with previews: VaultButton (brass primary, ghost secondary), PaperCard, KeeperBubble (keyhole avatar + speech), TimerBar + TimerText (turns red under 10 s), PlayerChip (initials, balance, broken-promise flag), Lever (two-state, animated handle), VaultDial (animated needle + ticks), Stamp (rotated red), ContractCard (sign/decline), ChatBubble.
2. Screens in :feature:round: QuestionScreen, NegotiationScreen (tabs: Lobby, Messages, Contracts), VoteScreen (two levers, double-vote toggle, lock status dots), RevealScreen (dial animation, counts, payouts, stamp). :feature:results: GameOverScreen with final ranking.
3. A HotSeatViewModel that runs the real :engine locally so I can click through a full 10-round game on one phone (pass the phone between players; hide the screen between turns).
4. Navigation between screens driven by Phase.
5. Accessibility: 48 dp targets, content descriptions, reduced-motion support (skip dial spin), font scaling up to 1.3x without clipping.

Acceptance:
- I can play a full hot-seat game start to finish on one phone.
- Every component has a Compose preview. Screens look right on a small phone (360 dp wide) and a large one.

Stop and report: screenshots or a short description per screen, anything that felt clunky to use.
```

---

## Phase 3 — Local multiplayer: lobby and connection

```
Read CLAUDE.md §3 and §7.

Goal: real phones find each other offline and join a lobby using Nearby Connections. No gameplay over the network yet.

Tasks:
1. :protocol — message envelope with version and seq, all ClientIntent and HostEvent types from CLAUDE.md §7, kotlinx.serialization, round-trip tests for every message.
2. :net — Transport interface plus NearbyTransport (P2P_STAR). HostSession: advertise, accept connections, track players, send per-player messages. ClientSession: discover, request connection, receive. Also a FakeTransport for tests.
3. Runtime permissions for Nearby Connections across Android versions (check the current official docs for the exact list per API level). Clear rationale screens and a graceful "permissions denied" state. Document each permission in docs/permissions.md.
4. :feature:lobby — Home (Host game / Join game), Host lobby (room code shown large, player list, add bot slots, start button enabled at 4+ players), Join flow (list of nearby hosts, enter name, waiting room).
5. Heartbeats, clock offset measurement at join, and a disconnected/rejoin state on clients.
6. Version mismatch shows a clear error.

Acceptance:
- 3+ real phones on airplane mode with Bluetooth/Wi-Fi on can join one lobby and see each other.
- Unit tests for HostSession/ClientSession logic using FakeTransport.
- Turning a client's Bluetooth off and on lets it rejoin within 60 s.

Stop and report: which phones you tested, connection times, any flaky behaviour.
```

---

## Phase 4 — Full round over the network

```
Goal: the complete game playable across multiple phones, host authoritative.

Tasks:
1. HostGameController: owns GameState, applies ClientIntents through the engine reducer, runs phase timers on host time, broadcasts PublicView to all and PrivateView to each player.
2. ClientGameController: renders views, sends intents, shows countdowns using endsAt + clock offset.
3. Chat: public lobby and DMs routed through host. MarkPromise on a message. DMs only delivered to the two participants.
4. Contracts end to end: propose from a template picker (never free text), respond, fee charged, shown on both sides, executed at settlement.
5. Vote: select, lock, unlock before timer ends, double vote. Unlocked at timer end = abstain.
6. Reveal and settlement: host sends a RevealSequence; all phones play it in sync.
7. Game over with final ranking, and "play again" with the same lobby.
8. The host player plays on the host phone like everyone else (host UI must not reveal anything extra).

Acceptance:
- A full 10-round game with 4 humans on 4 phones completes without desync.
- A test using FakeTransport plays a full game with 6 simulated clients and asserts no client ever received another player's vote before REVEAL.
- Killing a client app mid-negotiation and reopening it rejoins with correct state.

Stop and report: bugs found during the 4-phone play-test and how they were fixed.
```

---

## Phase 5 — Audio, haptics and juice

```
Read CLAUDE.md §8 (sound).

Goal: make it feel like a game, not a form.

Tasks:
1. :audio — SoundManager (SoundPool) for: dial click, final dial clunk, vote lock, countdown tick, heartbeat, reveal boom, coin cascade, betrayal sting, pen scratch, UI tap. Use royalty-free/CC0 sounds and record their sources and licences in docs/assets.md.
2. MusicController (Media3): layered stems for negotiation (base + 2 tension layers faded in as the timer drains), near-silence during vote, stingers on reveal (win/lose per player).
3. Haptics wrapper: lever pull, vote lock (strong), coin win (light pattern), betrayal (double pulse).
4. Animations: lever handle, dial spin with ease-out, coin count-up, stamp slam, timer pulse under 10 s. All respect reduced motion.
5. Settings: master/music/SFX volume, haptics on/off.

Acceptance:
- Sounds play in sync on all phones during reveal (within ~150 ms).
- No audio glitches when the app goes to background and back.

Stop and report: what still feels flat.
```

---

## Phase 6 — The Vault Keeper (AI host)

```
Read CLAUDE.md §3 rule 4 and §6.

Goal: an AI host that picks twists and taunts players, with zero risk to game integrity and full offline support.

Tasks:
1. :ai — VaultKeeper interface: suspend fun choose(context: KeeperContext, options: List<TwistOption>): KeeperOutput. KeeperContext includes round number, public balances, recent public events, broken promises, and player style labels. Never DMs or votes.
2. TemplateKeeper: deterministic, seeded, 40+ template lines across situations (round start, someone broke a promise, big pot, someone near bankruptcy, final round). Always works.
3. NanoKeeper: ML Kit GenAI Prompt API. Check feature availability at runtime; download model if needed and allowed; handle BUSY and quota errors with fallback.
4. CloudKeeper: calls the proxy (Phase 6b) with a 4 s timeout.
5. KeeperChain: Nano → Cloud → Template. Prefetch next round's output during the current reveal so a timer never waits on AI.
6. Strict output parsing: JSON with twistId (must be in the offered list) and line (≤160 chars, passes a profanity/safety filter). Anything invalid → TemplateKeeper.
7. Settings toggle: "AI host: Smart (online) / On-device only / Classic (offline)".
8. Tests with fake providers: malformed JSON, invalid twistId, timeout, exception, offensive line — all fall back correctly.

Phase 6b — proxy/:
1. Cloudflare Worker in TypeScript: POST /keeper accepts KeeperContext + options, builds the prompt server-side, calls the Gemini API with the key from a Worker secret, returns KeeperOutput.
2. Per-install rate limit (e.g. 30 requests/hour) and request size limit. Reject anything that doesn't match the schema.
3. The app sends a random install ID only. No names beyond in-game display names.
4. README for deploying with wrangler and setting the secret.

Acceptance:
- Game is identical in rules whichever provider runs. Airplane mode → TemplateKeeper, no errors shown to players.
- No API key anywhere in the app repo or APK (grep the APK to prove it).

Stop and report: sample Keeper lines from each provider, average latency of Cloud and Nano.
```

---

## Phase 7 — AI bot players

```
Goal: bots that fill empty seats, feel human-ish, and play reasonably well.

Tasks:
1. BotPlayer runs on the host, receives only its own PrivateView plus PublicView, and sends normal ClientIntents (same path as humans).
2. Personalities: LOYAL (keeps deals), GREEDY (maximises expected coins, breaks deals when profitable), PARANOID (avoids contracts, votes against predicted majority), VENGEFUL (targets whoever betrayed it), CHAOTIC (random within reason).
3. Decision logic: estimate majority from public chat signals and past rounds; evaluate contracts by expected value; decide when to betray.
4. Chat: template lines per personality and situation, with human-like timing delays. Optional rephrasing via the Keeper provider chain.
5. Use the engine simulator to run bot-vs-bot tournaments (10,000 games) and print win rates per personality. Tune until no personality wins over ~35% in mixed tables.
6. Lobby UI: add/remove bots with a personality picker, or "random".

Acceptance:
- 2 humans + 4 bots is a fun, complete game.
- Tournament results are saved to docs/bot-balance.md.

Stop and report: tournament win rates and which personality feels most "human".
```

---

## Phase 8 — Player profiling and post-match analysis (the ML showcase)

```
Goal: turn match data into insight, and use it to make the game smarter.

Tasks:
1. :data — Room entities: Match, MatchPlayer, RoundRecord, ContractRecord, PromiseRecord. Save every finished match on the host and send each player a summary to save locally.
2. Feature extraction per player per match: promises made/broken, contracts proposed/signed/honoured, minority rate, average vote lock time, coalition size, risk score.
3. Style labels: start rule-based (Loyalist, Opportunist, Gambler, Ghost, Kingmaker), then add a k-means clustering option trained on accumulated match data (write the training notebook in /ml with Python, export centroids as JSON into assets). Document the method in docs/ml.md.
4. Post-match screen: betrayal graph (nodes = players, edges = deals, red edges = broken), each player's style card, "moment of the match" (biggest swing).
5. Feed style labels into KeeperContext and bot VENGEFUL/PARANOID logic.
6. History screen: past matches, personal stats over time.

Acceptance:
- Betrayal graph renders correctly for 4–8 players.
- docs/ml.md explains features, method, and limitations clearly enough for a viva/project report.

Stop and report: example profiles from real play-tests.
```

---

## Phase 9 — Onboarding, tutorial and settings

```
Goal: a first-time player understands the game in under 2 minutes.

Tasks:
1. Interactive tutorial: a short scripted round against 4 bots that teaches question → negotiation → contract → vote → reveal, with the Vault Keeper as narrator.
2. "How to play" reference screen with the rules from CLAUDE.md §2 in plain language.
3. Settings: player name, negotiation length, rounds (5/10), AI host mode, volumes, haptics, reduced motion, language (English now; structure ready for Hindi).
4. First-launch permission explanation flow (from Phase 3), polished.

Acceptance:
- A friend who has never seen the game completes the tutorial without help.

Stop and report: friction points from that test.
```

---

## Phase 10 — Hardening

```
Goal: make it robust enough for real groups.

Tasks:
1. Edge cases: player leaves mid-game (convert to abstaining ghost or bot, host choice), host app backgrounded (pause + warning), phone call interrupts, low battery, 8-player stress test.
2. Security review: no leaks of votes/DMs in views or logs, input validation on every ClientIntent (sizes, rates, legal phase), reject spoofed player IDs.
3. Performance: Compose recomposition check, cold start under 2 s on a mid-range phone, memory under 200 MB during a game, battery usage of a 30-minute session.
4. Accessibility pass with TalkBack on every screen.
5. Crashlytics integrated (release builds only), with no personal data or chat content in logs.

Acceptance:
- Three full play-test sessions with no crashes or desyncs.
- Checklist results saved to docs/qa.md.

Stop and report: remaining known issues, ranked.
```

---

## Phase 11 — Release (free)

```
Read CLAUDE.md §12.

Goal: shipping pipeline that costs nothing.

Tasks:
1. Release signing: generate a keystore (I'll keep it safe offline), read signing config from environment variables, never from the repo.
2. R8/ProGuard rules so release builds work (kotlinx.serialization, Hilt, Room, Nearby).
3. GitHub Actions release workflow on tag v*: test, lint, build signed APK, compute SHA-256, create a GitHub Release with the APK and checksum, optional upload to Firebase App Distribution and itch.io (butler) when their secrets are present.
4. In-app "Check for updates" that compares the installed version against the latest GitHub Release tag and opens the download page (no auto-install).
5. Store-style assets: icon, feature graphic, 4 screenshots, a short trailer GIF, itch.io page text, privacy policy (GitHub Pages) stating what is and isn't collected.
6. A docs/distribution.md section on Android developer verification: check the current rules, register the app with a limited distribution account for testers, and list what's needed for a full public release.

Acceptance:
- Pushing tag v1.0.0 produces a downloadable signed APK on GitHub Releases automatically.
- A friend installs it from the link and plays.

Stop and report: release URL, APK size, anything still manual.
```
