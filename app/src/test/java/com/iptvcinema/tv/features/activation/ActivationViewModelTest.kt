package com.iptvcinema.tv.features.activation

import com.iptvcinema.tv.core.data.repository.ActivationStatusSnapshot
import com.iptvcinema.tv.core.data.repository.AuthRepository
import com.iptvcinema.tv.core.data.repository.DeviceActivationRepository
import com.iptvcinema.tv.core.datastore.AppSessionState
import com.iptvcinema.tv.core.datastore.SessionPreparer
import com.iptvcinema.tv.core.datastore.StartupDestination
import com.iptvcinema.tv.core.device.DeviceIdentity
import com.iptvcinema.tv.core.model.ActivationSessionStatus
import com.iptvcinema.tv.core.model.DeviceActivationSession
import androidx.lifecycle.viewModelScope
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val auth = FakeAuthRepository()
    private val repo = FakeActivationRepository(auth)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun pollApprovalAndButtonPress_exchangeRunsOnce() = vmTest {
        repo.statusFor = { ActivationSessionStatus.APPROVED }
        val viewModel = createViewModel()

        runCurrent()
        advanceTimeBy(POLL_START_MS)
        viewModel.checkApprovalNow()
        settle()

        assertEquals(1, repo.exchangeCount)
        assertEquals(ActivationUiState.Succeeded(StartupDestination.AddSource), viewModel.uiState.value)
    }

    @Test
    fun countdownReachingZero_createsNewCode() = vmTest {
        repo.expiresAtFor = { index -> if (index == 0) Instant.now().minusSeconds(1) else Instant.now().plusSeconds(900) }
        val viewModel = createViewModel()

        settle()

        assertEquals(2, repo.createCount)
        val state = viewModel.uiState.value as ActivationUiState.Ready
        assertEquals("CODE-1", state.code)
        assertEquals(ActivationStatus.Waiting, state.status)
    }

    @Test
    fun restart_stopsPollingTheOldSession() = vmTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.startActivation()
        settle()
        repo.polledSessionIds.clear()
        advanceTimeBy(20_000)

        assertTrue(repo.polledSessionIds.isNotEmpty())
        assertTrue(repo.polledSessionIds.all { it == "session-1" })
    }

    @Test
    fun repeatedPollFailures_showConnectionProblem() = vmTest {
        repo.statusError = IOException("offline")
        val viewModel = createViewModel()

        runCurrent()
        advanceTimeBy(30_000)

        val state = viewModel.uiState.value as ActivationUiState.Ready
        assertEquals(ActivationStatus.ConnectionProblem, state.status)
    }

    @Test
    fun createFailure_offline_showsNetworkError() = vmTest {
        repo.createError = IOException("offline")
        val viewModel = createViewModel()

        settle()

        assertEquals(ActivationUiState.Error(ActivationError.Network), viewModel.uiState.value)
    }

    @Test
    fun buttonPressWhilePending_showsNotApprovedYet() = vmTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.checkApprovalNow()
        settle()

        val state = viewModel.uiState.value as ActivationUiState.Ready
        assertEquals(ActivationStatus.NotApprovedYet, state.status)
        assertEquals(0, repo.exchangeCount)
    }

    @Test
    fun failedExchange_showsSignInFailed() = vmTest {
        repo.statusFor = { ActivationSessionStatus.APPROVED }
        repo.exchangeResult = Result.failure(IllegalStateException("claimed"))
        val viewModel = createViewModel()

        runCurrent()
        advanceTimeBy(POLL_START_MS)
        settle()

        val state = viewModel.uiState.value as ActivationUiState.Ready
        assertEquals(ActivationStatus.SignInFailed, state.status)
    }

    private fun TestScope.settle() {
        advanceTimeBy(1_500)
        runCurrent()
    }

    // The poll and countdown loops never end on their own, so each view model's scope is
    // cancelled at the end of the test body; otherwise runTest waits forever for the scheduler to idle.
    private val createdViewModels = mutableListOf<ActivationViewModel>()

    private fun vmTest(block: suspend TestScope.() -> Unit): TestResult = runTest(dispatcher) {
        try {
            block()
        } finally {
            createdViewModels.forEach { it.viewModelScope.cancel() }
        }
    }

    private fun createViewModel(): ActivationViewModel = ActivationViewModel(
        authRepository = auth,
        deviceActivationRepository = repo,
        sessionPreparer = FakeSessionPreparer(auth),
        deviceIdentity = DeviceIdentity { "Test TV" },
    ).also { createdViewModels += it }

    private companion object {
        const val POLL_START_MS = 2_100L
    }
}

private class FakeAuthRepository : AuthRepository {
    var signedIn = false
    override val isAuthenticated: Flow<Boolean> = flowOf(false)
    override val currentUserId: Flow<String?> = flowOf(null)
    override suspend fun currentUserEmail(): String? = null
    override suspend fun currentUserDisplayName(): String? = null
    override suspend fun awaitAuthInitialization() = Unit
    override suspend fun syncSessionToLocal() = Unit
    override suspend fun importSession(accessToken: String, refreshToken: String) {
        signedIn = true
    }
    override suspend fun hasActiveSession(): Boolean = signedIn
    override suspend fun signOut() {
        signedIn = false
    }
    override fun isConfigured(): Boolean = true
}

private class FakeActivationRepository(private val auth: FakeAuthRepository) : DeviceActivationRepository {
    var createCount = 0
    var exchangeCount = 0
    var createError: Throwable? = null
    var statusError: Throwable? = null
    var statusFor: (DeviceActivationSession) -> ActivationSessionStatus = { ActivationSessionStatus.PENDING }
    var expiresAtFor: (Int) -> Instant = { Instant.now().plusSeconds(900) }
    var exchangeResult: Result<Unit> = Result.success(Unit)
    val polledSessionIds = mutableListOf<String>()

    override suspend fun createSession(deviceName: String): DeviceActivationSession {
        createError?.let { throw it }
        val index = createCount++
        return DeviceActivationSession(
            id = "session-$index",
            code = "CODE-$index",
            qrToken = "token-$index",
            status = ActivationSessionStatus.PENDING,
            userId = null,
            deviceName = deviceName,
            expiresAt = expiresAtFor(index),
        )
    }

    override suspend fun checkStatus(session: DeviceActivationSession): ActivationStatusSnapshot {
        polledSessionIds += session.id
        statusError?.let { throw it }
        return ActivationStatusSnapshot(status = statusFor(session), expiresAt = session.expiresAt)
    }

    override suspend fun exchangeForAuthSession(session: DeviceActivationSession): Result<Unit> {
        exchangeCount++
        delay(500)
        if (exchangeResult.isSuccess) auth.signedIn = true
        return exchangeResult
    }

    override fun buildActivationUrl(code: String): String = "https://example.test/?activation=$code"
}

private class FakeSessionPreparer(private val auth: FakeAuthRepository) : SessionPreparer {
    override suspend fun prepareSessionState(): AppSessionState =
        AppSessionState(isAuthenticated = auth.signedIn)

    override suspend fun authenticateLocalDev(): AppSessionState = AppSessionState(isAuthenticated = true)
}
