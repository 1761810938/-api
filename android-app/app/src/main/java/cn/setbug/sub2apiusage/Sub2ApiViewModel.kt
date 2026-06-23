package cn.setbug.sub2apiusage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import org.json.JSONObject

class Sub2ApiViewModel(
    private val repository: SiteRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(Sub2ApiUiState(sites = repository.loadSites()))
    val uiState: StateFlow<Sub2ApiUiState> = _uiState.asStateFlow()

    fun saveSite(site: Sub2ApiSite) {
        val updated = repository.saveSite(site)
        _uiState.update { it.copy(sites = updated) }
    }

    fun deleteSite(siteId: String) {
        val updated = repository.deleteSite(siteId)
        _uiState.update {
            it.copy(
                sites = updated,
                usageBySiteId = it.usageBySiteId - siteId,
                loadingSiteId = if (it.loadingSiteId == siteId) null else it.loadingSiteId,
            )
        }
    }

    fun refreshSite(siteId: String) {
        val site = _uiState.value.sites.firstOrNull { it.id == siteId } ?: return
        _uiState.update { it.copy(loadingSiteId = siteId) }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching { Sub2ApiClient.fetchTodayUsage(site) }
                .onSuccess { usage ->
                    _uiState.update {
                        it.copy(
                            loadingSiteId = null,
                            usageBySiteId = it.usageBySiteId + (siteId to usage),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loadingSiteId = null,
                            errorMessage = error.message ?: "查询失败",
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

@Composable
fun rememberSub2ApiViewModel(): Sub2ApiViewModel {
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    return remember {
        Sub2ApiViewModel(siteRepository(appContext))
    }
}

data class Sub2ApiUiState(
    val sites: List<Sub2ApiSite> = emptyList(),
    val usageBySiteId: Map<String, UsageSnapshot> = emptyMap(),
    val loadingSiteId: String? = null,
    val errorMessage: String? = null,
)

data class Sub2ApiSite(
    val id: String,
    val name: String,
    val baseUrl: String,
    val email: String,
    val password: String,
) {
    companion object {
        fun new() = Sub2ApiSite(
            id = UUID.randomUUID().toString(),
            name = "",
            baseUrl = "",
            email = "",
            password = "",
        )
    }
}

data class UsageSnapshot(
    val totalRequests: String,
    val totalTokens: String,
    val inputTokens: String,
    val outputTokens: String,
    val standardCost: String,
    val totalActualCost: String,
    val averageDurationMs: String,
    val updatedAtLabel: String,
) {
    companion object
}

class SiteRepository(private val file: File) {
    fun loadSites(): List<Sub2ApiSite> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val raw = file.readText()
            if (raw.isBlank()) return emptyList()
            val array = org.json.JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        Sub2ApiSite(
                            id = item.optString("id"),
                            name = item.optString("name"),
                            baseUrl = item.optString("baseUrl"),
                            email = item.optString("email"),
                            password = item.optString("password"),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveSite(site: Sub2ApiSite): List<Sub2ApiSite> {
        val current = loadSites().toMutableList()
        val index = current.indexOfFirst { it.id == site.id }
        if (index >= 0) current[index] = site else current.add(site)
        writeSites(current)
        return current
    }

    fun deleteSite(siteId: String): List<Sub2ApiSite> {
        val current = loadSites().filterNot { it.id == siteId }
        writeSites(current)
        return current
    }

    private fun writeSites(sites: List<Sub2ApiSite>) {
        val array = org.json.JSONArray()
        sites.forEach { site ->
            array.put(
                JSONObject().apply {
                    put("id", site.id)
                    put("name", site.name)
                    put("baseUrl", site.baseUrl)
                    put("email", site.email)
                    put("password", site.password)
                }
            )
        }
        file.writeText(array.toString())
    }
}

object Sub2ApiClient {
    fun fetchTodayUsage(site: Sub2ApiSite): UsageSnapshot {
        val baseUrl = normalizeBaseUrl(site.baseUrl)
        if (baseUrl.isBlank() || site.email.isBlank() || site.password.isBlank()) {
            throw IllegalArgumentException("请先填写站点 URL、邮箱和密码")
        }

        val loginPayload = postJson(
            "$baseUrl/api/v1/auth/login",
            JSONObject().apply {
                put("email", site.email)
                put("password", site.password)
            },
            "登录失败"
        )
        val loginData = unwrapApiResponse(loginPayload, "登录失败")
        val token = loginData.optString("access_token").trim()
        if (token.isBlank()) {
            if (loginData.optBoolean("requires_2fa")) {
                throw IllegalStateException("当前账号启用了 2FA，App 暂不支持自动登录")
            }
            throw IllegalStateException("登录响应缺少 access_token")
        }

        val timezone = try {
            TimeZone.getDefault().id.ifBlank { "Asia/Shanghai" }
        } catch (_: Exception) {
            "Asia/Shanghai"
        }
        val statsUrl = "$baseUrl/api/v1/usage/dashboard/stats?timezone=${URLEncoder.encode(timezone, "UTF-8")}"
        val statsPayload = getJson(statsUrl, mapOf("Authorization" to "Bearer $token"), "读取失败")
        val stats = unwrapApiResponse(statsPayload, "读取失败")

        return UsageSnapshot(
            totalRequests = formatNumber(stats.optDouble("today_requests")),
            totalTokens = formatNumber(stats.optDouble("today_tokens")),
            inputTokens = formatNumber(stats.optDouble("today_input_tokens")),
            outputTokens = formatNumber(stats.optDouble("today_output_tokens")),
            standardCost = formatCost(stats.optDouble("today_cost")),
            totalActualCost = formatCost(stats.optDouble("today_actual_cost")),
            averageDurationMs = formatDuration(stats.optDouble("average_duration_ms")),
            updatedAtLabel = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
        )
    }

    private fun postJson(url: String, body: JSONObject, fallbackTitle: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        connection.outputStream.use { output ->
            output.write(body.toString().toByteArray())
        }
        return parseJsonResponse(connection, fallbackTitle, url)
    }

    private fun getJson(url: String, headers: Map<String, String>, fallbackTitle: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
        }
        return parseJsonResponse(connection, fallbackTitle, url)
    }

    private fun parseJsonResponse(connection: HttpURLConnection, fallbackTitle: String, url: String): JSONObject {
        val status = connection.responseCode
        val input = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = input?.bufferedReader()?.use { it.readText() }.orEmpty()
        val route = runCatching { URL(url).path }.getOrNull()?.let { "$it：" }.orEmpty()

        val json = runCatching { JSONObject(text) }.getOrElse {
            throw IllegalStateException("$fallbackTitle：${route}HTTP $status，非 JSON 响应：${text.take(120)}")
        }

        if (status !in 200..299) {
            throw IllegalStateException("$fallbackTitle：${route}${readableMessage(json).ifBlank { "HTTP $status" }}")
        }
        return json
    }

    private fun unwrapApiResponse(payload: JSONObject, fallbackTitle: String): JSONObject {
        if (payload.has("code")) {
            if (payload.optInt("code") == 0) {
                return payload.optJSONObject("data") ?: JSONObject()
            }
            throw IllegalStateException("$fallbackTitle：${readableMessage(payload).ifBlank { "code=${payload.optInt("code")}" }}")
        }
        return payload
    }

    private fun normalizeBaseUrl(value: String): String {
        var url = value.trim()
        if (url.isBlank()) return ""
        url = url.removeSuffix("/")
        url = url.replace(Regex("/admin/usage$", RegexOption.IGNORE_CASE), "")
        url = url.replace(Regex("/api/v1$", RegexOption.IGNORE_CASE), "")
        return url.trimEnd('/')
    }

    private fun formatNumber(value: Double): String {
        val number = if (value.isFinite()) value else 0.0
        val absolute = kotlin.math.abs(number)
        return when {
            absolute >= 1_000_000_000 -> "${trimTrailingZeros(number / 1_000_000_000, 2)}B"
            absolute >= 1_000_000 -> "${trimTrailingZeros(number / 1_000_000, 2)}M"
            absolute >= 1_000 -> "${trimTrailingZeros(number / 1_000, 2)}K"
            else -> number.toLong().toString()
        }
    }

    private fun formatCost(value: Double): String {
        val number = if (value.isFinite()) value else 0.0
        return if (number == 0.0) "$0.00"
        else if (kotlin.math.abs(number) < 1) "$${trimTrailingZeros(number, 6)}"
        else "$${trimTrailingZeros(number, 4)}"
    }

    private fun formatDuration(value: Double): String {
        val number = if (value.isFinite()) value else 0.0
        return if (number >= 1000) "${trimTrailingZeros(number / 1000, 1)} s" else "${number.toInt()} ms"
    }

    private fun trimTrailingZeros(value: Double, digits: Int): String {
        return String.format(Locale.US, "%.${digits}f", value).replace(Regex("\\.?0+$"), "")
    }

    private fun readableMessage(payload: JSONObject): String {
        return payload.optString("message").ifBlank {
            payload.optString("error").ifBlank {
                payload.optString("detail")
            }
        }
    }
}
