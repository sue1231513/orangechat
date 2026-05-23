package me.rerere.rikkahub.data.datastore

import kotlinx.serialization.Serializable

@Serializable
data class SystemToolsSetting(
    val amapApiKey: String = "",
    val notificationAccess: Boolean = false,
    val cameraAccess: Boolean = false,
    val locationAccess: Boolean = false,
    val appUsageAccess: Boolean = false,
    val ocrProvider: String = "local",
    val ocrApiKey: String = "",
    val ocrApiUrl: String = "",
    val ocrModel: String = "",

    // Feature 1: Location exploration
    val locationExploreEnabled: Boolean = false,
    val locationExploreRadius: Int = 1000,

    // Feature 2: Notification query
    val notificationQueryEnabled: Boolean = false,

    // Feature 3: App usage tracking
    val appUsageEnabled: Boolean = false,

    // Feature 6: Camera OCR
    val cameraOcrEnabled: Boolean = false,

    // Feature 12: Proactive messaging
    val proactiveMessagingEnabled: Boolean = false,
    val proactiveMessagingMinInterval: Int = 30,
    val proactiveMessagingMaxInterval: Int = 90,

    // Feature 13: Supabase data sync
    val supabaseEnabled: Boolean = false,
    val supabaseUrl: String = "",
    val supabaseApiKey: String = "",
    val supabaseTableName: String = "device_data",

    // Feature 14: Gadgetbridge health data
    val gadgetbridgeEnabled: Boolean = false,
    val gadgetbridgeDbPath: String = "",

    // Feature 15: Alarm
    val alarmEnabled: Boolean = false,
) {
    fun getEnabledOptions(): Set<me.rerere.rikkahub.data.ai.tools.SystemToolOption> {
        val options = mutableSetOf<me.rerere.rikkahub.data.ai.tools.SystemToolOption>()
        if (locationAccess || locationExploreEnabled) options.add(me.rerere.rikkahub.data.ai.tools.SystemToolOption.Location)
        if (notificationAccess || notificationQueryEnabled) options.add(me.rerere.rikkahub.data.ai.tools.SystemToolOption.Notifications)
        if (appUsageAccess || appUsageEnabled) options.add(me.rerere.rikkahub.data.ai.tools.SystemToolOption.AppUsage)
        if (cameraAccess || cameraOcrEnabled) options.add(me.rerere.rikkahub.data.ai.tools.SystemToolOption.Camera)
        if (locationExploreEnabled) options.add(me.rerere.rikkahub.data.ai.tools.SystemToolOption.ExploreNearby)
        if (gadgetbridgeEnabled) options.add(me.rerere.rikkahub.data.ai.tools.SystemToolOption.Gadgetbridge)
        if (alarmEnabled) options.add(me.rerere.rikkahub.data.ai.tools.SystemToolOption.Alarm)
        return options
    }
}