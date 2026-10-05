# Claude Export

A RuneLite plugin that saves your OSRS account state (stats, quests, gear, inventory, bank and slayer task) to a JSON file on your PC. You can give that file to Claude for advice, or import it into a tracker so you stop updating stats by hand.

The plugin is **read-only**. It never clicks, moves, types or automates anything in-game, and it makes **no network calls**. It only writes local files.

## Where the file goes

```
%USERPROFILE%\.runelite\plugin-data\claude-export\state.json
```

To open the folder, press `Win + R`, paste `%USERPROFILE%\.runelite\plugin-data\claude-export` and press Enter.

| File | What it is |
|---|---|
| `state.json` | The export. Each write is atomic (written to a temp file, then swapped in), so it is never half-written. |
| `bank-cache.json` | Your last-seen bank, so the export still has it after a restart. Delete it to clear the cached bank. |

## When it exports

- After you log in, once your character has loaded
- When stats, quest points, slayer task, equipment, inventory or bank change (at most once every 5 seconds)
- Immediately when you close the bank
- When you click **Export now** in the side panel (the orange **C** icon)

## What's in `state.json`

| Key | Contents |
|---|---|
| `player` | Name, combat level, world |
| `skills` | Each skill's real level, boosted level and XP |
| `totalLevel`, `questPoints` | Totals |
| `quests` | Every quest: `NOT_STARTED`, `IN_PROGRESS` or `FINISHED` |
| `equipment` | Worn items with their slot (`HEAD`, `WEAPON`, ...) |
| `inventory` | Inventory items |
| `bank` | Last-seen bank items with GE price each, `totalGeValue`, and `lastSeen` (when the bank was last open; `null` if never) |
| `slayer` | Current task name and kills remaining, or `null` with no task |

The bank can only be read while it's open, so `bank` shows what it held the last time you opened it. It belongs to one account, so logging into another account won't export the first account's bank. The bank's potion storage and other storage (seed vault, POH) are not included.

## Settings

In RuneLite, open the wrench icon (Configuration) and search for **Claude Export**. Each section can be switched off. A switched-off section is written as empty (or `null`) rather than removed, so tools reading the file always see the same keys. Turning off **Export bank** also stops the bank being cached.

## Running it (development)

Requirements: Windows, Git, and **Java 11** (Eclipse Temurin 11).

1. Open PowerShell in this folder and run:
   ```
   .\gradlew.bat run --args="--developer-mode"
   ```
   This builds the plugin and opens a development copy of RuneLite with it loaded. Leave PowerShell open while you play. Plain `.\gradlew.bat run` also works but prints far more log output.
2. **Jagex accounts:** the dev client can't log in through the Jagex Launcher directly. One time, open **RuneLite (configure)** from the Start menu, add `--insecure-write-credentials` to *Client arguments*, save, then launch RuneLite once from the Jagex Launcher. The dev client then reuses that login. See [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
   > `.runelite\credentials.properties` lets anyone log into your account. Never share it. Delete it when you're done developing.
3. After changing any code, close the dev client and run the command again.

You can also open the folder in IntelliJ and run `ClaudeExportPluginTest`.

## Troubleshooting

- **The file isn't updating:** Notepad doesn't refresh on its own, so reopen the file. To watch it live, run:
  `Get-Content "$env:USERPROFILE\.runelite\plugin-data\claude-export\state.json" -Wait`
- **The panel says "Export failed":** check `%USERPROFILE%\.runelite\logs\client.log` for lines containing `Claude Export`. A failed write is skipped, never crashes the client, and is retried on the next change.
- **No file at all:** make sure you started the client with `gradlew.bat run`, not the Jagex Launcher, and that you're logged in.

## License

BSD 2-Clause. See [LICENSE](LICENSE). The slayer task lookup is adapted from RuneLite's Slayer plugin (also BSD 2-Clause); its copyright notice is kept in `StateCollector.java`.

Not affiliated with or endorsed by Jagex. Old School RuneScape is a trademark of Jagex Ltd.

## Privacy

`state.json` and `bank-cache.json` contain your character name, stats and bank. They stay on your PC unless you share them. `bank-cache.json` also holds RuneLite's internal account ID number (not a password or login token).
