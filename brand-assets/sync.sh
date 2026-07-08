#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/brand-assets/source"
RES="$ROOT/app/src/main/res"

mkdir -p "$SRC" "$RES/drawable-nodpi"

copy_if_exists() {
  local file="$1"
  local dest="$2"
  if [[ -f "$SRC/$file" ]]; then
    cp "$SRC/$file" "$dest"
    echo "✓ $file → ${dest#$ROOT/}"
    return 0
  fi
  return 1
}

if copy_if_exists icon_foreground.png "$RES/drawable-nodpi/ic_launcher_foreground_src.png"; then
  cat > "$RES/drawable/ic_launcher_foreground.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:gravity="center"
    android:src="@drawable/ic_launcher_foreground_src" />
EOF
  echo "  → ic_launcher_foreground.xml updated to use PNG"
fi

if copy_if_exists icon_background.png "$RES/drawable-nodpi/ic_launcher_background_src.png"; then
  cat > "$RES/drawable/ic_launcher_background.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:gravity="fill"
    android:src="@drawable/ic_launcher_background_src" />
EOF
  echo "  → ic_launcher_background.xml updated to use PNG"
fi

if copy_if_exists splash_icon.png "$RES/drawable-nodpi/ic_lumora_splash_icon_src.png"; then
  cat > "$RES/drawable/ic_lumora_splash_icon.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:gravity="center"
    android:src="@drawable/ic_lumora_splash_icon_src" />
EOF
  echo "  → ic_lumora_splash_icon.xml updated to use PNG"
fi

copy_if_exists wordmark.png "$RES/drawable-nodpi/ic_lumora_wordmark.png" || true

copy_if_exists sidebar-min.png "$RES/drawable-nodpi/nav_sidebar_min.png" || true
copy_if_exists sidebar-full.png "$RES/drawable-nodpi/nav_sidebar_full.png" || true

if copy_if_exists tv_banner.png "$RES/drawable-nodpi/tv_banner_src.png"; then
  cat > "$RES/drawable/tv_banner.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:gravity="center"
    android:src="@drawable/tv_banner_src" />
EOF
  echo "  → tv_banner.xml updated to use PNG"
fi

echo ""
echo "Done. Rebuild with: ./gradlew :app:assembleDebug"
