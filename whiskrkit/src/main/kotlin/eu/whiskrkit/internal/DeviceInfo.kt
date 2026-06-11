package eu.whiskrkit.internal

import android.content.Context
import android.os.Build
import java.util.Locale

/**
 * Host app and device facts sent as request headers; the Android counterpart of
 * the iOS `addCommonHeaders` values. `deviceId` is supplied lazily because it is
 * generated and owned by the eligibility storage (decision #16).
 */
internal class DeviceInfo(
    context: Context,
    private val deviceIdProvider: () -> String,
) {
    val packageName: String = context.packageName

    val appVersion: String
    val appBuild: String

    init {
        val packageInfo = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
        appVersion = packageInfo?.versionName ?: "unknown"
        appBuild = packageInfo?.longVersionCode?.toString() ?: "unknown"
    }

    val deviceId: String get() = deviceIdProvider().ifEmpty { "unknown" }

    val osVersion: String = Build.VERSION.RELEASE ?: "unknown"
    val deviceModel: String = Build.MODEL ?: "unknown"

    /** Hardware identifier in the spirit of iOS's "iPhone14,2" (decision B3). */
    val deviceIdentifier: String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    val localeTag: String get() = Locale.getDefault().toLanguageTag()
    val language: String get() = Locale.getDefault().language.ifEmpty { "unknown" }
    val region: String get() = Locale.getDefault().country.ifEmpty { "unknown" }
    val timezone: String get() = java.util.TimeZone.getDefault().id
}
