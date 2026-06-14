package com.brainsmooch.service

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import com.brainsmooch.data.AppInfo

object AppBlockerManager {

    private val POPULAR_APPS = listOf(
        // Social Media
        AppInfo("com.instagram.android", "Instagram", false),
        AppInfo("com.twitter.android", "X (Twitter)", false),
        AppInfo("com.facebook.katana", "Facebook", false),
        AppInfo("com.facebook.orca", "Messenger", false),
        AppInfo("com.snapchat.android", "Snapchat", false),
        AppInfo("com.zhiliaoapp.musically", "TikTok", false),
        AppInfo("com.linkedin.android", "LinkedIn", false),
        AppInfo("com.pinterest", "Pinterest", false),
        AppInfo("com.tumblr", "Tumblr", false),
        AppInfo("com.reddit.frontpage", "Reddit", false),
        AppInfo("com.discord", "Discord", false),
        AppInfo("org.telegram.messenger", "Telegram", false),
        AppInfo("com.whatsapp", "WhatsApp", false),
        AppInfo("com.viber.voip", "Viber", false),
        AppInfo("jp.naver.line.android", "LINE", false),
        AppInfo("com.bereal.ft", "BeReal", false),
        AppInfo("com.threads.android", "Threads", false),
        // Video & Streaming
        AppInfo("com.google.android.youtube", "YouTube", false),
        AppInfo("com.netflix.mediaclient", "Netflix", false),
        AppInfo("com.amazon.avod.thirdpartyclient", "Prime Video", false),
        AppInfo("com.disney.disneyplus", "Disney+", false),
        AppInfo("com.hbo.hbonow", "Max (HBO)", false),
        AppInfo("com.spotify.music", "Spotify", false),
        AppInfo("tv.twitch.android.app", "Twitch", false),
        AppInfo("com.crunchyroll.crunchyroid", "Crunchyroll", false),
        // Games
        AppInfo("com.supercell.brawlstars", "Brawl Stars", false),
        AppInfo("com.supercell.clashofclans", "Clash of Clans", false),
        AppInfo("com.supercell.clashroyale", "Clash Royale", false),
        AppInfo("com.king.candycrushsaga", "Candy Crush Saga", false),
        AppInfo("com.activision.callofduty.shooter", "Call of Duty Mobile", false),
        AppInfo("com.tencent.ig", "PUBG Mobile", false),
        AppInfo("com.mojang.minecraftpe", "Minecraft", false),
        AppInfo("com.roblox.client", "Roblox", false),
        AppInfo("com.miHoYo.GenshinImpact", "Genshin Impact", false),
        AppInfo("com.dts.freefireth", "Free Fire", false),
        AppInfo("com.nianticlabs.pokemongo", "Pokemon GO", false),
        AppInfo("com.innersloth.spacemafia", "Among Us", false),
        AppInfo("com.ea.game.fifa23_row", "EA FC Mobile", false),
        AppInfo("com.mobile.legends", "Mobile Legends", false),
        AppInfo("com.kiloo.subwaysurf", "Subway Surfers", false),
        AppInfo("com.chess", "Chess.com", false),
        AppInfo("org.lichess.mobileapp", "Lichess", false),
        AppInfo("com.garena.game.codm", "COD Mobile (Garena)", false),
        AppInfo("com.rovio.angrybirds2.revo", "Angry Birds 2", false),
        AppInfo("com.ea.gp.apexlegendsmobilefps", "Apex Legends Mobile", false),
        AppInfo("com.blizzard.diablo.immortal", "Diablo Immortal", false),
        AppInfo("com.blizzard.wtcg.hearthstone", "Hearthstone", false),
        AppInfo("com.plarium.raidlegends", "RAID: Shadow Legends", false),
        AppInfo("com.playgendary.kickthebuddy", "Kick the Buddy", false),
        AppInfo("com.miniclip.eightballpool", "8 Ball Pool", false),
        AppInfo("com.outfit7.talkingtom", "Talking Tom", false),
        AppInfo("com.igg.android.lordsmobile", "Lords Mobile", false),
        AppInfo("com.gamedevltd.wwh", "World War Heroes", false),
        AppInfo("com.gameloft.android.ANMP.GloftA9HM", "Asphalt 9", false),
        AppInfo("com.gamedevltd.modernstrike", "Modern Strike Online", false),
        // Dating
        AppInfo("com.tinder", "Tinder", false),
        AppInfo("com.bumble.app", "Bumble", false),
        AppInfo("com.badoo.mobile", "Badoo", false),
        AppInfo("co.hinge.app", "Hinge", false),
        // News & Reading
        AppInfo("com.twitter.android.lite", "X Lite", false),
        AppInfo("flipboard.app", "Flipboard", false),
        AppInfo("com.medium.reader", "Medium", false),
        // Shopping
        AppInfo("com.amazon.mShop.android.shopping", "Amazon Shopping", false),
        AppInfo("com.ebay.mobile", "eBay", false),
        AppInfo("com.alibaba.aliexpresshd", "AliExpress", false),
        AppInfo("com.shopee.es", "Shopee", false),
        // Other distracting
        AppInfo("com.google.android.apps.magazines", "Google News", false),
        AppInfo("com.brave.browser", "Brave Browser", false),
        AppInfo("org.mozilla.firefox", "Firefox", false),
        AppInfo("com.opera.browser", "Opera", false),
        AppInfo("com.UCMobile.intl", "UC Browser", false),
    )

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getUsageStatsSettingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        val installed = apps
            .filter { isUserApp(it) && hasLaunchIntent(pm, it.packageName) }
            .filter { it.packageName != context.packageName }
            .map { AppInfo(it.packageName, pm.getApplicationLabel(it).toString(), isInstalled = true) }

        val installedPackages = installed.map { it.packageName }.toSet()
        val popularNotInstalled = POPULAR_APPS.filter { it.packageName !in installedPackages }

        return (installed + popularNotInstalled).sortedBy { it.label.lowercase() }
    }

    private fun isUserApp(appInfo: ApplicationInfo): Boolean =
        (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
        (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

    private fun hasLaunchIntent(pm: PackageManager, packageName: String): Boolean =
        pm.getLaunchIntentForPackage(packageName) != null

    fun getForegroundApp(context: Context): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000 * 60

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            beginTime,
            endTime
        )

        return usageStats
            ?.filter { it.lastTimeUsed > 0 }
            ?.maxByOrNull { it.lastTimeUsed }
            ?.packageName
    }
}
