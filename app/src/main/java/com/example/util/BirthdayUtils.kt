package com.example.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object BirthdayUtils {
    private val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    private val fullMonthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun monthName(month: Int): String =
        monthNames.getOrNull(month - 1) ?: ""

    fun fullMonthName(month: Int): String =
        fullMonthNames.getOrNull(month - 1) ?: ""

    fun maxDaysInMonth(month: Int): Int = when (month) {
        2 -> 29
        4, 6, 9, 11 -> 30
        else -> 31
    }

    fun formatBirthday(month: Long, day: Long): String {
        if (month <= 0L || day <= 0L) return "Birthday not set"
        return "${fullMonthName(month.toInt())} $day"
    }

    fun daysUntilBirthday(month: Long, day: Long): Int? {
        if (month !in 1..12 || day !in 1..31) return null
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentYear = today.get(Calendar.YEAR)
        val target = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, month.toInt() - 1)
            val safeDay = day.toInt().coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.DAY_OF_MONTH, safeDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(today)) {
            target.add(Calendar.YEAR, 1)
        }
        val diffMillis = target.timeInMillis - today.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()
    }

    fun reminderLabel(daysAway: Int?): String = when (daysAway) {
        null -> "Add birthday"
        0 -> "Today! 🎉"
        1 -> "Tomorrow!"
        in 2..7 -> "In $daysAway days"
        else -> "In $daysAway days"
    }

    fun currentYear(): Long = Calendar.getInstance().get(Calendar.YEAR).toLong()

    fun formatRelativeTime(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: Date()
        val diffMs = (System.currentTimeMillis() - date.time).coerceAtLeast(0L)
        val mins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
        val days = TimeUnit.MILLISECONDS.toDays(diffMs)
        return when {
            mins < 1 -> "Just now"
            mins < 60 -> "${mins}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
        }
    }
}
