package com.solarrobo.feature.camera

import com.solarrobo.feature.camera.mock.FakeCameraRepository
import com.solarrobo.feature.camera.presentation.CameraViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CameraViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = CameraViewModel(FakeCameraRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun permissionGranted_setsPermissionState() {
        viewModel.onPermissionResult(true)
        assertTrue(viewModel.uiState.value.permissionGranted)
    }

    @Test
    fun permissionDenied_showsPermissionError() {
        viewModel.onPermissionResult(false)
        assertFalse(viewModel.uiState.value.permissionGranted)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun takeSnapshot_setsCapturedFrame() = runTest {
        viewModel.takeSnapshot()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.lastCapturedSnapshot)
    }
}
