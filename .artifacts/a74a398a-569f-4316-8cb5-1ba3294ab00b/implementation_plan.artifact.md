# Fix ECTS Display Issue (1801 vs 180)

The issue where ECTS appears as "1801" instead of "180" is caused by the citation index (e.g., `[cite: 1]`) being incorrectly included during the digit extraction process in the curriculum importer.

## Proposed Changes

### Data & Repository Layer

#### [MODIFY] [StudyRepository.kt](file:///C:/Code/StudyTracker/app/src/main/java/com/iu/studytracker/data/repository/StudyRepository.kt)
- Update `importCurriculumFromJson` to use a more robust regex that extracts only the first sequence of digits for the total credit points, preventing appended citation numbers from corrupting the value.

### UI & ViewModel Layer

#### [MODIFY] [DashboardViewModel.kt](file:///C:/Code/StudyTracker/app/src/main/java/com/iu/studytracker/ui/screen/dashboard/DashboardViewModel.kt)
- Synchronize `totalEcts` in the Dashboard with the `DegreePlan` stored in the database. Currently, it is hardcoded to 180 in the UI state and doesn't reflect database updates.

#### [MODIFY] [RoadmapViewModel.kt](file:///C:/Code/StudyTracker/app/src/main/java/com/iu/studytracker/ui/screen/roadmap/RoadmapViewModel.kt)
- Add a one-time "self-healing" check that resets `totalCreditsRequired` to 180 if it is found to be 1801 in the database, ensuring existing users get the fix immediately without re-importing.

#### [MODIFY] [SettingsViewModel.kt](file:///C:/Code/StudyTracker/app/src/main/java/com/iu/studytracker/ui/screen/settings/SettingsViewModel.kt)
- Add functions to update Graduation Date and Total ECTS in the `DegreePlan`.

#### [MODIFY] [SettingsScreen.kt](file:///C:/Code/StudyTracker/app/src/main/java/com/iu/studytracker/ui/screen/settings/SettingsScreen.kt)
- Add UI fields to allow users to manually edit their Graduation Date and Total ECTS, providing a way to correct or customize these values.

## Verification Plan

### Automated Tests
- Run existing `CurriculumModelsTest` to ensure basic parsing still works.
- Verify that `Regex("\\d+").find("180 CP[cite: 1]")?.value` returns "180".

### Manual Verification
- Deploy the app and check the Roadmap and Dashboard screens.
- Verify that the ECTS value is corrected to 180.
- Test the new settings in the Settings screen to ensure they update the Degree Plan.
