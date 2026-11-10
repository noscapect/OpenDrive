package net.opendrive.features

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID

data class TripRecord(val id: String, val start: Long, val end: Long, val km: Double?, val business: Boolean,
                      val costPerKm: Double?, val car: String) {
    fun json() = JSONObject().put("id", id).put("start", start).put("end", end).put("km", km ?: JSONObject.NULL)
        .put("business", business).put("costPerKm", costPerKm ?: JSONObject.NULL).put("car", car)
    companion object {
        fun parse(o: JSONObject) = TripRecord(o.getString("id"), o.getLong("start"), o.getLong("end"),
            o.optDouble("km").takeIf { it.isFinite() && it >= 0 }, o.optBoolean("business"),
            o.optDouble("costPerKm").takeIf { it.isFinite() && it >= 0 }, o.optString("car"))
    }
}

/** Stores only session times, aggregate odometer distance and a category. No route or GPS history. */
class TripStore private constructor(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "opendrive-trips.json"))
    private val mutable = MutableStateFlow(runCatching {
        val a = JSONArray(String(file.readFully(), Charsets.UTF_8))
        (0 until a.length()).map { TripRecord.parse(a.getJSONObject(it)) }
    }.getOrDefault(emptyList()))
    val records: StateFlow<List<TripRecord>> = mutable

    @Synchronized fun add(record: TripRecord, retentionDays: Int) = write(TripLogic.retain(listOf(record) + mutable.value, System.currentTimeMillis(), retentionDays))
    @Synchronized fun prune(retentionDays: Int) = write(TripLogic.retain(mutable.value, System.currentTimeMillis(), retentionDays))
    @Synchronized fun category(id: String, business: Boolean) = write(mutable.value.map { if (it.id == id) it.copy(business = business) else it })
    @Synchronized fun delete(id: String) = write(mutable.value.filterNot { it.id == id })
    @Synchronized fun clear() = write(emptyList())
    private fun write(records: List<TripRecord>) {
        val stream = file.startWrite()
        try {
            stream.write(JSONArray(records.map { it.json() }).toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(stream); mutable.value = records
        } catch (e: Exception) { file.failWrite(stream); throw e }
    }
    companion object {
        @Volatile private var instance: TripStore? = null
        fun get(context: Context): TripStore = instance ?: synchronized(this) {
            instance ?: TripStore(context.applicationContext).also { instance = it }
        }
    }
}

internal class TripAccumulator(private val start: Long, private val startOdometer: Double?, private val config: FeatureConfig, private val car: String) {
    private var lastOdometer: Double? = startOdometer
    private var valid = true
    fun sample(odometer: Double?) {
        if (odometer == null || !odometer.isFinite()) return
        if (lastOdometer?.let { odometer < it } == true) valid = false
        lastOdometer = odometer
    }
    fun finish(end: Long): TripRecord = TripRecord(UUID.randomUUID().toString(), start, end.coerceAtLeast(start),
        if (valid && startOdometer != null) lastOdometer?.minus(startOdometer)?.takeIf { it in 0.0..5000.0 } else null,
        config.businessTrips, FeatureConfig.decimal(config.costPerKm), car)
}

object TripLogic {
    fun retain(records: List<TripRecord>, now: Long, days: Int) = records.filter { it.end >= now - days.coerceIn(1,365) * 86_400_000L }.take(1000)
    fun csv(records: List<TripRecord>): String {
        fun cell(text: String): String {
            val safe = if (text.trimStart().firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r')) "'$text" else text
            return "\"${safe.replace("\"", "\"\"")}\""
        }
        return "id,start_utc,end_utc,car,category,distance_km,cost_eur\r\n" + records.joinToString("") { r ->
            listOf(r.id, Instant.ofEpochMilli(r.start).toString(), Instant.ofEpochMilli(r.end).toString(), r.car,
                if (r.business) "business" else "private", r.km?.toString().orEmpty(),
                if (r.km != null && r.costPerKm != null) (r.km * r.costPerKm).toString() else "").joinToString(",") { cell(it) } + "\r\n"
        }
    }
}
