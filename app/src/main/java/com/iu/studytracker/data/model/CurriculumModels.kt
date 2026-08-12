package com.iu.studytracker.data.model

import com.google.gson.annotations.SerializedName

data class CurriculumJson(
    @SerializedName("programme")
    val programme: String,
    @SerializedName("total_credit_points")
    val totalCreditPoints: String,
    @SerializedName("curriculum")
    val curriculum: List<SemesterJson>
)

data class SemesterJson(
    @SerializedName("semester")
    val semester: Int? = null,
    @SerializedName("semesters")
    val semesters: List<Int>? = null,
    @SerializedName("modules")
    val modules: List<ModuleJson>? = null,
    @SerializedName("specialisations_and_seminars")
    val specialisationsAndSeminars: List<ModuleJson>? = null
) {
    // Helper to get the effective semester (first one if multiple)
    val effectiveSemester: Int
        get() = semester ?: semesters?.firstOrNull() ?: 0

    // Helper to get all modules
    val allModules: List<ModuleJson>
        get() = (modules ?: emptyList()) + (specialisationsAndSeminars ?: emptyList())
}

data class ModuleJson(
    @SerializedName("code")
    val code: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("assessment")
    val assessment: String,
    @SerializedName("core_topics")
    val coreTopics: List<String>
)
