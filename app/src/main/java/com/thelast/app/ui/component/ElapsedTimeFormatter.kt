package com.thelast.app.ui.component

private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_HOUR = 60L * MILLIS_PER_MINUTE
private const val MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR

fun formatElapsedTime(
    timestamp: Long,
    nowMillis: Long = System.currentTimeMillis(),
): String {
    val elapsedMillis = (nowMillis - timestamp).coerceAtLeast(0L)
    val days = elapsedMillis / MILLIS_PER_DAY
    if (days > 0L) return "$days 天"

    val hours = elapsedMillis / MILLIS_PER_HOUR
    if (hours > 0L) return "$hours 小时"

    val minutes = elapsedMillis / MILLIS_PER_MINUTE
    return if (minutes > 0L) "$minutes 分钟" else "刚刚"
}
