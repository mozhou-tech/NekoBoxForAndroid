package io.nekohasekai.sagernet.fmt.headscale

import org.junit.Assert.*
import org.junit.Test

class TailnetSocksConfigTest {
    @Test
    fun acceptsIpAndPortWithoutScheme() {
        val bean = TailnetSocksConfig.parse(" 100.64.1.2:1080 ")
        assertEquals("100.64.1.2", bean.serverAddress)
        assertEquals(1080, bean.serverPort)
        assertEquals("", bean.username)
        assertEquals("", bean.password)
        val outbound = TailnetSocksConfig.outbound(bean).asMap()
        assertEquals("headscale", outbound["detour"])
    }

    @Test
    fun acceptsIpv6AndCredentialsWithoutScheme() {
        val bean = TailnetSocksConfig.parse("alice:p%40ss@[fd7a:115c:a1e0::1]:7891")
        assertEquals("fd7a:115c:a1e0::1", bean.serverAddress)
        assertEquals(7891, bean.serverPort)
        assertEquals("alice", bean.username)
        assertEquals("p@ss", bean.password)
    }

    @Test
    fun parsesCredentialsAndExplicitPort() {
        val bean = TailnetSocksConfig.parse(" socks5://alice:p%40ss@100.64.1.2:7891 ")
        assertEquals("100.64.1.2", bean.serverAddress)
        assertEquals(7891, bean.serverPort)
        assertEquals("alice", bean.username)
        assertEquals("p@ss", bean.password)
        assertEquals("5", bean.protocolVersionName())
    }

    @Test
    fun supportsIpv6AndMagicDnsHosts() {
        assertEquals("fd7a:115c:a1e0::1", TailnetSocksConfig.parse("socks5://[fd7a:115c:a1e0::1]:1080").serverAddress)
        assertEquals("proxy.example.ts.net", TailnetSocksConfig.parse("socks5://proxy.example.ts.net:1080").serverAddress)
    }

    @Test
    fun rejectsMissingPortsWrongProtocolsAndNonProxyUrls() {
        listOf("", "100.64.1.2", "100.64.1.2:0", "100.64.1.2:65536",
            "100.64.1.2:abc", "100.64.1.2:1080/path", "http://100.64.1.2:1080", "socks5://100.64.1.2", "socks5://host:0",
            "socks5://host:65536", "socks5://host:1080/path", "socks5://host:1080?x=1",
            "socks5://host:1080#name", "socks5://[fd7a:115c:a1e0::1]").forEach {
            assertTrue("Should reject $it", runCatching { TailnetSocksConfig.parse(it) }.isFailure)
        }
    }

    @Test
    fun gatewayUsesTransientIdentityWithoutSubscriptionGroup() {
        val profile = TailnetSocksConfig.profile("socks5://100.64.1.2:1080")
        assertEquals(TailnetSocksConfig.PROFILE_ID, profile.id)
        assertEquals(0L, profile.groupId)
        assertEquals("100.64.1.2", profile.requireBean().serverAddress)
        assertEquals(profile.id, TailnetSocksConfig.serviceProfile().id)
    }

    @Test
    fun gatewayAlwaysDialsThroughTailnet() {
        val outbound = TailnetSocksConfig.outbound(TailnetSocksConfig.parse("socks5://100.64.1.2:1080"))
        val values = outbound.asMap()
        assertEquals("socks", values["type"])
        assertEquals("headscale", values["detour"])
        assertEquals("100.64.1.2", values["server"])
        assertEquals(1080L, values["server_port"])
    }

    @Test
    fun overviewShowsEndpointWithoutSchemeOrCredentials() {
        assertEquals("100.64.1.2:1080", TailnetSocksConfig.displayAddress("socks5://alice:secret@100.64.1.2:1080"))
        assertEquals("100.64.1.2:7891", TailnetSocksConfig.displayAddress("100.64.1.2:7891"))
        assertEquals("[fd7a:115c:a1e0::1]:1080", TailnetSocksConfig.displayAddress("alice:secret@[fd7a:115c:a1e0::1]:1080"))
    }

    @Test
    fun logRedactionHandlesEscapedQuotes() {
        val redacted = TailnetSocksConfig.redactCredentials("""{"username":"alice","password":"p\"ass","server":"100.64.1.2"}""")
        assertFalse(redacted.contains("alice"))
        assertTrue(redacted.contains("\"password\": \"***\""))
        assertTrue(redacted.contains("100.64.1.2"))
    }
}
