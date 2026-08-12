# Walkthrough - ECTS Fix & Settings Update

I have fixed the issue where the ECTS total was incorrectly displaying as "1801" and added new settings to allow manual management of degree progress.

## Changes Made

### 1. Robust ECTS Parsing
In `StudyRepository.kt`, I updated the curriculum import logic to use a safer regular expression (`Regex("\\d+")`). This ensures that only the first sequence of digits is extracted from strings like `"180 CP[cite: 1]"`, preventing the citation number from being appended to the total.

### 2. Dashboard Synchronization
Updated `DashboardViewModel.kt` to reactively observe the `DegreePlan` from the database. This ensures that the `totalEcts` displayed on the Dashboard stays in sync with any updates made during curriculum imports or manual edits.

### 3. Automated Self-Healing
Added a self-healing check in `RoadmapViewModel.kt`. When the roadmap is loaded, the app now checks if the stored ECTS total is exactly `1801`. If so, it automatically corrects it to `180`, providing an immediate fix for existing users.

### 4. Degree Progress Settings
Enhanced the Settings screen to give you more control:
- **Target Graduation**: You can now manually update your graduation date (e.g., "July 2027").
- **Total ECTS**: You can manually adjust the total ECTS required for your degree.
- Added reactive state management in `SettingsViewModel.kt` to persist these changes to the `DegreePlan` table.

## Verification
- **Build**: Successfully compiled the project using `:app:compileDebugKotlin`.
- **Logic**: Verified that the new regex correctly handles citation-heavy strings.
- **UI**: Added `verticalScroll` to the Settings screen to ensure usability on smaller devices as more settings are added.

render_diffs(file:///C:/Code/StudyTracker/app/src/main/java/com/iu/studytracker/data/repository/StudyRepository.kt)
render_diffs(file:///C:/Code/StudyTracker/app/src/main/java/com/iu/studytracker/ui/screen/dashboard/DashboardViewModel.kt)
render_diffs(file:///C:/Code/StudyTracker/app/src/main/java/com/iu/studytracker/ui/screen/roadmap/RoadmapViewModel.kt)
render_diffs(file:///C:/Code/StudyTracker/app/src/main/java/com/iu/studytracker/ui/screen/settings/SettingsViewModel.kt)
render_diffs(file:///C:/Code/StudyTracker/app/src/main/java/com/iu/studytracker/ui/screen/settings/SettingsScreen.kt)
