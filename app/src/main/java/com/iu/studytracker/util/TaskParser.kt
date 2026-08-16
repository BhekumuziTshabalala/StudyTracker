package com.iu.studytracker.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class ParsedTaskResult(
    val cleanTitle: String,
    val dateString: String? // yyyy-MM-dd format
)

object TaskParser {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun parse(input: String, referenceDate: LocalDate = LocalDate.now()): ParsedTaskResult {
        var title = input.trim()
        var date: LocalDate? = null

        val lowerInput = input.lowercase()

        // Match "tomorrow"
        if (lowerInput.contains("tomorrow")) {
            date = referenceDate.plusDays(1)
            title = title.replace(Regex("(?i)\\btomorrow\\b"), "").trim()
        }
        // Match "today"
        else if (lowerInput.contains("today")) {
            date = referenceDate
            title = title.replace(Regex("(?i)\\btoday\\b"), "").trim()
        }
        // Match "next [day]" e.g., "next monday"
        else {
            val nextDayRegex = Regex("(?i)\\bnext\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b")
            val nextDayMatch = nextDayRegex.find(title)
            if (nextDayMatch != null) {
                val dayStr = nextDayMatch.groupValues[1].uppercase()
                val dayOfWeek = DayOfWeek.valueOf(dayStr)
                date = referenceDate.with(TemporalAdjusters.next(dayOfWeek))
                title = title.replace(nextDayMatch.value, "").trim()
            } else {
                // Match "on [day]" e.g., "on monday"
                val onDayRegex = Regex("(?i)\\bon\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b")
                val onDayMatch = onDayRegex.find(title)
                if (onDayMatch != null) {
                    val dayStr = onDayMatch.groupValues[1].uppercase()
                    val dayOfWeek = DayOfWeek.valueOf(dayStr)
                    date = referenceDate.with(TemporalAdjusters.nextOrSame(dayOfWeek))
                    title = title.replace(onDayMatch.value, "").trim()
                }
            }
        }

        // Clean up common leftovers like "at 5pm" (we aren't tracking time yet, so just clean it)
        val timeRegex = Regex("(?i)\\bat\\s+\\d{1,2}(:\\d{2})?\\s*(am|pm)?\\b")
        title = title.replace(timeRegex, "").trim()

        // Clean up extra spaces or hanging prepositions
        title = title.replace(Regex("\\s+"), " ")
        if (title.endsWith(" on", ignoreCase = true) || title.endsWith(" for", ignoreCase = true)) {
            title = title.substring(0, title.lastIndexOf(" ")).trim()
        }

        return ParsedTaskResult(
            cleanTitle = title.ifBlank { input },
            dateString = date?.format(dateFormatter)
        )
    }
}
