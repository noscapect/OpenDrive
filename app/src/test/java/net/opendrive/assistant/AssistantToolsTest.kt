package net.opendrive.assistant

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AssistantToolsTest {
    @Test fun onlyNavigationAndMusicToolsAreExposedAndSchemasAreStrict() {
        val tools = OpenDriveTools.specs()
        val names = (0 until tools.length()).map { tools.getJSONObject(it).getString("name") }.toSet()
        assertEquals(setOf("car_status", "car_navigate", "car_media", "opendrive_apps", "opendrive_music_search", "opendrive_music_play"), names)
        for (i in 0 until tools.length()) {
            val tool = tools.getJSONObject(i)
            assertTrue(tool.getBoolean("strict"))
            val parameters = tool.getJSONObject("parameters")
            assertFalse(parameters.getBoolean("additionalProperties"))
            assertEquals(parameters.getJSONObject("properties").length(), parameters.getJSONArray("required").length())
        }
    }

    @Test fun rejectsExtraArgumentsInvalidActionsAndUnboundedSearches() {
        assertTrue(OpenDriveTools.validate("car_media", JSONObject().put("action", "pause")))
        assertFalse(OpenDriveTools.validate("car_media", JSONObject().put("action", "delete")))
        assertFalse(OpenDriveTools.validate("car_navigate", JSONObject().put("destination", " ")))
        assertFalse(OpenDriveTools.validate("car_navigate", JSONObject().put("destination", "Utrecht").put("intent", "other")))
        assertFalse(OpenDriveTools.validate("opendrive_music_search", JSONObject().put("query", "a".repeat(501)).put("app", "")))
        assertFalse(OpenDriveTools.validate("car_reply", JSONObject()))
    }

    @Test fun playbackMustUseAnIntegerFromTheBoundedResultSet() {
        assertTrue(OpenDriveTools.validate("opendrive_music_play", JSONObject().put("number", 1)))
        for (value in listOf(0, -1, 9, "1", 1.5)) {
            assertFalse(OpenDriveTools.validate("opendrive_music_play", JSONObject().put("number", value)))
        }
    }
}
