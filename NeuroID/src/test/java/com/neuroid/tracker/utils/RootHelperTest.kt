package com.neuroid.tracker.utils

import android.content.Context
import android.content.pm.PackageManager
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class RootHelperTest {

    private lateinit var mockContext: Context
    private lateinit var mockPackageManager: PackageManager
    private lateinit var mockEnvironmentProvider: NIDEnvironmentProvider
    private lateinit var mockRuntimeProvider: NIDRuntimeProvider
    private lateinit var rootHelper: RootHelper
    private lateinit var mockedFileUtil: FileCreationUtils
    private lateinit var mockedBuildTagUtils: NIDTagUtils

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockPackageManager = mockk(relaxed = true)
        mockedFileUtil = mockk()
        mockEnvironmentProvider = mockk()
        mockRuntimeProvider = mockk()
        mockedBuildTagUtils = mockk()
        every { mockContext.packageManager } returns mockPackageManager
        rootHelper = RootHelper(mockEnvironmentProvider, mockRuntimeProvider, mockedFileUtil, mockedBuildTagUtils)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun isRooted_test_true() {
        // given

        // detectRootManagementApps() true
        // detectPotentiallyDangerousApps() true
        every {mockEnvironmentProvider.getenv("PATH")} returns "/system/bin:/system/xbin"
        every {mockPackageManager.getPackageInfo(any<String>(), any<Int>())} throws PackageManager.NameNotFoundException()
        every {mockContext.packageManager} returns mockPackageManager
        every {mockPackageManager.getInstalledPackages(0) } returns emptyList()

        // checkSuExists() true
        val process = mockk<Process>()
        every {process.destroy()} just runs
        every {mockRuntimeProvider.executeCommand(any<Array<String>>())} returns process
        every {mockedFileUtil.getBufferedReader(any())} returns mockk{
            every { readLine() } returns "/bin/su, yep its there"
        }
        // detectTestKeys() true
        every { mockedBuildTagUtils.getBuildTags() } returns "test-keys:gsdgdasgdasg53454rgdfgdf"

        // checkForBinary() true
        // checkForMagiskBinary() true
        every {mockedFileUtil.getFile(any(), any())} returns mockk{
            every {exists()} returns true
        }
        // when
        val result = rootHelper.isRooted(mockContext)

        // then
        assert(result)
    }

    @Test
    fun isRooted_test_false() {
        // given

        // detectRootManagementApps() false
        // detectPotentiallyDangerousApps() false
        every {mockEnvironmentProvider.getenv("PATH")} returns "/system/bin:/system/xbin"
        every {mockPackageManager.getPackageInfo(any<String>(), any<Int>())} throws PackageManager.NameNotFoundException()
        every {mockContext.packageManager} returns mockPackageManager
        every {mockPackageManager.getInstalledPackages(0) } returns emptyList()

        // checkSuExists() false
        val process = mockk<Process>()
        every {process.destroy()} just runs
        every {mockRuntimeProvider.executeCommand(any<Array<String>>())} returns process
        every {mockedFileUtil.getBufferedReader(any())} returns mockk{
            every { readLine() } returns null
        }

        // detectTestKeys() false
        every { mockedBuildTagUtils.getBuildTags() } returns "prod-keys"

        // checkForBinary() false
        // checkForMagiskBinary() false
        every {mockedFileUtil.getFile(any(), any())} returns mockk{
            every {exists()} returns false
        }

        // when
        val result = rootHelper.isRooted(mockContext)

        //then
        assertFalse(result)
    }

    @Test
    fun test_getPaths() {
        // given
        every {mockEnvironmentProvider.getenv("PATH")} returns "/test/bin:/test/sd/xbin:/gsagsdag/"

        // when
        val result = rootHelper.getPaths()

        // then
        assert(result.contains("/test/bin/"))
        assert(result.contains("/test/sd/xbin/"))
        assertFalse(result.contains("/gsagsdag/"))
    }

    @Test
    fun test_isAnyPackageFromListInstalled_true() {
        // given
        val packageName = "com.test.package"
        every {mockPackageManager.getPackageInfo(packageName, 0)} returns mockk()
        // when
        val result = rootHelper.isAnyPackageFromListInstalled(mockContext, listOf(packageName))
        // then
        assertTrue(result)
    }

    @Test
    fun test_isAnyPackageFromListInstalled_false() {
        // given
        val packageName = "com.test.package"
        every {mockPackageManager.getPackageInfo(packageName, 0)} throws PackageManager.NameNotFoundException()
        // when
        val result = rootHelper.isAnyPackageFromListInstalled(mockContext, listOf(packageName))
        // then
        assertFalse(result)
    }

    @Test
    fun test_isProbablyEmulator_true() {
        every { mockedBuildTagUtils.getFingerprint() } returns "google/sdk_gphone_:user/release-keys"
        every { mockedBuildTagUtils.getManufacturer() } returns "sdk"
        every { mockedBuildTagUtils.getBrand() } returns "google"
        every { mockedBuildTagUtils.getModel() } returns "sdk_gphone_"
        every { mockedBuildTagUtils.getDevice() } returns "Emulator"
        every { mockedBuildTagUtils.getBoard() } returns "QC_Reference_Phone"
        every { mockedBuildTagUtils.getHardware() } returns "goldfish"
        every { mockedBuildTagUtils.getHost() } returns "Build"
        every { mockedBuildTagUtils.getProduct() } returns "sdk_x86"
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_false() {
        every { mockedBuildTagUtils.getFingerprint() } returns "gdfsgd"
        every { mockedBuildTagUtils.getManufacturer() } returns "sdgdfsgsdfk"
        every { mockedBuildTagUtils.getBrand() } returns "genegsdfgsdfgdsric"
        every { mockedBuildTagUtils.getModel() } returns "Emulhdhdsfhator"
        every { mockedBuildTagUtils.getDevice() } returns "hdfshsdfhsdfh"
        every { mockedBuildTagUtils.getBoard() } returns "hdfshsdfh"
        every { mockedBuildTagUtils.getHardware() } returns "hdshdsfhdfh"
        every { mockedBuildTagUtils.getHost() } returns "dhdsfh"
        every { mockedBuildTagUtils.getProduct() } returns "sdhsdfhsdfh"
        assertFalse(rootHelper.isProbablyEmulator())
    }

}