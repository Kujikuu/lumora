# Lumora Play — brand assets

Replace launcher icon, Android TV banner, and splash artwork from this folder, then run the sync script.

## Quick start

1. Export your artwork into `brand-assets/source/` using the filenames below.
2. Run:

```bash
./brand-assets/sync.sh
./gradlew :app:assembleDebug
```

3. Reinstall on the TV/emulator and check: home launcher row, app icon, and cold start splash.

## Source files

| File | Used for | Recommended size | Notes |
|------|----------|------------------|-------|
| `icon_foreground.png` | App launcher icon (foreground) | **1024×1024** PNG, transparent | Keep logo inside center **66%** safe zone (adaptive icon mask). |
| `icon_background.png` | App launcher icon (background) | **1024×1024** PNG (optional) | Solid `#000000` is fine; project default is a black XML shape if omitted. |
| `splash_icon.png` | Native cold-start splash | **432×432** PNG, transparent | Shown centered on black; use a **mark only** (no wide wordmark). |
| `wordmark.png` | Splash / marketing wordmark | **432×432** or wider PNG | Optional; copied to `drawable-nodpi` if you reference it from XML/Compose. |
| `sidebar-min.png` | Collapsed nav rail logo | **432×432** PNG | Icon-only mark shown when the rail is collapsed. |
| `sidebar-full.png` | Expanded nav rail header | **1116×433** PNG (wide) | Full wordmark + tagline when the rail expands. |
| `tv_banner.png` | Android TV launcher banner | **1280×720** PNG (16:9) | Required on TV; shows in the apps row on the home screen. |

Vector fallbacks in `app/src/main/res/drawable/` are used until you add PNGs.

## Where files land in the app

| Asset | Manifest / theme reference | Destination |
|-------|---------------------------|-------------|
| Launcher icon | `AndroidManifest.xml` → `android:icon="@mipmap/ic_launcher"` | `mipmap-anydpi-v26/ic_launcher.xml` + foreground/background drawables |
| TV banner | `AndroidManifest.xml` → `android:banner="@drawable/tv_banner"` | `drawable/tv_banner.xml` or `drawable-nodpi/tv_banner.png` |
| Native splash | `values/themes.xml` → `Theme.IptvCinema.Splash` | `windowSplashScreenAnimatedIcon` → `@drawable/ic_lumora_splash_icon` |
| Splash background | `values/themes.xml` | `@color/brand_background` (`#000000`) |

## Design tokens (current app)

- Background: `#000000`
- Accent / logo: `#E70302` (Lumora red)
- App name string: `Lumora Play` (`values/strings.xml`)

## Tips

- **TV banner**: Use a horizontal lockup (mark + “Lumora Play”). Avoid thin text; 10-foot UI needs bold shapes.
- **Launcher icon**: Square mark only; wordmarks get cropped on adaptive icons.
- **Splash**: Same mark as the icon, on transparent PNG; the system centers it on the splash background.
- After swapping vectors, clear the launcher cache on the TV (or uninstall/reinstall) to see icon changes.

## Manual overrides

If you prefer vectors instead of PNGs, edit:

- `app/src/main/res/drawable/ic_launcher_foreground.xml` — launcher mark
- `app/src/main/res/drawable/ic_lumora_splash_icon.xml` — splash mark
- `app/src/main/res/drawable/tv_banner.xml` — TV banner
