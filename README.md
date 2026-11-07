# NekoBox for Android

[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![Releases](https://img.shields.io/github/v/release/MatsuriDayo/NekoBoxForAndroid)](https://github.com/MatsuriDayo/NekoBoxForAndroid/releases)
[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-orange.svg)](https://www.gnu.org/licenses/gpl-3.0)

sing-box / universal proxy toolchain for Android.

一款使用 sing-box 的 Android 通用代理软件.

## 下载 / Downloads

[![GitHub All Releases](https://img.shields.io/github/downloads/Matsuridayo/NekoBoxForAndroid/total?label=downloads-total&logo=github&style=flat-square)](https://github.com/Matsuridayo/NekoBoxForAndroid/releases)

[GitHub Releases 下载](https://github.com/Matsuridayo/NekoBoxForAndroid/releases)

**Google Play 版本自 2024 年 5 月起已被第三方控制，为非开源版本，请不要下载。**

**The Google Play version has been controlled by a third party since May 2024 and is a non-open
source version. Please do not download it.**

## 更新日志 & Telegram 发布频道 / Changelog & Telegram Channel

https://t.me/Matsuridayo

## 项目主页 & 文档 / Homepage & Documents

https://matsuridayo.github.io

## 支持的代理协议 / Supported Proxy Protocols

* SOCKS (4/4a/5)
* HTTP(S)
* SSH
* Shadowsocks
* VMess
* Trojan
* VLESS
* AnyTLS
* ShadowTLS
* TUIC
* Hysteria 1/2
* WireGuard
* Trojan-Go (trojan-go-plugin)
* NaïveProxy (naive-plugin)
* Mieru (mieru-plugin)

请到[这里](https://matsuridayo.github.io/nb4a-plugin/)下载插件以获得完整的代理支持.

Please visit [here](https://matsuridayo.github.io/nb4a-plugin/) to download plugins for full proxy
supports.

## 支持的订阅格式 / Supported Subscription Format

* 一些广泛使用的格式 (如 Shadowsocks, ClashMeta 和 v2rayN)
* sing-box 出站

仅支持解析出站，即节点。分流规则等信息会被忽略。

* Some widely used formats (like Shadowsocks, ClashMeta and v2rayN)
* sing-box outbound

Only resolving outbound, i.e. nodes, is supported. Information such as diversion rules are ignored.

## Tailnet SOCKS5 出口

在设置的 Headscale 区域填写「Tailnet SOCKS5 代理」，格式为
`100.x.x.x:1080`，无需填写 `socks5://` 协议头，输入框以明文显示。
需要认证时可填写 `用户名:密码@100.x.x.x:1080`（特殊字符需做 URL 编码）；
已有的 `socks5://` 地址仍然兼容。IPv6 地址使用 `[IPv6]:端口` 格式。
需要启用内置 Headscale 连接并使用 VPN 模式；不要同时启动独立的 Tailscale Android VPN。
服务器的 SOCKS5 端口必须监听可从 tailnet 访问的地址，并放行相应 ACL 和防火墙规则。
建议使用 Tailscale IP；使用 MagicDNS 域名时，还需配置对应的 MagicDNS 后缀。

配置后，默认出口通过内置 tailnet 连接该 SOCKS5，手机不需要导入 Clash 订阅。
已有订阅节点、节点选择器、分组代理链和指向其他节点的路由不参与 VPN 出口，
自动及手动订阅更新暂停。直连、阻断和内网路由仍然生效，服务器端可统一管理订阅和出口。
全局自定义核心配置在此模式下不应用，避免覆盖固定出口。
Tailnet 未启用或配置无效时连接失败，不回退到订阅节点。

清空「Tailnet SOCKS5 代理」并重新连接即可恢复原来的节点选择和订阅更新设置，
已保存的订阅和节点不会被删除。UDP 是否可用取决于服务器的 SOCKS5 UDP 支持。

## 捐助 / Donate

<details>

如果这个项目对您有帮助, 可以通过捐赠的方式帮助我们维持这个项目.

捐赠满等额 50 USD 可以在「[捐赠榜](https://mtrdnt.pages.dev/donation_list)」显示头像, 如果您未被添加到这里,
欢迎联系我们补充.

Donations of 50 USD or more can display your avatar on
the [Donation List](https://mtrdnt.pages.dev/donation_list). If you are not added here, please
contact us to add it.

USDT TRC20

`TRhnA7SXE5Sap5gSG3ijxRmdYFiD4KRhPs`

XMR

`49bwESYQjoRL3xmvTcjZKHEKaiGywjLYVQJMUv79bXonGiyDCs8AzE3KiGW2ytTybBCpWJUvov8SjZZEGg66a4e59GXa6k5`

</details>

## Credits

Core:

- [SagerNet/sing-box](https://github.com/SagerNet/sing-box)

Android GUI:

- [shadowsocks/shadowsocks-android](https://github.com/shadowsocks/shadowsocks-android)
- [SagerNet/SagerNet](https://github.com/SagerNet/SagerNet)

Web Dashboard:

- [Yacd-meta](https://github.com/MetaCubeX/Yacd-meta)

## 构建版本号

构建 APK/AAB（`assemble`、`bundle`、`package`、`install` 或 `build`）时，
自动将 `nb4a.properties` 的 `VERSION_NAME` 末位加 1，例如 `1.4.2 → 1.4.3`，
并递增 `VERSION_CODE`。一次 Gradle 调用只递增一次，多个渠道共用该次版本号。
构建成功后保存版本号；构建失败不会消耗版本号。测试、lint、项目同步和 `--dry-run` 不递增。
