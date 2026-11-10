package net.opendrive.assistant

import net.prototux.aare.BuildConfig
import net.prototux.aare.launch.LaunchMain
import net.prototux.aare.service.ProjectionService
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplicationIdentityTest {
    @Test fun openDriveHasItsOwnInstalledIdentity() {
        assertEquals("io.github.noscapect.opendrive", BuildConfig.APPLICATION_ID)
    }

    @Test fun internalActionsBelongToTheInstalledApplication() {
        val prefix = BuildConfig.APPLICATION_ID + "."
        assertEquals(prefix + "USB", ProjectionService.ACTION_USB)
        assertEquals(prefix + "USB_PERMISSION", ProjectionService.ACTION_USB_PERMISSION)
        assertEquals(prefix + "IDLE", ProjectionService.ACTION_IDLE)
        assertEquals(prefix + "STOP", ProjectionService.ACTION_STOP)
        assertEquals(prefix + "DOWNLOAD_CERT", ProjectionService.ACTION_DOWNLOAD_CERT)
        assertEquals(prefix + "WIRELESS", ProjectionService.ACTION_WIRELESS)
        assertEquals(prefix + "PROBE_WIRELESS", ProjectionService.ACTION_PROBE_WIRELESS)
        assertEquals(prefix + "AUDIO_CONSENT", ProjectionService.ACTION_AUDIO_CONSENT)
        assertEquals(prefix + "LAUNCH_HOST", LaunchMain.ACTION_HOST)
    }
}
