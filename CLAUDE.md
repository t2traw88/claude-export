@AGENTS.md

# Project: Claude Export (RuneLite plugin)

## Summary
A RuneLite plugin that writes my OSRS account state (stats, quests, gear, inventory, bank, slayer) to a local `state.json` so Claude can give advice and my HTML tracker can import it.

## End result
A Plugin Hub–compatible RuneLite plugin (Java 11, built from runelite/example-plugin) plus a short README.

## Always / never
- Read-only and advisory. Never click, move, type, or automate anything in-game.
- No network calls. Only write local files.
- Read game state on the client thread; write files off it with an injected `ScheduledExecutorService`.
- Atomic writes: write a temp file, then rename it over `state.json`.
- Keep `build.gradle` matching the template. Don't add dependencies the client already provides (Gson, Lombok, OkHttp, Guice).
- Verify every RuneLite API name against the current runelite-api, not memory.
- Keep the `state.json` field names stable (schemaVersion 1); other tools depend on them.
- Work in stages. Stop after each one, explain how to test it, and wait for the owner's go-ahead.

## Stages
1. Template runs: rename to "Claude Export" and launch via the test class in developer mode.
2. Stats and quests: player, skills, totalLevel, questPoints, quests; 5s debounce; atomic write.
3. Items: equipment, inventory, bank (cached across restarts, GE prices); side panel with "Export now".
4. Polish: config (per-section toggles), error handling, README.

Later (not this build): an "Import state.json" button in `osrs_tracker.html`.

## Environment
- Windows, PowerShell. JDK: Temurin 11 at `C:\Program Files\Eclipse Adoptium\jdk-11.0.32.101-hotspot\`.
- Run the dev client with `.\gradlew.bat run`, then log in using https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts

## File layout (after the Stage 1 rename)
```
build.gradle, settings.gradle        Gradle build (keep matching the template)
runelite-plugin.properties           Plugin Hub metadata
src/main/java/com/claudeexport/
  ClaudeExportPlugin.java            Main plugin: events, debounce, export
  ClaudeExportConfig.java            Settings shown in RuneLite's config panel
  ExportState.java                   Shape of state.json (field names = JSON keys)
  StateWriter.java                   Atomic write: temp file, then rename
  StateCollector.java                Reads game state (client thread only)
  BankCache.java                     Last-seen bank, saved to bank-cache.json
  ClaudeExportPanel.java             Side panel with "Export now"
src/test/resources/logback-test.xml  Dev-client logging: terminal shows only our logs + warnings
src/test/java/com/claudeexport/
  ClaudeExportPluginTest.java        Launches RuneLite with this plugin (dev mode)
```

## Decisions
- Output location (2026-10-05): `%USERPROFILE%\.runelite\plugin-data\claude-export\state.json`, via `getPluginDirectory()`. No user-configurable folder: that needs `Filepath.Unchecked`, which blocks Plugin Hub auto-review. (Brief's "output folder" config option is dropped.)
- Debounce: change events only set a `dirty` flag; `onGameTick` exports at most once per 5s.
- Use `net.runelite.api.gameval` constants (`InventoryID`, `VarPlayerID`); the old `InventoryID`/`VarPlayer` are deprecated.
- The `ScheduledExecutorService` is RuneLite's shared single thread. Never shut it down.
- Slayer: included. Task name uses the same DB lookup as RuneLite's built-in Slayer plugin; `"slayer": null` when no task.
- Bank cache is tied to the account hash, so another account never exports this account's bank. Placeholders and bank fillers are skipped.
- Config toggles per section (skills, quests, equipment, inventory, bank, slayer). Off = written empty/null, never removed, so JSON keys stay stable. Bank off also stops caching.
- Dev client: run `.\gradlew.bat run --args="--developer-mode"` for a quieter terminal (no `--debug`).
