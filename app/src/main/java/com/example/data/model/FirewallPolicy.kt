package com.example.data.model

enum class FirewallPolicy(val titleAr: String, val descAr: String) {
    ALLOW_ALL("السماح بالكل", "السماح بالاتصال عبر Wi-Fi وشبكة الهاتف"),
    BLOCK_ALL("حظر الكل", "منع التطبيق من الوصول لأي اتصال بالإنترنت"),
    WIFI_ONLY("Wi-Fi فقط", "السماح بالاتصال عبر Wi-Fi ومنع شريحة الهاتف"),
    CELLULAR_ONLY("شريحة فقط", "السماح بالاتصال عبر الشريحة ومنع Wi-Fi");

    fun isAllowedOn(networkType: NetworkType): Boolean {
        return when (this) {
            ALLOW_ALL -> true
            BLOCK_ALL -> false
            WIFI_ONLY -> networkType == NetworkType.WIFI
            CELLULAR_ONLY -> networkType == NetworkType.CELLULAR
        }
    }
}
