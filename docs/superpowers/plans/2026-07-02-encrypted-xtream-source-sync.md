# Encrypted Xtream Source Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first Focused Premium V1 implementation slice: passphrase-encrypted cloud sync for Xtream credentials, companion-side source entry, and TV-side passphrase unlock.

**Architecture:** Keep full Xtream catalogs local in Room and sync only encrypted source credentials through the existing `playlist_sources.encrypted_credentials` JSONB column. The companion encrypts credentials with a user passphrase before upload; the TV downloads the encrypted blob, asks for the same passphrase, decrypts locally, stores credentials in `LocalCredentialsStore`, and then runs the existing Xtream sync path.

**Tech Stack:** Kotlin, Jetpack Compose for TV, DataStore, Hilt, Supabase Kotlin, Supabase JS, WebCrypto PBKDF2/AES-GCM, JVM `PBKDF2WithHmacSHA256`, existing Gradle/JUnit tests.

---

## Scope Check

The approved Focused Premium V1 spec covers multiple subsystems. This plan implements only the source-security/onboarding slice. Separate plans should cover V1 scope cleanup, player release hardening, Arabic-first QA, and Play release packaging.

This plan does not add M3U, parental controls, billing, admin console, full EPG grid, or provider catalog storage in Supabase.

## File Structure

- Modify `app/src/main/java/com/iptvcinema/tv/core/supabase/dto/SupabaseDtos.kt`
  - Add nullable `kdf` and `salt` fields to `EncryptedCredentialsDto`.
- Modify `app/src/main/java/com/iptvcinema/tv/core/supabase/security/CloudCredentialsCipher.kt`
  - Replace user-id-derived cloud encryption with passphrase-derived v2 encryption.
  - Keep v1 decrypt support only for existing app-managed payloads.
- Modify `app/src/test/java/com/iptvcinema/tv/core/supabase/security/CloudCredentialsCipherTest.kt`
  - Cover passphrase round trip, wrong passphrase rejection, v2 payload shape, and legacy v1 rejection behavior.
- Modify `app/src/main/java/com/iptvcinema/tv/core/data/repository/PlaylistSourcesRepository.kt`
  - Add source unlock methods.
- Modify `app/src/main/java/com/iptvcinema/tv/core/data/repository/supabase/SupabasePlaylistSourcesRepository.kt`
  - Stop decrypting cloud credentials without a passphrase.
  - Add `unlockXtreamSource(sourceId, passphrase)`.
  - Save direct-TV Xtream credentials with a passphrase only if that debug/fallback path remains visible.
- Modify `app/src/main/java/com/iptvcinema/tv/core/datastore/AppPreferences.kt`
  - Add `SOURCE_CREDENTIALS_LOCKED`.
- Modify `app/src/main/java/com/iptvcinema/tv/core/datastore/AppSessionState.kt`
  - Add locked-source startup routing.
- Modify `app/src/main/java/com/iptvcinema/tv/core/datastore/AppSessionRepository.kt`
  - Persist and clear locked-source state.
- Modify `app/src/main/java/com/iptvcinema/tv/core/datastore/StartupSessionBootstrap.kt`
  - Restore active cloud source as locked when local credentials are missing.
- Modify `app/src/test/java/com/iptvcinema/tv/core/datastore/AppSessionStateTest.kt`
  - Cover locked-source route and requirements.
- Modify `app/src/main/java/com/iptvcinema/tv/core/navigation/AppRoute.kt`
  - Add `UNLOCK_SOURCE`.
- Modify `app/src/main/java/com/iptvcinema/tv/core/navigation/AppNavGraph.kt`
  - Register the unlock route.
- Create `app/src/main/java/com/iptvcinema/tv/features/sources/SourceUnlockViewModel.kt`
  - Own passphrase unlock and initial Xtream sync.
- Create `app/src/main/java/com/iptvcinema/tv/features/sources/SourceUnlockScreen.kt`
  - TV passphrase entry UI.
- Modify `app/src/main/res/values/strings.xml`
  - Add unlock-source English strings.
- Modify `app/src/main/res/values-ar/strings.xml`
  - Add unlock-source Arabic strings.
- Modify `supabase/activation-companion/index.html`
  - Add Xtream source/passphrase form after sign-in.
- Modify `supabase/activation-companion/app.js`
  - Add WebCrypto encryption and source save before TV approval.
- Modify `supabase/activation-companion/i18n.js`
  - Add companion source/passphrase copy in English and Arabic.
- Modify `supabase/activation-companion/styles.css`
  - Style the source form using the current card system.

## Task 1: Make Cloud Credentials Passphrase-Based

**Files:**
- Modify: `app/src/main/java/com/iptvcinema/tv/core/supabase/dto/SupabaseDtos.kt`
- Modify: `app/src/main/java/com/iptvcinema/tv/core/supabase/security/CloudCredentialsCipher.kt`
- Test: `app/src/test/java/com/iptvcinema/tv/core/supabase/security/CloudCredentialsCipherTest.kt`

- [ ] **Step 1: Write failing v2 cipher tests**

Replace `CloudCredentialsCipherTest.kt` with:

```kotlin
package com.iptvcinema.tv.core.supabase.security

import com.iptvcinema.tv.core.model.XtreamCredentials
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CloudCredentialsCipherTest {
    private val cipher = CloudCredentialsCipher(Json { ignoreUnknownKeys = true })
    private val passphrase = "correct horse battery staple"

    @Test
    fun encryptXtream_roundTripsWithPassphrase() {
        val original = XtreamCredentials(
            serverUrl = "http://example.com:8080",
            username = "demo",
            password = "secret",
            accountName = "Home IPTV",
        )

        val encrypted = cipher.encryptXtream(passphrase, original)
        val restored = cipher.decryptXtream(passphrase, encrypted)

        assertNotNull(restored)
        assertEquals(original, restored)
    }

    @Test
    fun encryptXtream_usesRandomSaltAndIv() {
        val credentials = XtreamCredentials(
            serverUrl = "http://example.com:8080",
            username = "demo",
            password = "secret",
            accountName = "Home IPTV",
        )

        val first = cipher.encryptXtream(passphrase, credentials)
        val second = cipher.encryptXtream(passphrase, credentials)

        assertNotEquals(first.salt, second.salt)
        assertNotEquals(first.iv, second.iv)
        assertNotEquals(first.data, second.data)
    }

    @Test
    fun decryptXtream_rejectsWrongPassphrase() {
        val encrypted = cipher.encryptXtream(
            passphrase,
            XtreamCredentials(
                serverUrl = "http://example.com:8080",
                username = "demo",
                password = "secret",
                accountName = "Home IPTV",
            ),
        )

        assertNull(cipher.decryptXtream("wrong passphrase", encrypted))
    }

    @Test
    fun encryptedPayload_usesVersionTwoPassphraseShape() {
        val encrypted = cipher.encryptXtream(
            passphrase,
            XtreamCredentials(
                serverUrl = "http://example.com:8080",
                username = "demo",
                password = "secret",
                accountName = "Home IPTV",
            ),
        )

        assertEquals(2, encrypted.v)
        assertEquals("XTREAM_CODES", encrypted.type)
        assertEquals("PBKDF2WithHmacSHA256", encrypted.kdf)
        assertNotNull(encrypted.salt)
        assertNotNull(encrypted.iv)
        assertNotNull(encrypted.data)
    }
}
```

- [ ] **Step 2: Run cipher test to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.iptvcinema.tv.core.supabase.security.CloudCredentialsCipherTest"
```

Expected: FAIL because `encryptXtream(passphrase, credentials)`, `salt`, and `kdf` are not implemented in the current v1 cipher.

- [ ] **Step 3: Extend encrypted DTO shape**

In `SupabaseDtos.kt`, replace `EncryptedCredentialsDto` with:

```kotlin
@Serializable
data class EncryptedCredentialsDto(
    val v: Int = 2,
    val type: String,
    val iv: String,
    val data: String,
    val kdf: String? = null,
    val salt: String? = null,
)
```

- [ ] **Step 4: Implement passphrase-derived v2 encryption**

In `CloudCredentialsCipher.kt`, keep the payload data classes and replace the public encrypt/decrypt functions plus private crypto helpers with:

```kotlin
fun encryptXtream(passphrase: String, credentials: XtreamCredentials): EncryptedCredentialsDto {
    val payload = XtreamPayload(
        serverUrl = credentials.serverUrl,
        username = credentials.username,
        password = credentials.password,
        accountName = credentials.accountName,
    )
    return encrypt(passphrase, TYPE_XTREAM, json.encodeToString(payload))
}

fun decryptXtream(passphrase: String, encrypted: EncryptedCredentialsDto): XtreamCredentials? {
    if (encrypted.type != TYPE_XTREAM) return null
    return runCatching {
        json.decodeFromString<XtreamPayload>(decrypt(passphrase, encrypted)).toDomain()
    }.getOrNull()
}

private fun encrypt(passphrase: String, type: String, plaintext: String): EncryptedCredentialsDto {
    require(passphrase.isNotBlank()) { "Passphrase is required" }
    val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
    val iv = ByteArray(GCM_IV_BYTES).also { secureRandom.nextBytes(it) }
    val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
    val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    return EncryptedCredentialsDto(
        v = 2,
        type = type,
        iv = Base64.getEncoder().encodeToString(iv),
        data = Base64.getEncoder().encodeToString(ciphertext),
        kdf = KDF,
        salt = Base64.getEncoder().encodeToString(salt),
    )
}

private fun decrypt(passphrase: String, encrypted: EncryptedCredentialsDto): String {
    require(encrypted.v == 2) { "Unsupported encrypted credentials version" }
    require(encrypted.kdf == KDF) { "Unsupported encrypted credentials KDF" }
    val salt = Base64.getDecoder().decode(requireNotNull(encrypted.salt))
    val iv = Base64.getDecoder().decode(encrypted.iv)
    val ciphertext = Base64.getDecoder().decode(encrypted.data)
    val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
    return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
}

private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
    val keyFactory = SecretKeyFactory.getInstance(KDF)
    val spec = PBEKeySpec(passphrase.toCharArray(), salt, KDF_ITERATIONS, KEY_BITS)
    return SecretKeySpec(keyFactory.generateSecret(spec).encoded, "AES")
}
```

Update imports:

```kotlin
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
```

Update companion constants:

```kotlin
private const val TYPE_XTREAM = "XTREAM_CODES"
private const val KDF = "PBKDF2WithHmacSHA256"
private const val KDF_ITERATIONS = 120_000
private const val KEY_BITS = 256
private const val SALT_BYTES = 16
private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_IV_BYTES = 12
private const val GCM_TAG_BITS = 128
private val secureRandom = SecureRandom()
```

Keep the existing M3U methods compiling as legacy app-managed encryption until the separate V1 scope cleanup removes M3U from the product surface. Add these methods back below the Xtream methods if the file no longer has them after the refactor:

```kotlin
fun encryptM3u(userId: String, credentials: M3uCredentials): EncryptedCredentialsDto {
    val payload = M3uPayload(
        playlistUrl = credentials.playlistUrl,
        epgUrl = credentials.epgUrl,
        playlistName = credentials.playlistName,
        userAgent = credentials.userAgent,
        referer = credentials.referer,
        customHeaders = credentials.customHeaders,
    )
    return encryptLegacy(userId, TYPE_M3U, json.encodeToString(payload))
}

fun decryptM3u(userId: String, encrypted: EncryptedCredentialsDto): M3uCredentials? {
    if (encrypted.type != TYPE_M3U || encrypted.v != 1) return null
    return runCatching {
        json.decodeFromString<M3uPayload>(decryptLegacy(userId, encrypted)).toDomain()
    }.getOrNull()
}

private fun encryptLegacy(userId: String, type: String, plaintext: String): EncryptedCredentialsDto {
    val iv = ByteArray(GCM_IV_BYTES).also { secureRandom.nextBytes(it) }
    val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, deriveLegacyKey(userId), GCMParameterSpec(GCM_TAG_BITS, iv))
    val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    return EncryptedCredentialsDto(
        v = 1,
        type = type,
        iv = Base64.getEncoder().encodeToString(iv),
        data = Base64.getEncoder().encodeToString(ciphertext),
    )
}

private fun decryptLegacy(userId: String, encrypted: EncryptedCredentialsDto): String {
    val iv = Base64.getDecoder().decode(encrypted.iv)
    val ciphertext = Base64.getDecoder().decode(encrypted.data)
    val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, deriveLegacyKey(userId), GCMParameterSpec(GCM_TAG_BITS, iv))
    return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
}

private fun deriveLegacyKey(userId: String): SecretKeySpec {
    val digest = MessageDigest.getInstance("SHA-256")
    val keyMaterial = digest.digest("$userId:$LEGACY_KEY_SALT".toByteArray(Charsets.UTF_8))
    return SecretKeySpec(keyMaterial, "AES")
}
```

Keep these constants for legacy M3U compatibility:

```kotlin
private const val TYPE_M3U = "M3U"
private const val LEGACY_KEY_SALT = "iptv-cinema-cloud-credentials-v1"
```

- [ ] **Step 5: Run cipher test to verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.iptvcinema.tv.core.supabase.security.CloudCredentialsCipherTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/iptvcinema/tv/core/supabase/dto/SupabaseDtos.kt app/src/main/java/com/iptvcinema/tv/core/supabase/security/CloudCredentialsCipher.kt app/src/test/java/com/iptvcinema/tv/core/supabase/security/CloudCredentialsCipherTest.kt
git commit -m "Use passphrase encryption for cloud Xtream credentials"
```

## Task 2: Add Locked Source Session Routing

**Files:**
- Modify: `app/src/main/java/com/iptvcinema/tv/core/datastore/AppPreferences.kt`
- Modify: `app/src/main/java/com/iptvcinema/tv/core/datastore/AppSessionState.kt`
- Modify: `app/src/main/java/com/iptvcinema/tv/core/datastore/AppSessionRepository.kt`
- Modify: `app/src/main/java/com/iptvcinema/tv/core/navigation/AppRoute.kt`
- Test: `app/src/test/java/com/iptvcinema/tv/core/datastore/AppSessionStateTest.kt`

- [ ] **Step 1: Write failing session tests**

Add these tests to `AppSessionStateTest.kt`:

```kotlin
@Test
fun resolveStartupDestination_lockedSource_returnsUnlockSource() {
    val state = AppSessionState(
        isAuthenticated = true,
        hasSource = true,
        sourceCredentialsLocked = true,
        currentSourceId = "source-1",
        sourceType = SourceType.XTREAM_CODES,
    )

    assertEquals(StartupDestination.UnlockSource, state.resolveStartupDestination())
}

@Test
fun meetsRequirement_hasSourceFailsWhenCredentialsLocked() {
    val state = AppSessionState(
        isAuthenticated = true,
        hasSource = true,
        sourceCredentialsLocked = true,
        currentSourceId = "source-1",
        sourceType = SourceType.XTREAM_CODES,
    )

    assertFalse(state.meetsRequirement(SessionRequirement.HasSource))
    assertFalse(state.meetsRequirement(SessionRequirement.Ready))
}
```

- [ ] **Step 2: Run session tests to verify they fail**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.iptvcinema.tv.core.datastore.AppSessionStateTest"
```

Expected: FAIL because `sourceCredentialsLocked` and `StartupDestination.UnlockSource` do not exist.

- [ ] **Step 3: Add the locked-source preference**

In `AppPreferences.kt`, add:

```kotlin
val SOURCE_CREDENTIALS_LOCKED = booleanPreferencesKey("source_credentials_locked")
```

- [ ] **Step 4: Add locked-source session state**

In `AppSessionState.kt`, add the property:

```kotlin
val sourceCredentialsLocked: Boolean = false,
```

Update startup resolution:

```kotlin
fun resolveStartupDestination(): StartupDestination = when {
    !isAuthenticated -> StartupDestination.Activation
    !hasSource -> StartupDestination.AddSource
    sourceCredentialsLocked -> StartupDestination.UnlockSource
    currentProfileId == null -> StartupDestination.ProfileSelection
    else -> StartupDestination.Home
}
```

Update requirements:

```kotlin
fun meetsRequirement(requirement: SessionRequirement): Boolean = when (requirement) {
    SessionRequirement.None -> true
    SessionRequirement.Authenticated -> isAuthenticated
    SessionRequirement.HasSource -> isAuthenticated && hasSource && !sourceCredentialsLocked
    SessionRequirement.Ready -> isAuthenticated && hasSource && !sourceCredentialsLocked && currentProfileId != null
}
```

Add startup destination:

```kotlin
data object UnlockSource : StartupDestination()
```

Update `route()`:

```kotlin
StartupDestination.UnlockSource -> com.iptvcinema.tv.core.navigation.AppRoute.UNLOCK_SOURCE
```

- [ ] **Step 5: Persist locked-source state**

In `AppSessionRepository.kt`, read the new preference into `AppSessionState`:

```kotlin
sourceCredentialsLocked = preferences[AppPreferences.SOURCE_CREDENTIALS_LOCKED] ?: false,
```

Update `setSource` signature:

```kotlin
suspend fun setSource(
    sourceId: String,
    sourceType: SourceType,
    isDemoMode: Boolean = sourceType == SourceType.DEMO,
    credentialsLocked: Boolean = false,
)
```

Inside `setSource`, add:

```kotlin
preferences[AppPreferences.SOURCE_CREDENTIALS_LOCKED] = credentialsLocked
```

Inside `clearSource`, add:

```kotlin
preferences[AppPreferences.SOURCE_CREDENTIALS_LOCKED] = false
```

- [ ] **Step 6: Add unlock route constant**

In `AppRoute.kt`, add:

```kotlin
const val UNLOCK_SOURCE = "unlock_source"
```

- [ ] **Step 7: Run session tests to verify they pass**

Run after Task 3 Step 4:

```bash
./gradlew :app:testDebugUnitTest --tests "com.iptvcinema.tv.core.datastore.AppSessionStateTest"
```

Expected: PASS.

- [ ] **Step 8: Compile changed session code**

Run:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/iptvcinema/tv/core/datastore/AppPreferences.kt app/src/main/java/com/iptvcinema/tv/core/datastore/AppSessionState.kt app/src/main/java/com/iptvcinema/tv/core/datastore/AppSessionRepository.kt app/src/main/java/com/iptvcinema/tv/core/navigation/AppRoute.kt app/src/test/java/com/iptvcinema/tv/core/datastore/AppSessionStateTest.kt
git commit -m "Route locked cloud sources to passphrase unlock"
```

## Task 3: Add Repository Unlock API

**Files:**
- Modify: `app/src/main/java/com/iptvcinema/tv/core/data/repository/PlaylistSourcesRepository.kt`
- Modify: `app/src/main/java/com/iptvcinema/tv/core/data/repository/supabase/SupabasePlaylistSourcesRepository.kt`
- Modify: `app/src/main/java/com/iptvcinema/tv/core/datastore/StartupSessionBootstrap.kt`
- Modify: `app/src/main/java/com/iptvcinema/tv/features/sources/SourceViewModel.kt`
- Modify: `app/src/main/java/com/iptvcinema/tv/features/sources/SourceScreens.kt`
- Modify: `app/src/main/java/com/iptvcinema/tv/core/navigation/AppNavGraph.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-ar/strings.xml`

- [ ] **Step 1: Change repository contract**

In `PlaylistSourcesRepository.kt`, update signatures:

```kotlin
suspend fun saveXtreamSource(credentials: XtreamCredentials, passphrase: String): PlaylistSourceRecord
suspend fun activateXtreamSource(sourceId: String, credentials: XtreamCredentials, passphrase: String): PlaylistSourceRecord
suspend fun unlockXtreamSource(sourceId: String, passphrase: String): XtreamCredentials?
suspend fun ensureLocalCredentials(source: PlaylistSourceRecord): Boolean
```

Keep `saveM3uSource` for existing code until the separate V1 scope cleanup plan removes the visible M3U path.

- [ ] **Step 2: Update Supabase Xtream save and activation**

In `SupabasePlaylistSourcesRepository.kt`, change `saveXtreamSource` to accept `passphrase` and insert no readable server URL:

```kotlin
override suspend fun saveXtreamSource(
    credentials: XtreamCredentials,
    passphrase: String,
): PlaylistSourceRecord {
    val userId = requireUserId()
    deactivateAllSources(userId)
    val insert = PlaylistSourceInsertDto(
        userId = userId,
        name = credentials.accountName.ifBlank { "Xtream Codes" },
        type = SourceType.XTREAM_CODES.name,
        serverUrl = null,
        isActive = true,
        status = SourceStatus.ACTIVE.name,
        encryptedCredentials = cloudCredentialsCipher.encryptXtream(passphrase, credentials),
    )
    val saved = supabaseClient.from(TABLE)
        .insert(insert) {
            select(Columns.ALL)
        }
        .decodeSingle<PlaylistSourceDto>()
    localCredentialsStore.saveXtreamCredentials(saved.id, credentials)
    return saved.toDomain()
}
```

Add `encryptedCredentials` to `PlaylistSourceInsertDto`:

```kotlin
@SerialName("encrypted_credentials") val encryptedCredentials: EncryptedCredentialsDto? = null,
```

Update `activateXtreamSource`:

```kotlin
override suspend fun activateXtreamSource(
    sourceId: String,
    credentials: XtreamCredentials,
    passphrase: String,
): PlaylistSourceRecord {
    val userId = requireUserId()
    deactivateAllSources(userId)
    supabaseClient.from(TABLE)
        .update(
            PlaylistSourceActivationUpdateDto(
                isActive = true,
                status = SourceStatus.ACTIVE.name,
                encryptedCredentials = cloudCredentialsCipher.encryptXtream(passphrase, credentials),
            ),
        ) {
            filter {
                eq(COLUMN_ID, sourceId)
                eq(COLUMN_USER_ID, userId)
            }
        }
    localCredentialsStore.saveXtreamCredentials(sourceId, credentials)
    return getSources().first { it.id == sourceId }
}
```

- [ ] **Step 3: Add passphrase unlock**

In `SupabasePlaylistSourcesRepository.kt`, add:

```kotlin
override suspend fun unlockXtreamSource(sourceId: String, passphrase: String): XtreamCredentials? {
    if (passphrase.isBlank()) return null
    val dto = fetchSourceDto(sourceId) ?: return null
    val credentials = dto.encryptedCredentials
        ?.let { cloudCredentialsCipher.decryptXtream(passphrase, it) }
        ?: return null
    localCredentialsStore.saveXtreamCredentials(sourceId, credentials)
    return credentials
}
```

- [ ] **Step 4: Make startup credential restore return locked/unlocked**

Replace `ensureLocalCredentials` in `SupabasePlaylistSourcesRepository.kt`:

```kotlin
override suspend fun ensureLocalCredentials(source: PlaylistSourceRecord): Boolean {
    return when (source.type) {
        SourceType.DEMO -> true
        SourceType.XTREAM_CODES -> localCredentialsStore.getXtreamCredentials(source.id) != null
        SourceType.M3U -> {
            if (localCredentialsStore.getM3uCredentials(source.id) != null) return true
            hydrateM3uFromMetadata(source)
            localCredentialsStore.getM3uCredentials(source.id) != null
        }
    }
}
```

This intentionally does not decrypt Xtream cloud credentials during startup; passphrase unlock owns that.

- [ ] **Step 5: Restore active cloud sources as locked**

In `StartupSessionBootstrap.kt`, update the active-source restore branch:

```kotlin
val unlocked = playlistSourcesRepository.ensureLocalCredentials(activeSource)
appSessionRepository.setSource(
    sourceId = activeSource.id,
    sourceType = activeSource.type,
    isDemoMode = activeSource.type == SourceType.DEMO,
    credentialsLocked = activeSource.type == SourceType.XTREAM_CODES && !unlocked,
)
```

Update `validateExistingSource`:

```kotlin
val unlocked = playlistSourcesRepository.ensureLocalCredentials(source)
if (source.type == SourceType.XTREAM_CODES && !unlocked) {
    appSessionRepository.setSource(
        sourceId = source.id,
        sourceType = source.type,
        isDemoMode = false,
        credentialsLocked = true,
    )
    return
}
```

Leave the existing `canRestoreSource(source)` check after this block for unlocked sources.

- [ ] **Step 6: Update direct TV form callers**

In `SourceViewModel.kt`, the current direct TV form has no passphrase. Add a temporary passphrase parameter to `connectXtreamSource`:

```kotlin
fun connectXtreamSource(
    credentials: XtreamCredentials,
    passphrase: String,
    onComplete: () -> Unit,
)
```

Update repository calls:

```kotlin
playlistSourcesRepository.activateXtreamSource(existing.id, credentials, passphrase)
playlistSourcesRepository.saveXtreamSource(credentials, passphrase)
```

- [ ] **Step 7: Add passphrase field to the direct TV form**

In `XtreamFormScreen`, add state:

```kotlin
var passphrase by remember { mutableStateOf("") }
```

Add the field after account name:

```kotlin
CinemaTextField(
    value = passphrase,
    onValueChange = { passphrase = it },
    label = stringResource(R.string.field_source_passphrase),
    isPassword = true,
)
```

Update the composable signature:

```kotlin
onConnect: (XtreamCredentials, String) -> Unit,
```

Update click handler:

```kotlin
onConnect(
    XtreamCredentials(
        serverUrl = serverUrl.trim(),
        username = username.trim(),
        password = password,
        accountName = accountName.trim(),
    ),
    passphrase,
)
```

Update the `AppNavGraph.kt` `XTREAM_FORM` call:

```kotlin
onConnect = { credentials, passphrase ->
    viewModel.connectXtreamSource(credentials, passphrase) {
        navController.navigateOnboardingClearingStack(AppRoute.profileSelection())
    }
}
```

Add to `values/strings.xml`:

```xml
<string name="field_source_passphrase">Source passphrase</string>
```

Add to `values-ar/strings.xml`:

```xml
<string name="field_source_passphrase">عبارة مرور المصدر</string>
```

- [ ] **Step 8: Run compile to catch signature misses**

Run:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: PASS after updating all changed signatures.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/iptvcinema/tv/core/data/repository/PlaylistSourcesRepository.kt app/src/main/java/com/iptvcinema/tv/core/data/repository/supabase/SupabasePlaylistSourcesRepository.kt app/src/main/java/com/iptvcinema/tv/core/datastore/StartupSessionBootstrap.kt app/src/main/java/com/iptvcinema/tv/features/sources/SourceViewModel.kt app/src/main/java/com/iptvcinema/tv/features/sources/SourceScreens.kt app/src/main/java/com/iptvcinema/tv/core/navigation/AppNavGraph.kt app/src/main/res/values/strings.xml app/src/main/res/values-ar/strings.xml
git commit -m "Add passphrase unlock for cloud Xtream sources"
```

## Task 4: Add TV Unlock Screen

**Files:**
- Create: `app/src/main/java/com/iptvcinema/tv/features/sources/SourceUnlockViewModel.kt`
- Create: `app/src/main/java/com/iptvcinema/tv/features/sources/SourceUnlockScreen.kt`
- Modify: `app/src/main/java/com/iptvcinema/tv/core/navigation/AppNavGraph.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-ar/strings.xml`

- [ ] **Step 1: Create unlock ViewModel**

Create `SourceUnlockViewModel.kt`:

```kotlin
package com.iptvcinema.tv.features.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.data.repository.PlaylistSourcesRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.xtream.XtreamSyncRepository
import com.iptvcinema.tv.core.xtream.XtreamSyncResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SourceUnlockUiState(
    val isUnlocking: Boolean = false,
    val sourceName: String = "Xtream Codes",
    val errorMessage: String? = null,
)

@HiltViewModel
class SourceUnlockViewModel @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val playlistSourcesRepository: PlaylistSourcesRepository,
    private val xtreamSyncRepository: XtreamSyncRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SourceUnlockUiState())
    val uiState: StateFlow<SourceUnlockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val state = appSessionRepository.sessionState.first()
            val source = playlistSourcesRepository.getSources()
                .firstOrNull { it.id == state.currentSourceId }
                ?: return@launch
            _uiState.value = _uiState.value.copy(sourceName = source.name)
        }
    }

    fun unlock(passphrase: String, onUnlocked: () -> Unit) {
        viewModelScope.launch {
            val sourceId = appSessionRepository.sessionState.first().currentSourceId
            if (sourceId.isNullOrBlank()) {
                _uiState.value = SourceUnlockUiState(errorMessage = "Source not found")
                return@launch
            }
            if (passphrase.isBlank()) {
                _uiState.value = _uiState.value.copy(errorMessage = "Enter your source passphrase")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isUnlocking = true, errorMessage = null)
            val credentials = playlistSourcesRepository.unlockXtreamSource(sourceId, passphrase)
            if (credentials == null) {
                _uiState.value = _uiState.value.copy(
                    isUnlocking = false,
                    errorMessage = "Passphrase is incorrect",
                )
                return@launch
            }

            appSessionRepository.setSource(
                sourceId = sourceId,
                sourceType = SourceType.XTREAM_CODES,
                isDemoMode = false,
                credentialsLocked = false,
            )

            when (val syncResult = xtreamSyncRepository.syncSourceIfNeeded(sourceId, credentials)) {
                is XtreamSyncResult.Success -> onUnlocked()
                is XtreamSyncResult.AuthFailed -> _uiState.value = _uiState.value.copy(
                    isUnlocking = false,
                    errorMessage = syncResult.message,
                )
                is XtreamSyncResult.Failed -> _uiState.value = _uiState.value.copy(
                    isUnlocking = false,
                    errorMessage = syncResult.message,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Create unlock screen**

Create `SourceUnlockScreen.kt`:

```kotlin
package com.iptvcinema.tv.features.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.CinemaLogo
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.components.CinemaTextField
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.navigation.OnboardingBackHandler
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SourceUnlockScreen(
    state: SourceUnlockUiState,
    onUnlock: (String) -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    val firstFieldFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("source_unlock")

    LaunchedEffect(focusState.hasSavedFocus) {
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(firstFieldFocus)
        } else {
            focusState.requestInitialFocus(firstFieldFocus)
            focusState.saveFocusIndex(0)
        }
    }

    OnboardingBackHandler(allowBack = false)

    CinemaScreen(showTopNav = false) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
            ) {
                CinemaLogo()
                Text(
                    text = stringResource(R.string.unlock_source_title),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = stringResource(R.string.unlock_source_desc, state.sourceName),
                    style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
                )
                CinemaTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = stringResource(R.string.unlock_source_passphrase),
                    isPassword = true,
                    modifier = Modifier.focusRequester(firstFieldFocus),
                )
                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.Danger),
                    )
                }
                CinemaButton(
                    text = if (state.isUnlocking) {
                        stringResource(R.string.unlock_source_unlocking)
                    } else {
                        stringResource(R.string.unlock_source_button)
                    },
                    variant = CinemaButtonVariant.PrimaryAccent,
                    enabled = !state.isUnlocking,
                    onClick = { onUnlock(passphrase) },
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
            ) {
                Text(
                    text = stringResource(R.string.unlock_source_privacy_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = stringResource(R.string.unlock_source_privacy_desc),
                    style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
                )
            }
        }
    }
}
```

- [ ] **Step 3: Add strings**

Add to `values/strings.xml`:

```xml
<string name="unlock_source_title">Unlock your source</string>
<string name="unlock_source_desc">Enter the passphrase for %1$s to restore your IPTV source on this TV.</string>
<string name="unlock_source_passphrase">Source passphrase</string>
<string name="unlock_source_button">Unlock and sync</string>
<string name="unlock_source_unlocking">Unlocking…</string>
<string name="unlock_source_privacy_title">Private by design</string>
<string name="unlock_source_privacy_desc">Your Xtream credentials are encrypted before they reach IPTV Cinema. This TV can only restore them with your passphrase.</string>
```

Add to `values-ar/strings.xml`:

```xml
<string name="unlock_source_title">افتح المصدر</string>
<string name="unlock_source_desc">أدخل عبارة المرور الخاصة بـ %1$s لاستعادة مصدر IPTV على هذا التلفاز.</string>
<string name="unlock_source_passphrase">عبارة مرور المصدر</string>
<string name="unlock_source_button">فتح ومزامنة</string>
<string name="unlock_source_unlocking">جارٍ الفتح…</string>
<string name="unlock_source_privacy_title">خصوصية من البداية</string>
<string name="unlock_source_privacy_desc">يتم تشفير بيانات Xtream قبل وصولها إلى IPTV Cinema. لا يستطيع هذا التلفاز استعادتها إلا بعبارة المرور الخاصة بك.</string>
```

- [ ] **Step 4: Register unlock route**

In `AppNavGraph.kt`, add imports:

```kotlin
import com.iptvcinema.tv.features.sources.SourceUnlockScreen
import com.iptvcinema.tv.features.sources.SourceUnlockViewModel
```

Add a composable before profile selection:

```kotlin
composable(AppRoute.UNLOCK_SOURCE) {
    val viewModel: SourceUnlockViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()

    SessionRouteGuard(navController = navController, requirement = SessionRequirement.Authenticated) {
        SourceUnlockScreen(
            state = state,
            onUnlock = { passphrase ->
                viewModel.unlock(passphrase) {
                    navController.navigateOnboardingClearingStack(AppRoute.profileSelection())
                }
            },
        )
    }
}
```

- [ ] **Step 5: Run compile**

Run:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/iptvcinema/tv/features/sources/SourceUnlockViewModel.kt app/src/main/java/com/iptvcinema/tv/features/sources/SourceUnlockScreen.kt app/src/main/java/com/iptvcinema/tv/core/navigation/AppNavGraph.kt app/src/main/res/values/strings.xml app/src/main/res/values-ar/strings.xml
git commit -m "Add TV passphrase unlock for cloud sources"
```

## Task 5: Add Companion Xtream Source Encryption

**Files:**
- Modify: `supabase/activation-companion/index.html`
- Modify: `supabase/activation-companion/app.js`
- Modify: `supabase/activation-companion/i18n.js`
- Modify: `supabase/activation-companion/styles.css`

- [ ] **Step 1: Add source form markup**

In `index.html`, add this block inside `form#auth-form`, after the auth buttons and before `button#link-tv`:

```html
<section id="source-fields" class="source-fields hidden" aria-labelledby="source-title">
  <h2 id="source-title" data-i18n="source_title">Add Xtream source</h2>
  <p class="hint" data-i18n="source_hint">Your Xtream details are encrypted in this browser before upload.</p>

  <div class="field">
    <label for="source-name" data-i18n="label_source_name">Source name</label>
    <input id="source-name" name="source-name" autocomplete="off" value="Home IPTV" />
  </div>

  <div class="field">
    <label for="server-url" data-i18n="label_server_url">Server URL</label>
    <input id="server-url" name="server-url" type="url" autocomplete="url" placeholder="http://example.com:8080" />
  </div>

  <div class="field">
    <label for="xtream-username" data-i18n="label_xtream_username">Xtream username</label>
    <input id="xtream-username" name="xtream-username" autocomplete="username" />
  </div>

  <div class="field">
    <label for="xtream-password" data-i18n="label_xtream_password">Xtream password</label>
    <input id="xtream-password" name="xtream-password" type="password" autocomplete="current-password" />
  </div>

  <div class="field">
    <label for="source-passphrase" data-i18n="label_source_passphrase">Source passphrase</label>
    <input id="source-passphrase" name="source-passphrase" type="password" autocomplete="new-password" />
  </div>
</section>
```

- [ ] **Step 2: Wire new DOM elements**

In `app.js`, add to `els`:

```js
sourceFields: $("source-fields"),
sourceNameInput: $("source-name"),
serverUrlInput: $("server-url"),
xtreamUsernameInput: $("xtream-username"),
xtreamPasswordInput: $("xtream-password"),
sourcePassphraseInput: $("source-passphrase"),
```

Update `showBanner(session)` so signed-in users see the source form:

```js
els.sourceFields?.classList.toggle("hidden", !signedIn);
```

Update `setBusy(on)` to include the new inputs:

```js
for (const el of [
  els.signInBtn,
  els.signUpBtn,
  els.googleBtn,
  els.linkTvBtn,
  els.codeInput,
  els.emailInput,
  els.passwordInput,
  els.sourceNameInput,
  els.serverUrlInput,
  els.xtreamUsernameInput,
  els.xtreamPasswordInput,
  els.sourcePassphraseInput,
]) {
  if (el) el.disabled = on;
}
```

- [ ] **Step 3: Add WebCrypto helpers**

Add these helpers to `app.js`:

```js
const te = new TextEncoder();

function randomBytes(length) {
  const bytes = new Uint8Array(length);
  crypto.getRandomValues(bytes);
  return bytes;
}

function b64(bytes) {
  return btoa(String.fromCharCode(...new Uint8Array(bytes)));
}

async function deriveKey(passphrase, salt) {
  const baseKey = await crypto.subtle.importKey(
    "raw",
    te.encode(passphrase),
    "PBKDF2",
    false,
    ["deriveKey"],
  );
  return crypto.subtle.deriveKey(
    {
      name: "PBKDF2",
      salt,
      iterations: 120000,
      hash: "SHA-256",
    },
    baseKey,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt"],
  );
}

async function encryptXtreamCredentials(passphrase, credentials) {
  const salt = randomBytes(16);
  const iv = randomBytes(12);
  const key = await deriveKey(passphrase, salt);
  const payload = JSON.stringify({
    server_url: credentials.serverUrl,
    username: credentials.username,
    password: credentials.password,
    account_name: credentials.accountName,
  });
  const encrypted = await crypto.subtle.encrypt(
    { name: "AES-GCM", iv },
    key,
    te.encode(payload),
  );
  return {
    v: 2,
    type: "XTREAM_CODES",
    kdf: "PBKDF2WithHmacSHA256",
    salt: b64(salt),
    iv: b64(iv),
    data: b64(encrypted),
  };
}
```

- [ ] **Step 4: Save encrypted Xtream source before approval**

Add:

```js
function readXtreamForm() {
  const accountName = els.sourceNameInput.value.trim() || "Xtream Codes";
  const serverUrl = els.serverUrlInput.value.trim();
  const username = els.xtreamUsernameInput.value.trim();
  const password = els.xtreamPasswordInput.value;
  const passphrase = els.sourcePassphraseInput.value;

  if (!serverUrl || !username || !password || !passphrase) {
    throw new Error(t("err_source_required"));
  }

  return {
    accountName,
    serverUrl,
    username,
    password,
    passphrase,
  };
}

async function saveEncryptedXtreamSource(session) {
  const source = readXtreamForm();
  const encrypted = await encryptXtreamCredentials(source.passphrase, source);
  await supabase
    .from("playlist_sources")
    .update({ is_active: false })
    .eq("user_id", session.user.id);

  const { error } = await supabase.from("playlist_sources").insert({
    user_id: session.user.id,
    name: source.accountName,
    type: "XTREAM_CODES",
    encrypted_credentials: encrypted,
    is_active: true,
    status: "ACTIVE",
  });
  if (error) throw error;
}
```

In `approveTv(session)`, before the RPC call, add:

```js
await saveEncryptedXtreamSource(session);
```

- [ ] **Step 5: Add companion translations**

In `i18n.js`, add English keys:

```js
source_title: "Add Xtream source",
source_hint: "Your Xtream details are encrypted in this browser before upload.",
label_source_name: "Source name",
label_server_url: "Server URL",
label_xtream_username: "Xtream username",
label_xtream_password: "Xtream password",
label_source_passphrase: "Source passphrase",
err_source_required: "Source URL, username, password, and passphrase are required.",
```

Add Arabic keys:

```js
source_title: "إضافة مصدر Xtream",
source_hint: "يتم تشفير بيانات Xtream في هذا المتصفح قبل رفعها.",
label_source_name: "اسم المصدر",
label_server_url: "رابط الخادم",
label_xtream_username: "اسم مستخدم Xtream",
label_xtream_password: "كلمة مرور Xtream",
label_source_passphrase: "عبارة مرور المصدر",
err_source_required: "رابط المصدر واسم المستخدم وكلمة المرور وعبارة المرور مطلوبة.",
```

- [ ] **Step 6: Add companion styles**

In `styles.css`, add:

```css
.hidden {
  display: none !important;
}

.source-fields {
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px solid var(--border);
}

.source-fields h2 {
  margin: 0 0 6px;
  color: var(--gold-soft);
  font-size: 1.15rem;
}
```

- [ ] **Step 7: Manual browser check**

Run:

```bash
python3 -m http.server 4177 --directory supabase/activation-companion
```

Open `http://localhost:4177/?activation=ABCD-12`.

Expected:

- Sign-in form renders.
- Source fields appear after sign-in.
- Required-source validation appears before TV approval if fields are empty.
- Arabic locale keeps the page RTL when browser language starts with `ar`.

- [ ] **Step 8: Commit**

```bash
git add supabase/activation-companion/index.html supabase/activation-companion/app.js supabase/activation-companion/i18n.js supabase/activation-companion/styles.css
git commit -m "Add encrypted Xtream source entry to companion"
```

## Task 6: Final Verification

**Files:**
- Verify all files touched in Tasks 1-5.

- [ ] **Step 1: Run focused unit tests**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.iptvcinema.tv.core.supabase.security.CloudCredentialsCipherTest" --tests "com.iptvcinema.tv.core.datastore.AppSessionStateTest"
```

Expected: PASS.

- [ ] **Step 2: Run app compile**

Run:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 3: Run full unit suite**

Run:

```bash
./gradlew :app:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 4: Run debug build**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: PASS and APK produced under `app/build/outputs/apk/debug/`.

- [ ] **Step 5: Run lint**

Run:

```bash
./gradlew :app:lintDebug
```

Expected: PASS.

- [ ] **Step 6: Manual Android TV 9 check**

Install debug build on the Android TV 9 device:

```bash
./gradlew :app:installDebug
```

Check:

- Fresh app opens activation.
- Companion accepts code, sign-in, Xtream details, and source passphrase.
- TV links, detects locked cloud source, and shows unlock screen.
- Wrong passphrase shows an error and stays on unlock screen.
- Correct passphrase unlocks, syncs catalog, and routes to profile selection.
- Returning launch goes to profile selection or Home without asking for passphrase while local encrypted credentials exist.
- Clearing app data returns to activation/unlock flow as expected.
- Source removal deletes local credentials and catalog cache.

- [ ] **Step 7: Final status check**

Run:

```bash
git status --short
```

Expected: only unrelated pre-existing dirty `app/build` outputs or user changes remain. No planned source files should be unstaged.
