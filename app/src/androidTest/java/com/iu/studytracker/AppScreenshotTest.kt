package com.iu.studytracker

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.iu.studytracker.util.ScreenshotUtil
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AppScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureAllScreenshots() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = StudyRepository(context)
        
        runBlocking {
            // 1. Inject the Curriculum Mock Data
            repository.importCurriculumFromJson(MOCK_DATA)
            
            // 2. Fetch the modules and setup a month plan using the first two
            val modules = repository.getAllCurriculumModulesSync()
            if (modules.size >= 2) {
                val now = LocalDate.now()
                repository.setupMonthWithCurriculumModules(
                    year = now.year,
                    month = now.monthValue,
                    moduleIds = listOf(modules[0].id, modules[1].id),
                    startFrom = now
                )
            }
        }
        
        // Let the app settle
        composeTestRule.waitForIdle()
        Thread.sleep(60000)
    }

    companion object {
        const val MOCK_DATA = """{
  "programme": "BSc Computer Science (FS-FI-BACS)[cite: 1]",
  "total_credit_points": "180 CP[cite: 1]",
  "curriculum": [
    {
      "semester": 1,
      "modules": [
        {
          "code": "DLBCSICS[cite: 1]",
          "name": "Introduction to Computer Science[cite: 1]",
          "assessment": "Exam, 90 Minutes[cite: 1]",
          "core_topics": [
            "Information representation[cite: 1]",
            "Algorithms and data structures[cite: 1]",
            "Propositional logic / Boolean algebra[cite: 1]",
            "Hardware[cite: 1]",
            "Networks and the internet[cite: 1]",
            "Software[cite: 1]",
            "Computer science as a discipline[cite: 1]"
          ]
        },
        {
          "code": "DLBIAWITT[cite: 1]",
          "name": "Introduction to Academic Work for IT and Technology[cite: 1]",
          "assessment": "Advanced Workbook[cite: 1]",
          "core_topics": [
            "Everyday Knowledge vs. Academic Work[cite: 1]",
            "Working with Sources and Literature[cite: 1]",
            "Research Design[cite: 1]",
            "Writing an Academic Paper[cite: 1]"
          ]
        },
        {
          "code": "DLBCSM1[cite: 1]",
          "name": "Mathematics I[cite: 1]",
          "assessment": "Exam, 90 Minutes[cite: 1]",
          "core_topics": [
            "Basic definitions and terms of discrete mathematics[cite: 1]",
            "Sets and propositional logic[cite: 1]",
            "Number systems such as decimal and binary systems[cite: 1]",
            "Graphs and mappings[cite: 1]",
            "Selected topics of elementary number theory[cite: 1]",
            "Cryptography[cite: 1]"
          ]
        },
        {
          "code": "DLBCSOOPJ[cite: 1]",
          "name": "Object-oriented Programming with Java[cite: 1]",
          "assessment": "Exam, 90 Minutes[cite: 1]",
          "core_topics": [
            "Introduction to the Java language[cite: 1]",
            "Java language constructs[cite: 1]",
            "Introduction to object-oriented system developement[cite: 1]",
            "Inheritance[cite: 1]",
            "Object-oriented concepts[cite: 1]",
            "Exception handling[cite: 1]",
            "Interfaces[cite: 1]"
          ]
        },
        {
          "code": "DLBCSDSJCL[cite: 1]",
          "name": "Data Structures and Java Class Library[cite: 1]",
          "assessment": "Exam, 90 Minutes[cite: 1]",
          "core_topics": [
            "Programming style[cite: 1]",
            "Working with objects[cite: 1]",
            "External packages and libraries[cite: 1]",
            "Data structures[cite: 1]",
            "Strings and calendar[cite: 1]",
            "File system and data streams[cite: 1]"
          ]
        },
        {
          "code": "DLBCSIDM[cite: 1]",
          "name": "Intercultural and Ethical Decision-Making[cite: 1]",
          "assessment": "Written Assessment: Case Study[cite: 1]",
          "core_topics": [
            "Basics of Intercultural Competence[cite: 1]",
            "Cultural Concepts[cite: 1]",
            "Culture and Ethics[cite: 1]",
            "Intercultural Learning and Working[cite: 1]",
            "Case Studies for Cultural and Ethical Conflicts[cite: 1]"
          ]
        }
      ]
    },
    {
      "semester": 2,
      "modules": [
        {
          "code": "DLBCSM2[cite: 1]",
          "name": "Mathematics II[cite: 1]",
          "assessment": "Exam, 90 Minutes[cite: 1]",
          "core_topics": [
            "Selected Topics of Linear Algebra[cite: 1]",
            "Selected Chapters on Graphs and Algorithms[cite: 1]"
          ]
        }
      ]
    }
  ]
}"""
    }
}
