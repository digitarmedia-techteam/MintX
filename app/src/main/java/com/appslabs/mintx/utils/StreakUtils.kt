package com.appslabs.mintx.utils

import java.util.Calendar
import java.util.Locale

object StreakUtils {

    fun isToday(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)

        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.DAY_OF_YEAR) == today && calendar.get(Calendar.YEAR) == year
    }

    fun isSameDay(ts1: Long, ts2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = ts1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = ts2 }
        return cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    // Returns a list of booleans for the current week (Mon-Sun)
    fun getCurrentWeekActivity(activityDates: List<Long>): List<Boolean> {
        val result = MutableList(7) { false }
        val calendar = Calendar.getInstance()
        
        // Set to Monday of the current week
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        
        for (i in 0 until 7) {
            val checkTime = calendar.timeInMillis
            val isActive = activityDates.any { date ->
                isSameDay(date, checkTime)
            }
            result[i] = isActive
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    fun getCurrentStreak(activityDates: List<Long>): Int {
        if (activityDates.isEmpty()) return 0
        
        val sortedDates = activityDates.sortedDescending()
        var streak = 0
        
        // Check if active today
        var isStreakAlive = isSameDay(sortedDates[0], System.currentTimeMillis())
        if (!isStreakAlive) {
            // Check if active yesterday
            val yesterday = Calendar.getInstance()
            yesterday.add(Calendar.DAY_OF_YEAR, -1)
            if (isSameDay(sortedDates[0], yesterday.timeInMillis)) {
                isStreakAlive = true
            }
        }

        if (!isStreakAlive && sortedDates.isNotEmpty()) {
             return 0
        }
        
        val activeDayStrings = sortedDates.map { 
             val c = Calendar.getInstance().apply { timeInMillis = it }
             "${c.get(Calendar.DAY_OF_YEAR)}-${c.get(Calendar.YEAR)}"
        }.toSet()
        
        val checkCal = Calendar.getInstance()
        val todayKey = "${checkCal.get(Calendar.DAY_OF_YEAR)}-${checkCal.get(Calendar.YEAR)}"
        
        // If today not present, assume checking from yesterday
        if (!activeDayStrings.contains(todayKey)) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        while (true) {
            val key = "${checkCal.get(Calendar.DAY_OF_YEAR)}-${checkCal.get(Calendar.YEAR)}"
            if (activeDayStrings.contains(key)) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    // Returns standard key for today in "yyyy-MM-dd" format
    fun getTodayDateKey(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // 0-indexed
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    // Return 365 days integer level list ending today
    // Levels: 0 (None), 1 (1-2), 2 (3-5), 3 (6-9), 4 (10+)
    fun getYearActivityLevels(dailyStats: Map<String, Int>): List<Int> {
        val result = MutableList(365) { 0 }
        
        for (i in 0 until 365) {
             val cal = Calendar.getInstance()
             cal.add(Calendar.DAY_OF_YEAR, -(364 - i)) // i=364 -> Today
             
             val year = cal.get(Calendar.YEAR)
             val month = cal.get(Calendar.MONTH) + 1
             val day = cal.get(Calendar.DAY_OF_MONTH)
             val key = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
             
             val count = dailyStats[key] ?: 0
             
             // Map count to level
             val level = when {
                 count == 0 -> 0
                 count <= 2 -> 1
                 count <= 5 -> 2
                 count <= 9 -> 3
                 else -> 4
             }
             result[i] = level
        }
        return result
    }
}

