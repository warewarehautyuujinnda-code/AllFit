# Final review fix wave report

Date: 2026-09-04 (Asia/Tokyo)

## Scope

- Home weekly weight card now shows current weight, goal weight, and the directional difference (`あと … kg` / `… kg 達成`).
- Home, Strength, and Running weekly calculations subscribe to a shared non-polling current-date flow. It emits on subscription and at the next local midnight; `WhileSubscribed` and `viewModelScope` cancel suspended waits safely.
- ExercisePicker merges pending planned exercises into its rows. Planned exercises absent from both presets and history appear in the frequent tab, remain selectable, and receive the existing planned flag badge.
- Added the requested focused regressions for running overachievement, record-side whitespace, and a different exercise remaining pending.
- Added WeeklyGoalSection KDoc at its existing placement immediately after the daily summary.
- Natural-key entities and append-only seed synchronization were not changed. Food, GPS tracking, and strength-record persistence behavior were not changed.

## RED

Command:

`./gradlew testDebugUnitTest --tests "com.hinata.fitlog.domain.PlanProgressTest" --tests "com.hinata.fitlog.domain.WeeklyUiTest"`

Expected failure observed before production implementation:

- `compileDebugUnitTestKotlin FAILED`
- unresolved `durationUntilNextDate`
- unresolved `exercisePickerRows`
- unresolved `weightGoalDifferenceLabel`

The three requested PlanProgress regressions characterize behavior already implemented in `PlanProgress.kt`; they required no production change.

The weight-card contract was then tightened to retain the explicit goal value. Before the formatter change, `WeeklyUiTest` ran 6 tests with 2 expected comparison failures.

## GREEN

Focused tests:

`./gradlew testDebugUnitTest --tests "com.hinata.fitlog.domain.PlanProgressTest" --tests "com.hinata.fitlog.domain.WeeklyUiTest"`

- `BUILD SUCCESSFUL in 11s`
- 26 actionable tasks: 6 executed, 20 up-to-date

Full unit tests (fresh tasks):

`./gradlew testDebugUnitTest --rerun-tasks`

- `BUILD SUCCESSFUL in 47s`
- 124 tests, 0 failures, 0 errors across 9 suites
- 26 actionable tasks: 26 executed

Debug build (fresh tasks):

`./gradlew assembleDebug --rerun-tasks`

- `BUILD SUCCESSFUL in 47s`
- 38 actionable tasks: 38 executed
- APK: `app/build/outputs/apk/debug/app-debug.apk`

Release build (fresh tasks):

`./gradlew assembleRelease --rerun-tasks`

- `BUILD SUCCESSFUL in 1m 1s`
- 51 actionable tasks: 51 executed
- APK: `app/build/outputs/apk/release/app-release.apk`

Both assemble runs reported the existing informational fallback that `libandroidx.graphics.path.so` could not be stripped and was packaged as-is. Neither build failed.

Additional check:

- `git diff --check`: no whitespace errors (only Git's line-ending conversion notices on Windows).
