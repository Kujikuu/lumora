# Supabase setup (Phase 5)

Phase 5 connects the Android TV app to Supabase Auth, syncs user data, and enables TV activation via the companion website at **https://iptv.afifistudio.com**.

## 1. Project and Android app

1. Create a project at https://supabase.com
2. Copy [`../local.properties.example`](../local.properties.example) to `local.properties` and set:
   - `SUPABASE_URL`
   - `SUPABASE_ANON_KEY`
   - `ACTIVATION_LINK_BASE=https://iptv.afifistudio.com` (optional; this is the default)
3. Apply schema migrations:
   ```bash
   supabase db push
   ```
   Or run these in the SQL editor, in order:
   - [`migrations/001_initial_schema.sql`](migrations/001_initial_schema.sql)
   - [`migrations/002_fix_activation_rls.sql`](migrations/002_fix_activation_rls.sql)
   - [`migrations/003_activation_expires_server_default.sql`](migrations/003_activation_expires_server_default.sql)
   - [`migrations/004_watch_history_series_id.sql`](migrations/004_watch_history_series_id.sql)
   - [`migrations/005_harden_activation_and_indexes.sql`](migrations/005_harden_activation_and_indexes.sql)
   - [`migrations/006_activation_housekeeping.sql`](migrations/006_activation_housekeeping.sql)
   - [`migrations/007_enable_realtime_user_data.sql`](migrations/007_enable_realtime_user_data.sql)

## 2. Auth providers

In **Authentication → Providers**:

- **Email** — enabled
- **Google** — enabled; add the Supabase callback URL to your Google Cloud OAuth client

In **Authentication → URL Configuration**:

| Setting | Value |
|---------|-------|
| Site URL | `https://iptv.afifistudio.com` |
| Redirect URLs | `https://iptv.afifistudio.com`, `https://iptv.afifistudio.com/**` |

## 3. Edge function

Deploy the token exchange function (used by the TV app after approval):

```bash
supabase functions deploy exchange-activation-session
```

The companion website calls the `approve_device_activation` RPC directly with the signed-in user's JWT, so approval needs no edge function.

Flow:

1. **TV** creates a `PENDING` session and shows QR + code. It keeps the session id and a secret `qr_token`.
2. **Companion** user signs in, sees which TV the code belongs to, and taps to approve (RPC marks it `APPROVED`).
3. **TV** polls with `session_id` + `qr_token`, then calls `exchange-activation-session` with the same pair. The function claims the session once and returns tokens.

Deploy order matters: migration `008` and the new edge function change the request shape. Deploy them together with the TV app build that sends `session_id` + `qr_token`. Older app builds can no longer activate.

Old activation rows are purged daily by pg_cron when the extension is enabled (Dashboard > Database > Extensions). Without pg_cron, run `select public.purge_stale_activation_sessions();` from any scheduler.

## 4. Activation companion (production)

Host the static files in [`activation-companion/`](activation-companion/) at **https://iptv.afifistudio.com**.

### Hostinger (shared hosting)

1. Open **File Manager** → `public_html/` (or the folder bound to the domain)
2. Upload:
   - `index.html`
   - `app.js`
   - `styles.css`
   - `config.js` (create from [`config.example.js`](activation-companion/config.example.js))
   - `.htaccess` (security headers)
3. Create `config.js` on the server:

   ```javascript
   window.IPTV_CINEMA_CONFIG = {
     SUPABASE_URL: "https://YOUR_PROJECT.supabase.co",
     SUPABASE_ANON_KEY: "YOUR_PUBLISHABLE_OR_ANON_KEY",
   };
   ```

4. Confirm HTTPS/SSL is active for the domain.
5. After deploy, verify the page loads and activation works end-to-end.

`config.js` is gitignored locally — never commit real keys to the repo.

### Companion features

- Email/password sign-in and sign-up (with email confirmation redirect)
- Google OAuth (PKCE-safe; TV codes use `?activation=` not `?code=`)
- Persists activation code across OAuth redirects (cookie + storage)
- Shows the TV's device name and waits for a tap before calling `approve_device_activation` (never approves on its own after sign-in)
- Drops a saved code after 15 minutes
- Polished cinema-themed UI with step indicator

## 5. End-to-end test

1. Rebuild and install the Android TV app (`./gradlew :app:installDebug`)
2. Launch the app → note the activation code and QR URL (`https://iptv.afifistudio.com?activation=XXXX-XX`)
3. Open that URL on phone/desktop
4. Sign in (email or Google) → page shows **Your TV is linked**
5. TV should sign in automatically within a few seconds
6. Optional: create a new account → confirm email → return to site with saved code

### Troubleshooting

| Symptom | Fix |
|---------|-----|
| Website shows old UI | Re-upload `index.html`, `app.js`, `styles.css`; hard-refresh browser |
| "Invalid or expired code" | Use a fresh code from TV; codes expire after 15 minutes |
| TV says "not approved" after website success | Make sure migration `008`, the new edge function and the new TV app are all deployed |
| TV codes expire immediately | Migration `003` must be applied; TV app must not send client `expires_at` |

## Tables

- `profiles`
- `device_activation_sessions`
- `playlist_sources`
- `favorites`
- `watch_history`
- `user_settings`
- `parental_controls`

## Dev fallback

If `SUPABASE_URL` / `SUPABASE_ANON_KEY` are empty, the app falls back to local-only session flags (Phase 4 behavior) so UI can still be tested without a backend.
