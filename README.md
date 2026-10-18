# Prune — Root Debloater

Root bloatware debloater for Oppo / Realme, Xiaomi / Redmi / POCO, Samsung, Infinix / Tecno / itel, Vivo / iQOO. Single-shell execution, UI only selects and monitors. Package `com.raininaugust26.prune`.

## Features
- Root detection (Magisk, KernelSU, APatch) via `su -c id` (uid 0).
- Auto brand/model/ROM detection from `getprop`.
- Curated package lists per brand in `app/src/main/assets/packages/` (`common`, `xiaomi`, `oppo`, `samsung`, `transsion`, `vivo`, `denylist`). Only installed packages shown (via `scan`).
- Safety levels: `safe` / `optional` / `caution` (hidden by default) / `denylisted` (never shown, re-checked in shell).
- Profiles: Safe (safe only), Aggressive (safe+optional), Custom.
- Actions: **Disable** (`pm disable-user --user 0`), **Uninstall for user 0** (`pm uninstall -k --user 0`, optional `--wipe`), **Restore** (`pm enable` + `cmd package install-existing --user 0`).
- Dry-run (default ON), mandatory backup, live log, history with per-package `OK/FAIL/SKIP/DRY/INFO` + `DONE` summary, restore from history. Only one execution at a time.
- No `INTERNET` permission, no telemetry.

## Screens
Root Check → Home (device summary, scan counts) → Package List (search, tabs, profile, select-all) → Detail → Confirm & Execute (dry-run switch, live log) → History (restore) → Settings.

## Settings
Default action, dry-run, backup (locked ON), wipe data on uninstall, show caution, reboot after (Ask/Auto/Never), brand override, theme (system/light/dark), custom list import/export (JSON, same schema).

## Shell contract — `app/src/main/assets/debloat.sh`
Single entry point. No `eval`, no bashisms (mksh/toybox only). Quoted vars, strict `is_valid_pkg`, denylist re-check.

```
sh debloat.sh info                              # INFO|brand|...
sh debloat.sh scan                              # INFO|<pkg>|installed|disabled|uninstalled
sh debloat.sh disable   --list FILE [--dry-run] [--workdir DIR]
sh debloat.sh uninstall --list FILE [--dry-run] [--wipe] [--workdir DIR]
sh debloat.sh restore   --list FILE [--dry-run] [--workdir DIR]
sh debloat.sh backup    [--workdir DIR]
```
Output: `STATUS|package|message` per line (`OK/FAIL/SKIP/DRY/INFO`), last line `DONE|total|ok|fail|skip`. Exit codes: 0 ok/skip, 1 partial fail, 2 bad args, 3 not root, 4 backup failed.

Execution order: verify uid 0 → read list → validate + denylist → skip not-installed (disable/uninstall) → backup (fail → abort) → per-package `pm` → log to `--workdir` + stdout.

Denylist: `assets/packages/denylist.json` patterns (exact or `prefix*`, e.g. `com.android.providers.*`, `com.qualcomm.*`). Copied to `files/prune/denylist.txt` at runtime. Last 20 backups/logs kept.

Test without UI:
```sh
adb shell su -c 'sh /data/data/com.raininaugust26.prune/files/prune/debloat.sh scan'
adb shell su -c 'sh /data/data/com.raininaugust26.prune/files/prune/debloat.sh disable --list /data/data/com.raininaugust26.prune/files/prune/selection.txt --dry-run --workdir /data/data/com.raininaugust26.prune/files/prune'
```

## Package database
`assets/packages/<group>.json`:
```json
{
  "schemaVersion": 1,
  "group": "xiaomi",
  "matchers": { "manufacturer": ["xiaomi"], "brand": ["xiaomi","redmi","poco"] },
  "packages": [{ "id":"com.miui.msa.global","name":"MSA","level":"safe","category":"ads","description":"...","impact":"...","actions":["disable","uninstall"],"verified":false }]
}
```
`level`: safe / optional / caution (denylisted in `denylist.json`). `verified` → true only after real-device test. Matching uses `ro.product.manufacturer` + `ro.product.brand` (lowercased). Unknown brand falls back to `common.json`.

## Project structure
```
.github/workflows/build.yml
app/
  build.gradle.kts
  src/main/
    AndroidManifest.xml
    java/com/raininaugust26/prune/{MainActivity,PruneApp,data/{Models,PackageRepository,SettingsStore},root/{RootExecutor,ShellContract},ui/{AppNav,theme/Theme,screens/*}}
    assets/{debloat.sh,packages/*.json}
    res/values/{strings,colors,themes}.xml
tests/run.sh
gradle/wrapper/gradle-wrapper.properties
```

## Build — GitHub Actions
Push/PR → checks + shell tests + `assembleDebug` artifact. Tag `v*` → signed `assembleRelease` + GitHub Release.

### Secrets (repo Settings → Secrets and variables → Actions)
`KEYSTORE_BASE64` (base64 release keystore), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Fork PRs get no secrets, run checks + debug only.

### Release
```sh
git tag v0.1.0 && git push origin v0.1.0
```

## Local build
```sh
./gradlew assembleDebug
# release (with env)
# Linux/macOS:  KEYSTORE_PATH=/tmp/release.jks ./gradlew assembleRelease
# Windows PowerShell: $env:KEYSTORE_PATH="C:\tmp\release.jks"; ./gradlew assembleRelease
```
Requirements: JDK 17, Android SDK 34.

## Validation
```sh
for f in app/src/main/assets/packages/*.json; do python3 -m json.tool "$f" > /dev/null || exit 1; done
sh -n app/src/main/assets/debloat.sh
shellcheck --shell=sh app/src/main/assets/debloat.sh
sh tests/run.sh
./gradlew testDebugUnitTest
```

## Safety & limits
- Disable/Uninstall affect user 0 only; APK stays on system partition, system storage not reclaimed (only app data, and only with wipe). Factory reset restores everything. Recovery: `adb shell su -c 'cmd package install-existing --user 0 <pkg>'`.
- OTA can restore packages — rescan on each launch.
- System protects some packages → `FAIL` with reason, others continue.
- No system partition writes, no ROM flashing, no Shizuku/ADB mode in v1.

## License
MIT — see `LICENSE` (add before first release). Community lists (e.g. UAD-NG `uad_lists.json`) are reference only; check their license before copying.
