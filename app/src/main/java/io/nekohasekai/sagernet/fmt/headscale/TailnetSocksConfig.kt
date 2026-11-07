package io.nekohasekai.sagernet.fmt.headscale

import io.nekohasekai.sagernet.database.ProxyEntity
import io.nekohasekai.sagernet.fmt.socks.SOCKSBean
import io.nekohasekai.sagernet.fmt.socks.buildSingBoxOutboundSocksBean
import moe.matsuri.nb4a.SingBoxOptions.Outbound_SocksOptions
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** A transient profile: never replaces or modifies the user's stored subscription selection. */
object TailnetSocksConfig {
    const val PROFILE_ID = -2L

    fun parse(value: String): SOCKSBean {
        val text = value.trim()
        // Keep existing saved URLs working while accepting host:port directly.
        require(!text.contains("://") || text.startsWith("socks5://", ignoreCase = true)) {
            "Invalid Tailnet SOCKS5 address"
        }
        val authority = text.substringAfter("://", text)
        val url = ("http://" + authority).toHttpUrlOrNull()
        require(url != null && url.encodedPath == "/" && url.query == null && url.fragment == null) {
            "Invalid Tailnet SOCKS5 URL"
        }
        // HttpUrl defaults missing ports to 80; require an explicitly specified proxy port.
        val hostPort = authority.substringAfterLast('@').removeSuffix("/")
        val port = hostPort.substringAfterLast(':', "").toIntOrNull()
        require(port != null && port in 1..65535 &&
            (!hostPort.startsWith('[') || hostPort.contains("]:"))) { "Missing SOCKS5 port" }
        return SOCKSBean().apply {
            initializeDefaultValues()
            name = "Tailnet SOCKS5"
            protocol = SOCKSBean.PROTOCOL_SOCKS5
            serverAddress = url.host
            serverPort = url.port
            username = url.username
            password = url.password
        }
    }

    /** Show only the endpoint, even when a saved URL includes authentication. */
    fun displayAddress(value: String): String = parse(value).displayAddress()

    fun redactCredentials(config: String): String = config.replace(
        Regex(""""(username|password)"\s*:\s*"(?:\\.|[^"\\])*""""),
        "\"$1\": \"***\"",
    )

    fun serviceProfile(): ProxyEntity = ProxyEntity(id = PROFILE_ID).apply {
        putBean(SOCKSBean().apply {
            initializeDefaultValues()
            name = "Tailnet SOCKS5"
        })
    }

    fun profile(value: String): ProxyEntity = ProxyEntity(id = PROFILE_ID).apply {
        putBean(parse(value))
    }

    fun outbound(bean: SOCKSBean): Outbound_SocksOptions = buildSingBoxOutboundSocksBean(bean).apply {
        _hack_config_map["detour"] = HeadscaleConfig.ENDPOINT_TAG
    }
}
