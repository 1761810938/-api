package cn.setbug.sub2apiusage

import android.content.Context
import org.json.JSONObject

private const val WIDGET_PREFS = "sub2api_widget_prefs"
private const val KEY_SITE_PREFIX = "site_id_"
private const val KEY_SNAPSHOT_PREFIX = "snapshot_"
private const val KEY_ERROR_PREFIX = "error_"

object WidgetStore {
    fun saveWidgetSite(context: Context, appWidgetId: Int, siteId: String) {
        prefs(context).edit().putString(KEY_SITE_PREFIX + appWidgetId, siteId).apply()
    }

    fun loadWidgetSite(context: Context, appWidgetId: Int): String? {
        return prefs(context).getString(KEY_SITE_PREFIX + appWidgetId, null)
    }

    fun removeWidget(context: Context, appWidgetId: Int) {
        prefs(context).edit()
            .remove(KEY_SITE_PREFIX + appWidgetId)
            .remove(KEY_SNAPSHOT_PREFIX + appWidgetId)
            .remove(KEY_ERROR_PREFIX + appWidgetId)
            .apply()
    }

    fun saveSnapshot(context: Context, appWidgetId: Int, snapshot: UsageSnapshot) {
        prefs(context).edit()
            .putString(KEY_SNAPSHOT_PREFIX + appWidgetId, snapshot.toJson().toString())
            .remove(KEY_ERROR_PREFIX + appWidgetId)
            .apply()
    }

    fun loadSnapshot(context: Context, appWidgetId: Int): UsageSnapshot? {
        val raw = prefs(context).getString(KEY_SNAPSHOT_PREFIX + appWidgetId, null) ?: return null
        return runCatching { UsageSnapshot.fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun saveError(context: Context, appWidgetId: Int, message: String) {
        prefs(context).edit().putString(KEY_ERROR_PREFIX + appWidgetId, message).apply()
    }

    fun loadError(context: Context, appWidgetId: Int): String? {
        return prefs(context).getString(KEY_ERROR_PREFIX + appWidgetId, null)
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
}

fun UsageSnapshot.toJson(): JSONObject {
    return JSONObject().apply {
        put("totalRequests", totalRequests)
        put("totalTokens", totalTokens)
        put("inputTokens", inputTokens)
        put("outputTokens", outputTokens)
        put("standardCost", standardCost)
        put("totalActualCost", totalActualCost)
        put("averageDurationMs", averageDurationMs)
        put("averageTokensPerRequest", averageTokensPerRequest)
        put("averageCostPerRequest", averageCostPerRequest)
        put("inputShare", inputShare)
        put("outputShare", outputShare)
        put("costGap", costGap)
        put("updatedAtLabel", updatedAtLabel)
    }
}

fun UsageSnapshot.Companion.fromJson(json: JSONObject): UsageSnapshot {
    return UsageSnapshot(
        totalRequests = json.optString("totalRequests"),
        totalTokens = json.optString("totalTokens"),
        inputTokens = json.optString("inputTokens"),
        outputTokens = json.optString("outputTokens"),
        standardCost = json.optString("standardCost"),
        totalActualCost = json.optString("totalActualCost"),
        averageDurationMs = json.optString("averageDurationMs"),
        averageTokensPerRequest = json.optString("averageTokensPerRequest"),
        averageCostPerRequest = json.optString("averageCostPerRequest"),
        inputShare = json.optString("inputShare"),
        outputShare = json.optString("outputShare"),
        costGap = json.optString("costGap"),
        updatedAtLabel = json.optString("updatedAtLabel"),
    )
}
