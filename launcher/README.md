# Lumora Home

A home-screen launcher for Android TV boxes that are too weak, or too locked down, to show
Lumora Play's rows on their stock launcher. It ships as its own APK (`com.iptvcinema.launcher`),
built from this repo next to the app.

## What it shows

Header: clock, network state, shortcuts (Lumora Play, Settings, Network, Manage apps).
Rows, top to bottom: Continue watching, Favorite channels, Live now, New this week, Apps, Inputs.
Every row always has at least one card (a placeholder when there is nothing to show), so the
layout and the D-pad path never change.

## How it gets Lumora Play's rows

The launcher cannot share a user id with the app and cannot read the app's database. Instead:

1. `HomeChannelsSync` in the app (`core/tvhome`) already rebuilds the home rows whenever
   favourites, watch history, parental controls or the catalog change. It now writes all four rows
   as JSON to the app's `files/launcher/rows.json` (`LauncherSnapshotStore`) and calls
   `notifyChange` on `content://com.iptvcinema.tv.launcher/rows`.
2. `LauncherContentProvider` in the app serves that file as a cursor. It never touches Room,
   Supabase or the Hilt graph, so a query from the launcher costs a small file read even when it
   has to start the app's process. The provider is guarded by the signature-level permission
   `com.iptvcinema.tv.permission.READ_LAUNCHER_ROWS`, declared in both manifests: only an APK signed
   with the same key as the app can read it. Both APKs use the debug signing config, so build both
   on the same machine.
3. The launcher (`content/ContentRowsRepository`) keeps its own copy of the last rows in its files
   dir and draws the first frame from it, then queries the provider after the first frame and
   whenever the provider reports a change. Live now cards carry the current programme's end time;
   an ended programme loses its text until the app rebuilds the rows. On resume the launcher asks
   the app to rebuild (`call("refresh")`), at most once every 10 minutes.
4. A card opens the app through the same `lumora://play?type=...&id=...` link the Watch Next
   row uses (`core/tvhome/PlaybackDeepLink.kt`).

If the app is not installed, every content row shows one "Install Lumora Play" card; if it is
installed but has no source yet, "Open Lumora Play"; if a row is empty, "Nothing here yet".

Why not the system TV provider: a third-party launcher can only read its own channels there
(`ACCESS_ALL_EPG_DATA` is signature|privileged), and the Realtek box's TV provider answered
nothing at all.

`content/LauncherRowsContract.kt` is a hand-kept copy of the app's
`core/tvhome/launcher/LauncherRowsContract.kt`; change both together.

## Apps row

`LEANBACK_LAUNCHER` and `LAUNCHER` activities from the package manager, Lumora Play first, TV apps
before touch-only apps, then by name. The launcher itself and the box's other home apps are
hidden. Banners (or icons) are rendered once to `files/app_icons/<package>-<lastUpdateTime>.png`
so a later Home press loads a small file instead of inflating every drawable again. Long press
opens a menu: Open, App info, Uninstall (hidden for system apps).

## Inputs row

From `TvInputManager`: HDMI, VGA, YPbPr, AV and tuners, with the connection state when the box
reports it. A card tries, in order: the system's viewer for passthrough URIs (none on the Realtek
box), the launcher's own `InputViewerActivity` (a `TvView` tuned to the passthrough input), and
finally the system input picker (`com.toptech.tv.action.INPUT_ACTIVITY` on Realtek/Toptech
boxes, else Settings). Tuners go straight to the picker.

## Performance rules

- No Hilt, no splash, no window animation. `MainActivity` draws from the cached rows at once;
  data sources start after the first frame.
- `singleTask`, `stateNotNeeded`, and every configuration change handled in place, so standby,
  HDMI mode changes and a language switch never recreate the activity.
- All six rows stay composed. Up/Down are routed through one focus requester per row instead of
  geometric focus search; Left/Right at the ends of a rail are swallowed. Focus can never land
  nowhere.
- Coil memory cache capped at 12 MB; `onTrimMemory` clears it.
- Release builds use R8 with resource shrinking.

## Building, installing, choosing it as home

```
./gradlew :app:assembleRelease :launcher:assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
adb install -r launcher/build/outputs/apk/release/launcher-release.apk
adb shell cmd package set-home-activity com.iptvcinema.launcher/.MainActivity
```

Install the app first: a signature permission is only granted at install time when the package
that defines it is present (both manifests define it, so the order only matters once).
On a box without that command, pick it in Settings > Apps > Default apps > Home app.

## Tests

`./gradlew :launcher:testReleaseUnitTest` covers the pure logic: row assembly and placeholders,
provider row parsing, the JSON cache, app ordering and icon cache keys, input mapping and the
input switch decision, and the clock format.
