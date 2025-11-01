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

   private fun setupProbablyEmulatorTest(fingerprint: String = "defaultFingerprint",
                                  manufacturer: String = "defaultManufacturer",
                                  brand: String = "defaultBrand",
                                  model: String = "defaultModel",
                                  device: String = "defaultDevice",
                                  board: String = "defaultBoard",
                                  hardware: String = "defaultHardware",
                                  host: String = "defaultHost",
                                  product: String = "defaultProduct") {
        every { mockedBuildTagUtils.getFingerprint() } returns fingerprint
        every { mockedBuildTagUtils.getManufacturer() } returns manufacturer
        every { mockedBuildTagUtils.getBrand() } returns brand
        every { mockedBuildTagUtils.getModel() } returns model
        every { mockedBuildTagUtils.getDevice() } returns device
        every { mockedBuildTagUtils.getBoard() } returns board
        every { mockedBuildTagUtils.getHardware() } returns hardware
        every { mockedBuildTagUtils.getHost() } returns host
        every { mockedBuildTagUtils.getProduct() } returns product
    }

    @Test
    fun test_isProbablyEmulator_true_google1() {
        setupProbablyEmulatorTest(
            fingerprint = "google/sdk_gphone_fsdfasdf:user/release-keys",
            manufacturer = "Google",
            product = "sdk_gphone_xx",
            brand = "google",
            model = "sdk_gphone_aa",
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true_google2() {
        setupProbablyEmulatorTest(
            fingerprint = ":user/release-keys",
            manufacturer = "Google",
            product = "sdk_gphone_xx",
            brand = "google",
            model = "sdk_gphone_aa",
            device = "Emulator"
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true_fingerprint_generic() {
        setupProbablyEmulatorTest(
            fingerprint = "generic/sdk_gphoneghfkghldk:user/release-keys",
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true_fingerprint_unknown() {
        setupProbablyEmulatorTest(
            fingerprint = "unknown/sdk_gphoneghfkghldk:user/release-keys",
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true_model_google() {
        setupProbablyEmulatorTest(
            model = "aagoogle_sdkaa",
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true5() {
        setupProbablyEmulatorTest(
            model = "aaEmulatoraa",
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_device_emulator() {
        setupProbablyEmulatorTest(
            device = "Emulator"
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true7() {
        setupProbablyEmulatorTest(
            model = "aaAndroid SDK built for x86aa",
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true_model_QC() {
        setupProbablyEmulatorTest(
            board = "QC_Reference_Phone",
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_board_nox() {
        setupProbablyEmulatorTest(
            board = "nox"
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_hardware_goldfish() {
        setupProbablyEmulatorTest(
            hardware = "goldfish"
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true_hardware_vbox() {
        setupProbablyEmulatorTest(
            hardware = "vbox86"
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true_product_vbox86p() {
        setupProbablyEmulatorTest(
            product = "vbox86p"
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true_hardware_nox() {
        setupProbablyEmulatorTest(
            hardware = "nox"
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true_product_NOX() {
        setupProbablyEmulatorTest(
            product = "NOX"
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true_product_nox() {
        setupProbablyEmulatorTest(
            product = "nox"
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true_manufacturer_gennymotion() {
        setupProbablyEmulatorTest(
            manufacturer = "Genymotion"
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true_manufacturer_genymobile() {
        setupProbablyEmulatorTest(
            manufacturer = "Genymobile"
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true_host_build() {
        setupProbablyEmulatorTest(
            host = "Build"
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true_brand_generic_device_generic() {
        setupProbablyEmulatorTest(
            brand = "generic",
            device = "generic"
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true_product_google_sdk() {
        setupProbablyEmulatorTest(
            product = "google_sdk",
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true_product_sdk_x86() {
        setupProbablyEmulatorTest(
            product = "sdk_x86",
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_product_vbox86p() {
        setupProbablyEmulatorTest(
            product = "vbox86p",
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun test_isProbablyEmulator_true_product_not_xiaomi() {
        setupProbablyEmulatorTest(
            product = "Xiaomi",
            board = "QC_Reference_Phone",
        )
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun isEmulatorFilesPresent() {
        setupProbablyEmulatorTest()
        every { mockedFileUtil.getFileNoPath(any()) } returns mockk {
            every { exists() } returns true
        }
        assert(rootHelper.isProbablyEmulator())
    }

    @Test
    fun isEmulatorFilesPresent_no_files_present() {
        setupProbablyEmulatorTest()
        every { mockedFileUtil.getFileNoPath(any()) } returns mockk {
            every { exists() } returns false
        }
        assert(!rootHelper.isProbablyEmulator())
    }
}