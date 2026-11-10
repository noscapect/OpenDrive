package net.opendrive.features

import net.prototux.aare.car.CarData

data class HomeValue(val state: String, val unit: String, val readAt: Long)
data class HomeSnapshot(val values: Map<String, HomeValue> = emptyMap(), val error: String? = null)
data class EvValue(val value: Double, val source: String, val at: Long?)
data class EvData(val soc: EvValue?, val rangeKm: EvValue?, val powerKw: EvValue?, val temperatureC: EvValue?,
                  val estimatedRangeKm: Double?, val minutesToTarget: Double?) {
    companion object {
        fun read(car: CarData.State, config: FeatureConfig, home: HomeSnapshot, now: Long): EvData {
            fun remote(entity: String, units: Set<String>, range: ClosedFloatingPointRange<Double>, scale: Double = 1.0): EvValue? {
                val value = home.values[entity] ?: return null
                if (now - value.readAt !in 0..300_000 || value.unit !in units) return null
                val number = FeatureConfig.decimal(value.state)?.times(scale)?.takeIf { it in range } ?: return null
                return EvValue(number, "Home Assistant", value.readAt)
            }
            val electricOnly = car.fuelTypes.isNotEmpty() && car.fuelTypes.all { it == 10 }
            val vehicleSoc = car.fuel?.takeIf { car.connected && (electricOnly || config.vehicleBattery) }?.let { sample ->
                sample.value.levelPercent?.takeIf { it in 0..100 }?.let { EvValue(it.toDouble(), "Auto", sample.atMillis) }
            }
            val manual = FeatureConfig.decimal(config.manualSoc)?.takeIf { it in 0.0..100.0 }?.let { EvValue(it, "Handmatig", null) }
            val soc = remote(config.socEntity, setOf("%"), 0.0..100.0) ?: vehicleSoc ?: manual
            val range = remote(config.rangeEntity, setOf("km"), 0.0..2000.0) ?: car.fuel?.takeIf { car.connected && electricOnly }?.let { sample ->
                sample.value.rangeKm?.takeIf { it in 0..2000 }?.let { EvValue(it.toDouble(), "Auto", sample.atMillis) }
            }
            val power = remote(config.powerEntity, setOf("kW"), 0.0..1000.0)
                ?: remote(config.powerEntity, setOf("W"), 0.0..1000.0, 0.001)
            val temp = remote(config.temperatureEntity, setOf("°C", "C"), -50.0..100.0)
            val capacity = FeatureConfig.decimal(config.capacity)?.takeIf { it in 1.0..300.0 }
            val consumption = FeatureConfig.decimal(config.consumption)?.takeIf { it in 1.0..100.0 }
            val target = FeatureConfig.decimal(config.targetSoc)?.takeIf { it in 1.0..100.0 }
            return EvData(soc, range, power, temp,
                if (soc != null && capacity != null && consumption != null) soc.value * capacity / consumption else null,
                if (soc != null && soc.source != "Handmatig" && capacity != null && target != null && power != null && power.value > 0.1)
                    ((target - soc.value).coerceAtLeast(0.0) / 100 * capacity / power.value * 60).takeIf { it.isFinite() } else null)
        }
    }
}
