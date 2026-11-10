package net.opendrive.features

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FeatureConfigTest {
    @Test fun configurationRoundTripKeepsLayoutFavoritesAndTelemetryMapping() {
        val config = FeatureConfig(tiles = listOf("music", "favorites"), capacity = "64,8", manualSoc = "50", homeUrl = "https://home.example.test",
            homeEnabled = true, socEntity = "sensor.car_battery", pinnedApps = setOf("org.example.music"),
            destinations = listOf(Destination("Thuis", "Utrecht"), Destination(), Destination()),
            websites = listOf(WebFavorite("Eigen server", "https://example.test/music"), WebFavorite()),
            homeActions = listOf(HomeAction("Oprit", "light.oprit", "light.turn_on"), HomeAction(), HomeAction()))
        assertNull(config.validate())
        assertEquals(config, FeatureConfig.fromJson(JSONObject(config.json().toString())))
        assertFalse(config.json().toString().contains("token"))
    }
    @Test fun filtersUnknownAndDuplicateTilesFromStoredConfiguration() {
        assertEquals(listOf("music"), FeatureConfig.fromJson(JSONObject("{\"tiles\":[\"music\",\"other\",\"music\"]}")).tiles)
        assertNotNull(FeatureConfig(tiles = emptyList()).validate())
    }
    @Test fun rejectsNonFiniteAndOutOfRangeNumbers() {
        for (bad in listOf("NaN", "Infinity", "-1", "101", "abc")) assertNotNull(FeatureConfig(manualSoc = bad).validate())
        assertNotNull(FeatureConfig(capacity = "0").validate())
        assertNull(FeatureConfig(capacity = "64,8", consumption = "17.5", manualSoc = "0").validate())
    }
    @Test fun homeEndpointCannotCarryCredentialsPathsOrUnencryptedTransport() {
        assertTrue(FeatureConfig.validHomeUrl("https://home.example.test:8123/"))
        for (bad in listOf("http://192.168.1.2:8123", "https://user:token@home.example.test", "https://home.example.test/api", "https://home.example.test/?token=x", "https://home.example.test/#x", "file:///tmp")) {
            assertFalse(bad, FeatureConfig.validHomeUrl(bad))
        }
    }
    @Test fun homeActionsAreRestrictedToSavedMatchingEntityDomains() {
        assertNotNull(FeatureConfig(homeActions = listOf(HomeAction("Bad", "light.a", "cover.open_cover"))).validate())
        assertNotNull(FeatureConfig(homeActions = listOf(HomeAction("Bad", "shell_command.a", "shell_command.run"))).validate())
        assertFalse(FeatureConfig.validEntity("sensor.a/../../services"))
        assertTrue(FeatureConfig.validEntity("sensor.kia_soc"))
    }
    @Test fun webFavoritesRejectExecutableSchemesAndEmbeddedPasswords() {
        assertTrue(FeatureConfig.validWebUrl("https://example.test/view?a=1#tab"))
        for (bad in listOf("javascript:alert(1)", "http://example.test", "file:///x", "https://user:password@example.test")) assertFalse(FeatureConfig.validWebUrl(bad))
    }
}
