package com.iu.studytracker.util

import java.time.DayOfWeek
import java.time.LocalDate

object RecurrenceHelper {

    /**
     * Given a start date and an RRULE string, returns the next logical occurrence date.
     * If the rule is not valid or unsupported, returns null.
     */
    fun calculateNextOccurrence(currentDate: String, rrule: String): String? {
        if (rrule.isBlank()) return null
        
        return try {
            val date = LocalDate.parse(currentDate)
            
            // Simple parsing
            val map = rrule.split(";").associate { 
                val parts = it.split("=")
                parts[0] to (if (parts.size > 1) parts[1] else "")
            }
            
            val freq = map["FREQ"] ?: return null
            
            when (freq) {
                "DAILY" -> date.plusDays(1).toString()
                "WEEKLY" -> {
                    val byDay = map["BYDAY"]
                    if (byDay != null) {
                        // Complex weekly (e.g. MO,WE,FR)
                        val days = byDay.split(",").mapNotNull { parseDayOfWeek(it) }
                        if (days.isEmpty()) return date.plusWeeks(1).toString()
                        
                        var nextDate = date.plusDays(1)
                        // Look ahead up to 7 days
                        for (i in 1..7) {
                            if (days.contains(nextDate.dayOfWeek)) {
                                return nextDate.toString()
                            }
                            nextDate = nextDate.plusDays(1)
                        }
                        null
                    } else {
                        date.plusWeeks(1).toString()
                    }
                }
                "MONTHLY" -> date.plusMonths(1).toString()
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun parseDayOfWeek(dayStr: String): DayOfWeek? {
        return when (dayStr.uppercase()) {
            "MO" -> DayOfWeek.MONDAY
            "TU" -> DayOfWeek.TUESDAY
            "WE" -> DayOfWeek.WEDNESDAY
            "TH" -> DayOfWeek.THURSDAY
            "FR" -> DayOfWeek.FRIDAY
            "SA" -> DayOfWeek.SATURDAY
            "SU" -> DayOfWeek.SUNDAY
            else -> null
        }
    }
}
