package io.nekohasekai.sagernet.fmt.headscale

import moe.matsuri.nb4a.SingBoxOptions.MyOptions
import moe.matsuri.nb4a.SingBoxOptions.SingBoxOption
import moe.matsuri.nb4a.SingBoxOptions.TailscaleEndpointOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadscaleConfigTest {

    @Test
    fun nodeNameUsesDeviceNameAsSuffix() {
        assertEquals("work-pixel-9-pro", HeadscaleConfig.nodeName("Work", "Pixel 9 Pro"))
    }

    @Test
    fun nodeNameUsesDefaultPrefixAndDnsSafeCharacters() {
        assertEquals("neko-my-phone", HeadscaleConfig.nodeName("", "My_Phone"))
    }

    @Test
    fun nodeNameKeepsSuffixWithinDnsLabelLimit() {
        val name = HeadscaleConfig.nodeName("a".repeat(80), "pixel-9")
        assertTrue(name.length <= 63)
        assertTrue(name.endsWith("-pixel-9"))
    }

    @Test
    fun routesContainTailnetAndConfiguredSubnetsWithoutDuplicates() {
        assertEquals(
            listOf("100.64.0.0/10", "fd7a:115c:a1e0::/48", "192.168.8.0/24"),
            HeadscaleConfig.allRoutes("192.168.8.0/24, 100.64.0.0/10"),
        )
    }

    @Test
    fun authKeyIsRedactedBeforeConfigLogging() {
        val config = """{"auth_key": "secret-key", "hostname": "neko-phone"}"""
        val redacted = HeadscaleConfig.redactAuthKey(config)
        assertTrue(redacted.contains("\"auth_key\": \"***\""))
        assertTrue(!redacted.contains("secret-key"))
    }

    @Test
    fun tailscaleEndpointSerializesIntoTopLevelEndpoints() {
        val endpoint = TailscaleEndpointOptions().apply {
            type = "tailscale"
            tag = HeadscaleConfig.ENDPOINT_TAG
            control_url = "https://headscale.example.com"
            hostname = "neko-pixel-9"
        }
        val options = MyOptions().apply {
            endpoints = listOf<SingBoxOption>(endpoint)
        }

        val endpointMap = options.asMap()["endpoints"] as List<*>
        val values = endpointMap.single() as Map<*, *>
        assertEquals("tailscale", values["type"])
        assertEquals("headscale", values["tag"])
        assertEquals("neko-pixel-9", values["hostname"])
    }
}
