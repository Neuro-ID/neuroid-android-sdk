package com.neuroid.tracker.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class NIDPermissionCheckerTest {

    @After
    fun teardown() {
        unmockkAll()
    }

    // -----------------------------------------------------------------------
    // NIDPermissionChecker.checkSelfPermission
    //
    // checkSelfPermission delegates to ActivityCompat.checkSelfPermission, which
    // is the inherited static method declared on ContextCompat. We mock
    // ContextCompat (the declaring class) so the real implementation (which calls
    // the abstract Context.checkPermission) never runs.
    // -----------------------------------------------------------------------

    @Test
    fun test_checkSelfPermission_whenGranted_returnsPermissionGranted() {
        val context = mockk<Context>()

        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
        } returns PackageManager.PERMISSION_GRANTED

        val checker = NIDPermissionChecker()

        val result = checker.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)

        assertEquals(PackageManager.PERMISSION_GRANTED, result)
    }

    @Test
    fun test_checkSelfPermission_whenDenied_returnsPermissionDenied() {
        val context = mockk<Context>()

        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
        } returns PackageManager.PERMISSION_DENIED

        val checker = NIDPermissionChecker()

        val result = checker.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)

        assertEquals(PackageManager.PERMISSION_DENIED, result)
    }

    @Test
    fun test_checkSelfPermission_passesContextAndPermissionThrough() {
        val context = mockk<Context>()
        val permission = Manifest.permission.ACCESS_FINE_LOCATION

        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(any(), any())
        } returns PackageManager.PERMISSION_GRANTED

        val checker = NIDPermissionChecker()

        checker.checkSelfPermission(context, permission)

        verify(exactly = 1) {
            ContextCompat.checkSelfPermission(context, permission)
        }
    }
}

