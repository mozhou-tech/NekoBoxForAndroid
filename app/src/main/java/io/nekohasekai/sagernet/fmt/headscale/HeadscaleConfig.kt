package io.nekohasekai.sagernet.fmt.headscale

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.net.IDN

object HeadscaleConfig {

    const val ENDPOINT_TAG = "headscale"
    const val DNS_TAG = "headscale-dns"
    const val DEFAULT_PREFIX = "neko"

    val DEFAULT_ROUTES = listOf(
        "100.64.0.0/10",
        "fd7a:115c:a1e0::/48",
    )

    fun deviceName(context: Context): String {
        val configuredName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        } else {
            null
        }
        return configuredName?.takeIf { it.isNotBlank() }
            ?: Build.MODEL.takeIf { it.isNotBlank() }
            ?: "android"
    }

    fun nodeName(prefix: String?, deviceName: String): String {
        val suffix = dnsLabel(deviceName).ifBlank { "android" }
        val normalizedPrefix = dnsLabel(prefix.orEmpty()).ifBlank { DEFAULT_PREFIX }
        val maxPrefixLength = 63 - suffix.length - 1
        if (maxPrefixLength <= 0) return suffix.takeLast(63).trim('-').ifBlank { "android" }
        val shortenedPrefix = normalizedPrefix.take(maxPrefixLength).trim('-')
        return if (shortenedPrefix.isBlank()) suffix.takeLast(63) else "$shortenedPrefix-$suffix"
    }

    fun parseRoutes(value: String): List<String> = value
        .split(',', '\n', '\r', ' ')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()

    fun allRoutes(additionalRoutes: String): List<String> =
        (DEFAULT_ROUTES + parseRoutes(additionalRoutes)).distinct()

    fun redactAuthKey(config: String): String = config.replace(
        Regex("\\\"auth_key\\\"\\s*:\\s*\\\"[^\\\"]*\\\""),
        "\"auth_key\": \"***\"",
    )

    private fun dnsLabel(value: String): String {
        val prepared = value.trim()
            .replace(Regex("[\\s_]+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
        if (prepared.isBlank()) return ""
        val ascii = runCatching { IDN.toASCII(prepared, IDN.USE_STD3_ASCII_RULES) }
            .getOrElse { prepared }
        return ascii.lowercase()
            .replace(Regex("[^a-z0-9-]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
    }
}
