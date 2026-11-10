package net.opendrive.features

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class TripLogicTest {
    @Test fun distanceComesFromOdometerAndCategoryAndCostFromUserConfiguration() {
        val trip = TripAccumulator(1000, 1000.0, FeatureConfig(businessTrips = true, costPerKm = "0,25"), "Kia")
        trip.sample(1010.0)
        val record = trip.finish(61_000)
        assertEquals(10.0, record.km!!, 0.0); assertTrue(record.business); assertEquals(0.25, record.costPerKm!!, 0.0)
        assertEquals(record, TripRecord.parse(JSONObject(record.json().toString())))
    }
    @Test fun missingResetOrImpossibleOdometerNeverCreatesInventedDistance() {
        val unknown = TripAccumulator(0, null, FeatureConfig(), "Kia")
        unknown.sample(1234.0); assertNull(unknown.finish(1000).km)
        val reset = TripAccumulator(0, 1000.0, FeatureConfig(), "Kia")
        reset.sample(500.0); reset.sample(1001.0); assertNull(reset.finish(1000).km)
        val jump = TripAccumulator(0, 1000.0, FeatureConfig(), "Kia")
        jump.sample(7000.0); assertNull(jump.finish(1000).km)
    }
    @Test fun retentionRemovesOldRecordsAndBoundsStorage() {
        val now = 100 * 86_400_000L
        val base = TripRecord("x", 0, now, null, false, null, "")
        assertEquals(1, TripLogic.retain(listOf(base, base.copy(end = 0)), now, 7).size)
        assertEquals(1000, TripLogic.retain(List(1200) { base }, now, 90).size)
    }
    @Test fun csvQuotesSpecialCharactersNeutralizesSpreadsheetFormulasAndPreservesUnknowns() {
        val record = TripRecord("id", 0, 60_000, null, false, null, "=HYPERLINK(\"x\")\nKia")
        val csv = TripLogic.csv(listOf(record))
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"x\"\")\nKia\""))
        assertTrue(csv.endsWith("\"private\",\"\",\"\"\r\n"))
        assertTrue(csv.contains("1970-01-01T00:00:00Z"))
    }
}
