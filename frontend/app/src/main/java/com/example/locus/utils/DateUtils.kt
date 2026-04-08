package com.example.locus.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

fun formatTimeAgo(isoString: String): String {
    return try {
        val postTime = Instant.parse(isoString.uppercase())
        val now = Instant.now()

        val minutes = ChronoUnit.MINUTES.between(postTime, now)
        val hours = ChronoUnit.HOURS.between(postTime, now)
        val days = ChronoUnit.DAYS.between(postTime, now)

        when {
            minutes < 1 -> "Now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours h ago"
            days < 7 -> "$days days ago"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
                    .withZone(ZoneId.systemDefault())
                formatter.format(postTime)
            }
        }
    } catch (e: Exception) {
        isoString
    }
}