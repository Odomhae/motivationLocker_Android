# Text Size Segmented Button Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the horizontal RadioGroup text-size picker in the text-settings dialog with a Material 3 segmented button (`MaterialButtonToggleGroup`) so it takes less space.

**Architecture:** The 5 dynamically-inflated `RadioButton`s in `TextSettingsDialogFragment` become 5 statically-declared outlined `Button`s inside a `MaterialButtonToggleGroup` in `dialog_text_settings.xml`. The fragment maps the saved `textSize` int (0–4, in the `"SETTINGS"` SharedPreferences file) to/from button ids by position. No data migration — the stored int and its meaning are unchanged.

**Tech Stack:** Kotlin, `com.google.android.material:material` (already a dependency — app theme is `Theme.Material3.Light.NoActionBar`), Gradle.

**Spec:** `docs/superpowers/specs/2026-07-11-text-size-segmented-button-design.md`

## Global Constraints

- `textSize` stays a raw `Int` 0–4 in the `"SETTINGS"` SharedPreferences file; index order S→XXL must not change (persisted per-user).
- Keep the `textSizeCategory` string-array — `MainActivity.textSettingsSummary()` (MainActivity.kt:436) still reads it for the preference summary. Do NOT delete it.
- No new dependencies.
- Build requires JDK 11+; the system JDK is Java 8. Prefix Gradle commands (Git Bash): `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`.
- This is a pure view-layer change with no JVM-testable logic; verification is compile + existing unit tests (`QuoteRepositoryTest` etc. must stay green).

---

### Task 1: Replace RadioGroup with MaterialButtonToggleGroup

**Files:**
- Modify: `app/src/main/res/layout/dialog_text_settings.xml`
- Modify: `app/src/main/java/com/odom/motivationlocker/TextSettingsDialogFragment.kt`

**Interfaces:**
- Consumes: `prefs()` helper already in the fragment (`"SETTINGS"` SharedPreferences); string resources `textSizeSmall`–`textSizeXXLarge` (already "S"/"M"/"L"/"XL"/"XXL" in both locales).
- Produces: nothing consumed by later tasks (single-task plan). External contract preserved: `textSize` int pref written on selection; `onSummaryRefreshNeeded` invoked after write.

- [ ] **Step 1: Replace the RadioGroup block in the layout**

In `app/src/main/res/layout/dialog_text_settings.xml`, two changes:

(a) Add the `app` namespace to the root element (currently only `xmlns:android` is declared):

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
```

(b) Replace the entire `<RadioGroup ... />` element (currently lines 52–57, id `sizeRadioGroup`) with:

```xml
    <com.google.android.material.button.MaterialButtonToggleGroup
        android:id="@+id/sizeToggleGroup"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="4dp"
        app:singleSelection="true"
        app:selectionRequired="true">

        <Button
            android:id="@+id/sizeS"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:minWidth="0dp"
            android:paddingStart="0dp"
            android:paddingEnd="0dp"
            android:text="@string/textSizeSmall" />

        <Button
            android:id="@+id/sizeM"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:minWidth="0dp"
            android:paddingStart="0dp"
            android:paddingEnd="0dp"
            android:text="@string/textSizeMedium" />

        <Button
            android:id="@+id/sizeL"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:minWidth="0dp"
            android:paddingStart="0dp"
            android:paddingEnd="0dp"
            android:text="@string/textSizeLarge" />

        <Button
            android:id="@+id/sizeXL"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:minWidth="0dp"
            android:paddingStart="0dp"
            android:paddingEnd="0dp"
            android:text="@string/textSizeXLarge" />

        <Button
            android:id="@+id/sizeXXL"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:minWidth="0dp"
            android:paddingStart="0dp"
            android:paddingEnd="0dp"
            android:text="@string/textSizeXXLarge" />

    </com.google.android.material.button.MaterialButtonToggleGroup>
```

Notes for the implementer:
- `android:minWidth="0dp"` matters: `MaterialButton`'s default 88dp minWidth would overflow 5 weighted buttons on narrow screens.
- Plain `<Button>` (not `<com.google.android.material.button.MaterialButton>`) is correct — the Material theme's view inflater turns it into a `MaterialButton`, and `MaterialButtonToggleGroup` requires that. Do not use `androidx.appcompat.widget.AppCompatButton`.

- [ ] **Step 2: Rewrite the size-selection code in the fragment**

Replace `app/src/main/java/com/odom/motivationlocker/TextSettingsDialogFragment.kt` with:

```kotlin
package com.odom.motivationlocker

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.odom.ledscreen.ColorSelectorDialogBuilder

// "글자 설정" 1줄을 눌렀을 때 뜨는 팝업. 글자색(기존 ColorSelectorDialog 재사용)과
// 글자 크기를 한 다이얼로그에서 고르게 한다.
class TextSettingsDialogFragment : DialogFragment(), ColorSelectorDialog.OnDialogColorClickListener {

    var onSummaryRefreshNeeded: (() -> Unit)? = null
    var onColorChangeTracked: (() -> Unit)? = null

    private lateinit var rowTextColor: View
    private lateinit var textColorPreview: ImageView
    private lateinit var sizeToggleGroup: MaterialButtonToggleGroup

    // XML의 버튼 선언 순서와 저장되는 textSize 인덱스(0=S … 4=XXL)를 잇는 유일한 매핑.
    private val sizeButtonIds =
        listOf(R.id.sizeS, R.id.sizeM, R.id.sizeL, R.id.sizeXL, R.id.sizeXXL)

    private fun prefs() = requireContext().getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_text_settings, null)

        rowTextColor = view.findViewById(R.id.rowTextColor)
        textColorPreview = view.findViewById(R.id.textColorPreview)
        sizeToggleGroup = view.findViewById(R.id.sizeToggleGroup)

        refreshTextColorPreview()
        bindSizeToggleGroup()

        rowTextColor.setOnClickListener { showColorPicker() }

        return AlertDialog.Builder(context)
            .setTitle(R.string.text_settings_title)
            .setView(view)
            .setPositiveButton(R.string.settings_dialog_close, null)
            .create()
    }

    private fun currentTextColor(): Int = prefs().getInt("textColorCategory", Color.BLACK)

    private fun refreshTextColorPreview() {
        textColorPreview.setBackgroundColor(currentTextColor())
    }

    // MotivationLockerActivity/기존 요약 표시 모두 "textSize" 미설정 시 0(=S)을 기본값으로
    // 취급해 왔으므로(펜딩되던 pref.xml ListPreference의 defaultValue="1"은 실제로 읽히지 않던
    // 죽은 값), 여기서도 0을 기본값으로 맞춘다.
    private fun bindSizeToggleGroup() {
        val currentSize = prefs().getInt("textSize", 0).coerceIn(0, sizeButtonIds.lastIndex)
        sizeToggleGroup.check(sizeButtonIds[currentSize])
        sizeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val index = sizeButtonIds.indexOf(checkedId)
            if (index >= 0) {
                prefs().edit().putInt("textSize", index).apply()
                onSummaryRefreshNeeded?.invoke()
            }
        }
    }

    private fun showColorPicker() {
        val dialog = ColorSelectorDialogBuilder()
            .setColorList(ColorPalette.ALL)
            .setFigureType(FigureType.CIRCLE)
            .setSelectedColor(ColorPalette.resIdForColor(requireContext(), currentTextColor()))
            .setOnDialogColorClickListener(this)
            .build()
        dialog.show(childFragmentManager, "textColorPicker")
    }

    override fun onColorClick(tagDialog: String, selectedColor: Int?) {
        selectedColor?.let { colorResId ->
            val actualColor = ContextCompat.getColor(requireContext(), colorResId)
            prefs().edit().putInt("textColorCategory", actualColor).apply()
            refreshTextColorPreview()
            onSummaryRefreshNeeded?.invoke()
            onColorChangeTracked?.invoke()
        }
    }
}
```

Diff summary vs. the current file (everything else is identical): imports drop `RadioButton`/`RadioGroup` and gain `MaterialButtonToggleGroup`; field `sizeRadioGroup: RadioGroup` → `sizeToggleGroup: MaterialButtonToggleGroup` plus the new `sizeButtonIds` list; `buildSizeRadioButtons()` → `bindSizeToggleGroup()` (the existing Korean comment about default 0 stays, moved onto the new method).

- [ ] **Step 3: Build and run unit tests**

Run (Git Bash, from repo root):

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew assembleDebug testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`. Compile failure mentioning `sizeRadioGroup` means Step 2's fragment rewrite wasn't applied; `Style attribute '@style/Widget.Material3.Button.OutlinedButton' not found` would mean the Material dependency/theme assumption broke (it shouldn't — check `app/build.gradle` for `com.google.android.material:material`).

- [ ] **Step 4: Manual smoke check (emulator/device, optional but recommended)**

Install the debug APK, open the app → "글자 설정" row. Verify:
1. Segmented bar shows S M L XL XXL in one row, previously saved size pre-selected.
2. Tapping a segment updates the preference-row summary (the "#RRGGBB · S" line) immediately.
3. Tapping the already-selected segment does not deselect it (`selectionRequired`).
4. Lock screen (`MotivationLockerActivity`) renders the chosen size as before.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/layout/dialog_text_settings.xml \
        app/src/main/java/com/odom/motivationlocker/TextSettingsDialogFragment.kt \
        docs/superpowers
git commit -m "글자 크기 선택 RadioGroup → Material3 세그먼트 버튼 전환

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```
