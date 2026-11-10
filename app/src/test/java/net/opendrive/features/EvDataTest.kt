package net.opendrive.features

import net.prototux.aare.car.CarData
import org.junit.Assert.*
import org.junit.Test

class EvDataTest {
    private val now = 1_000_000L
    private fun car(types: List<Int>) = CarData.State(connected = true, fuelTypes = types,
        fuel = CarData.Timed(CarData.Fuel(50, 250, false), now))
    @Test fun unknownAndHybridEnergyLevelsAreNotPresentedAsBatteryData() {
        assertNull(EvData.read(car(emptyList()), FeatureConfig(), HomeSnapshot(), now).soc)
        val hybrid = EvData.read(car(listOf(1, 10)), FeatureConfig(), HomeSnapshot(), now)
        assertNull(hybrid.soc); assertNull(hybrid.rangeKm)
        val electric = EvData.read(car(listOf(10)), FeatureConfig(), HomeSnapshot(), now)
        assertEquals(50.0, electric.soc!!.value, 0.0)
        assertEquals(250.0, electric.rangeKm!!.value, 0.0)
    }
    @Test fun manualInputIsExplicitAndNeverPretendsToProvideLiveChargeTime() {
        val ev = EvData.read(CarData.State(), FeatureConfig(manualSoc = "50", capacity = "64", consumption = "16"), HomeSnapshot(), now)
        assertEquals("Handmatig", ev.soc!!.source)
        assertEquals(200.0, ev.estimatedRangeKm!!, 0.001)
        assertNull(ev.minutesToTarget); assertNull(ev.powerKw); assertNull(ev.temperatureC)
    }
    @Test fun chargingEstimateNeedsCapacityCurrentBatteryAndPositivePower() {
        val config = FeatureConfig(capacity = "60", targetSoc = "80", powerEntity = "sensor.power")
        val home = HomeSnapshot(mapOf("sensor.power" to HomeValue("30000", "W", now)))
        val ev = EvData.read(car(listOf(10)), config, home, now)
        assertEquals(30.0, ev.powerKw!!.value, 0.0)
        assertEquals(36.0, ev.minutesToTarget!!, 0.001)
        assertNull(EvData.read(car(listOf(10)), config.copy(capacity = ""), home, now).minutesToTarget)
    }
    @Test fun staleUnavailableAndWrongUnitReadingsAreNotUsed() {
        val config = FeatureConfig(socEntity = "sensor.soc", temperatureEntity = "sensor.temp")
        for (value in listOf(HomeValue("60", "%", now - 300_001), HomeValue("unknown", "%", now), HomeValue("60", "kWh", now), HomeValue("NaN", "%", now))) {
            assertNull(EvData.read(CarData.State(), config, HomeSnapshot(mapOf("sensor.soc" to value)), now).soc)
        }
        assertNull(EvData.read(CarData.State(), config, HomeSnapshot(mapOf("sensor.temp" to HomeValue("273", "K", now))), now).temperatureC)
    }
    @Test fun configuredHomeSensorTakesPrecedenceAndKeepsItsSource() {
        val ev = EvData.read(car(listOf(10)), FeatureConfig(socEntity = "sensor.soc", manualSoc = "80"),
            HomeSnapshot(mapOf("sensor.soc" to HomeValue("65.5", "%", now))), now)
        assertEquals(65.5, ev.soc!!.value, 0.0); assertEquals("Home Assistant", ev.soc!!.source)
    }
}
