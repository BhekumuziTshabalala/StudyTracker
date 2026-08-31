import os
import re

dir_path = "app/src/main/java/com/iu/studytracker/data/database/entity"

replacements = {
    "CurriculumModule.kt": [
        (r"val semester: Int,", r"val semester: Int = 0,"),
        (r"val code: String,", r"val code: String = \"\","),
        (r"val name: String,", r"val name: String = \"\","),
        (r"val assessment: String,", r"val assessment: String = \"\",")
    ],
    "CurriculumTopic.kt": [
        (r"val title: String,", r"val title: String = \"\",")
    ],
    "Module.kt": [
        (r"val monthPlanId: String,", r"val monthPlanId: String = \"\","),
        (r"val name: String,", r"val name: String = \"\","),
        (r"val orderIndex: Int,", r"val orderIndex: Int = 0,")
    ],
    "ModuleScheduleEvent.kt": [
        (r"val curriculumModuleId: String,", r"val curriculumModuleId: String = \"\","),
        (r"val title: String,", r"val title: String = \"\","),
        (r"val eventType: EventType,", r"val eventType: EventType = EventType.STUDY_BLOCK,")
    ],
    "ModuleTask.kt": [
        (r"val curriculumModuleId: String,", r"val curriculumModuleId: String = \"\","),
        (r"val title: String,", r"val title: String = \"\","),
        (r"val description: String,", r"val description: String = \"\","),
        (r"val type: TaskType,", r"val type: TaskType = TaskType.ASSIGNMENT,")
    ],
    "MonthPlan.kt": [
        (r"val year: Int,", r"val year: Int = 0,"),
        (r"val month: Int,", r"val month: Int = 0,")
    ],
    "Task.kt": [
        (r"val title: String,", r"val title: String = \"\",")
    ],
    "TaskTemplate.kt": [
        (r"val title: String,", r"val title: String = \"\",")
    ],
    "Topic.kt": [
        (r"val moduleId: String,", r"val moduleId: String = \"\","),
        (r"val title: String,", r"val title: String = \"\","),
        (r"val orderIndex: Int,", r"val orderIndex: Int = 0,")
    ]
}

for filename, rules in replacements.items():
    filepath = os.path.join(dir_path, filename)
    if not os.path.exists(filepath):
        print(f"Skipping {filepath}")
        continue
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    
    for old, new in rules:
        content = re.sub(old, new, content)
    
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"Patched {filename}")
