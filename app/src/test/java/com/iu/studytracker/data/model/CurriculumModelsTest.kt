package com.iu.studytracker.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CurriculumModelsTest {

    @Test
    fun testParseCurriculumJson() {
        val json = """
{
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
            "Information representation[cite: 1]"
          ]
        }
      ]
    },
    {
      "semesters": [5, 6],
      "specialisations_and_seminars": [
        {
          "code": "DLBCSSCTCS[cite: 1]",
          "name": "Seminar: Current Topics in Computer Science[cite: 1]",
          "assessment": "Written Assessment: Research Essay[cite: 1]",
          "core_topics": [
            "dive deep into a specific topic within a sub-discipline of choice[cite: 1]"
          ]
        }
      ]
    }
  ]
}
        """.trimIndent()

        val gson = Gson()
        val curriculumData = gson.fromJson(json, CurriculumJson::class.java)
        
        assertNotNull(curriculumData)
        assertEquals("BSc Computer Science (FS-FI-BACS)[cite: 1]", curriculumData.programme)
        assertEquals(2, curriculumData.curriculum.size)
        
        val semester1 = curriculumData.curriculum[0]
        assertEquals(1, semester1.effectiveSemester)
        assertEquals(1, semester1.allModules.size)
        assertEquals("DLBCSICS[cite: 1]", semester1.allModules[0].code)

        val semester5 = curriculumData.curriculum[1]
        assertEquals(5, semester5.effectiveSemester)
        assertEquals(1, semester5.allModules.size)
        assertEquals("DLBCSSCTCS[cite: 1]", semester5.allModules[0].code)
    }
}
