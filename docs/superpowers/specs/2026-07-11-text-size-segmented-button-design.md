# Text Size Selector: RadioGroup → MaterialButtonToggleGroup

**Date:** 2026-07-11  
**Scope:** `dialog_text_settings.xml`, `TextSettingsDialogFragment.kt`, `strings.xml`, `strings-ko-rKR.xml`

## Problem

The text size selector in `TextSettingsDialogFragment` uses a horizontal `RadioGroup` whose 5 `RadioButton` items are dynamically inflated at runtime with `weight=1`. Inside the narrow dialog, the full-text labels (소/중/대/특대/초대 or Small/Medium/Large/XLarge/XXLarge) get cramped and the row takes up disproportionate vertical space.

## Solution

Replace with `MaterialButtonToggleGroup` (Material 3 segmented button pattern) using S/M/L/XL/XXL abbreviations. The app already depends on `com.google.android.material`, so no new dependency is needed.

## Design

### 1. `dialog_text_settings.xml`

Remove `<RadioGroup android:id="@+id/sizeRadioGroup" .../>` and replace with:

```xml
<com.google.android.material.button.MaterialButtonToggleGroup
    android:id="@+id/sizeToggleGroup"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="4dp"
    app:singleSelection="true"
    app:selectionRequired="true">

    <Button
        style="@style/Widget.Material3.Button.OutlinedButton"
        android:id="@+id/sizeS"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:text="S" />

    <Button
        style="@style/Widget.Material3.Button.OutlinedButton"
        android:id="@+id/sizeM"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:text="M" />

    <Button
        style="@style/Widget.Material3.Button.OutlinedButton"
        android:id="@+id/sizeL"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:text="L" />

    <Button
        style="@style/Widget.Material3.Button.OutlinedButton"
        android:id="@+id/sizeXL"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:text="XL" />

    <Button
        style="@style/Widget.Material3.Button.OutlinedButton"
        android:id="@+id/sizeXXL"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:text="XXL" />

</com.google.android.material.button.MaterialButtonToggleGroup>
```

### 2. `TextSettingsDialogFragment.kt`

- Remove `private lateinit var sizeRadioGroup: RadioGroup` field and `buildSizeRadioButtons()` method.
- Add `private lateinit var sizeToggleGroup: MaterialButtonToggleGroup`.
- In `onCreateDialog`, bind the toggle group, check the button matching the saved `textSize` int (0–4), and register `addOnButtonCheckedListener` to persist on change.

```kotlin
private val sizeButtonIds = listOf(R.id.sizeS, R.id.sizeM, R.id.sizeL, R.id.sizeXL, R.id.sizeXXL)

private fun bindSizeToggleGroup() {
    val currentSize = prefs().getInt("textSize", 0).coerceIn(0, sizeButtonIds.lastIndex)
    sizeToggleGroup.check(sizeButtonIds[currentSize])
    sizeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
        if (isChecked) {
            val index = sizeButtonIds.indexOf(checkedId)
            if (index >= 0) {
                prefs().edit().putInt("textSize", index).apply()
                onSummaryRefreshNeeded?.invoke()
            }
        }
    }
}
```

### 3. String resources — no change needed

`textSizeSmall`–`textSizeXXLarge` are already S/M/L/XL/XXL in both `values/strings.xml` and `values-ko-rKR/strings.xml`. The new toggle buttons reference these same strings.

The `textSizeCategory` array must be **kept**: `MainActivity.textSettingsSummary()` (MainActivity.kt:436) uses it to render the preference summary ("#RRGGBB · S"). Only `TextSettingsDialogFragment`'s dynamic-RadioButton usage of the array goes away.

## Data Compatibility

`textSize` is stored as a raw `Int` (0–4) in `"SETTINGS"` SharedPreferences. The index mapping does not change, so existing user settings are preserved without migration.

## Out of Scope

- No change to how `textSize` is read in `MotivationLockerActivity`.
- No change to the color row in the same dialog.
- No runtime permission or service changes.
