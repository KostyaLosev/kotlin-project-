package com.example.travelmemories.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import com.example.travelmemories.R
import com.example.travelmemories.data.MemoryType
import com.example.travelmemories.data.SqliteTravelMemoryRepository
import com.example.travelmemories.data.TravelMemory
import com.example.travelmemories.data.TravelMemoryRepository
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditMemoryActivity : Activity() {
    private lateinit var repository: TravelMemoryRepository
    private var existingMemory: TravelMemory? = null

    private lateinit var typeGroup: RadioGroup
    private lateinit var cityRadio: RadioButton
    private lateinit var attractionRadio: RadioButton
    private lateinit var titleInput: EditText
    private lateinit var countryInput: EditText
    private lateinit var visitDateInput: EditText
    private lateinit var ratingSpinner: Spinner
    private lateinit var noteInput: EditText
    private lateinit var photoPreview: ImageView
    private var selectedPhotoUri: String? = null
    private var initialSnapshot: FormSnapshot? = null
    private var skipDiscardPrompt = false

    private val primaryColor = Color.rgb(165, 214, 167)
    private val textPrimary = Color.rgb(33, 33, 33)
    private val textSecondary = Color.rgb(117, 117, 117)
    private val accentColor = Color.rgb(128, 203, 196)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = SqliteTravelMemoryRepository(this)

        val memoryId = intent.getLongExtra(EXTRA_MEMORY_ID, 0L)
        existingMemory = if (memoryId > 0) repository.getById(memoryId) else null

        setContentView(createContent())
        existingMemory?.let { bindMemory(it) }
        initialSnapshot = currentSnapshot()
    }

    override fun onBackPressed() {
        confirmDiscardOrFinish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PHOTO || resultCode != RESULT_OK) {
            return
        }

        val uri = data?.data ?: return
        val flags = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // Some providers return readable URIs without persistable permissions.
        }
        selectedPhotoUri = uri.toString()
        updatePhotoPreview()
    }

    private fun createContent(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val toolbar = Toolbar(this).apply {
            title = if (existingMemory == null) "Add Memory" else "Memory Details"
            setTitleTextColor(textPrimary)
            setBackgroundColor(primaryColor)
            minimumHeight = dp(56)
            elevation = dp(4).toFloat()
            setNavigationIcon(R.drawable.ic_arrow_back_24)
            setNavigationOnClickListener { confirmDiscardOrFinish() }
            setPadding(dp(8), 0, dp(16), 0)
        }
        root.addView(toolbar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(56),
        ))

        val scroll = ScrollView(this)
        val form = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            createLandscapeForm()
        } else {
            createPortraitForm()
        }
        scroll.addView(form)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f,
        ))

        return root
    }

    private fun createPortraitForm(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(24))

            addView(createTypeSelector())
            addView(labeledInput("Title", createTitleInput()))
            addView(labeledInput("Country", createCountryInput()))
            addView(labeledInput("Visit Date", createVisitDateField(), DATE_FORMAT_HINT))
            addView(labeledInput("Rating", createRatingSpinner()))
            addView(labeledInput("Photo", createPhotoInput()))
            addView(labeledInput("Note", createNoteInput()))
            addView(createSaveButton())
        }
    }

    private fun createLandscapeForm(): LinearLayout {
        val leftColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(createTypeSelector())
            addView(labeledInput("Title", createTitleInput()))
            addView(labeledInput("Country", createCountryInput()))
            addView(labeledInput("Visit Date", createVisitDateField(), DATE_FORMAT_HINT))
            addView(labeledInput("Rating", createRatingSpinner()))
        }

        val rightColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(labeledInput("Photo", createPhotoInput()))
            addView(labeledInput("Note", createNoteInput()))
            addView(createSaveButton())
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(18), dp(16), dp(18), dp(24))
            addView(leftColumn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dp(12), 0)
            })
            addView(rightColumn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(12), 0, 0, 0)
            })
        }
    }

    private fun createTypeSelector(): LinearLayout {
        typeGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
        }
        cityRadio = RadioButton(this).apply {
            text = "City"
            id = TYPE_CITY_ID
            setTextColor(textPrimary)
        }
        attractionRadio = RadioButton(this).apply {
            text = "Attraction"
            id = TYPE_ATTRACTION_ID
            setTextColor(textPrimary)
        }
        typeGroup.addView(cityRadio)
        typeGroup.addView(attractionRadio)
        typeGroup.check(TYPE_CITY_ID)

        return labeledInput("Type", typeGroup)
    }

    private fun createTitleInput(): EditText {
        titleInput = baseEditText("Name of city or attraction").apply {
            filters = arrayOf(InputFilter.LengthFilter(MAX_TITLE_LENGTH))
        }
        return titleInput
    }

    private fun createCountryInput(): EditText {
        countryInput = baseEditText("Country").apply {
            filters = arrayOf(InputFilter.LengthFilter(MAX_COUNTRY_LENGTH))
        }
        return countryInput
    }

    private fun createVisitDateInput(): EditText {
        visitDateInput = baseEditText(DATE_FORMAT_HINT).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            addTextChangedListener(object : TextWatcher {
                private var isUpdating = false

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(s: Editable?) {
                    if (isUpdating || s == null) {
                        return
                    }

                    val formatted = formatVisitDateInput(s.toString())
                    if (s.toString() == formatted) {
                        return
                    }

                    isUpdating = true
                    setText(formatted)
                    setSelection(formatted.length)
                    isUpdating = false
                }
            })
        }
        return visitDateInput
    }

    private fun createVisitDateField(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(createVisitDateInput(), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(Button(this@EditMemoryActivity).apply {
                text = "Pick"
                isAllCaps = false
                textSize = 13f
                setTextColor(textPrimary)
                setOnClickListener {
                    showDatePicker()
                }
            }, LinearLayout.LayoutParams(dp(86), dp(48)).apply {
                setMargins(dp(10), 0, 0, 0)
            })
        }
    }

    private fun createRatingSpinner(): Spinner {
        ratingSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@EditMemoryActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("1", "2", "3", "4", "5"),
            )
        }
        return ratingSpinner
    }

    private fun createNoteInput(): EditText {
        noteInput = baseEditText("Your travel note").apply {
            filters = arrayOf(InputFilter.LengthFilter(MAX_NOTE_LENGTH))
            minLines = 5
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        return noteInput
    }

    private fun createPhotoInput(): LinearLayout {
        photoPreview = ImageView(this).apply {
            background = roundedBackground(Color.rgb(245, 245, 245), 8, this@EditMemoryActivity)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Selected travel photo"
        }

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(Button(this@EditMemoryActivity).apply {
                text = "Choose"
                isAllCaps = false
                textSize = 13f
                setTextColor(textPrimary)
                setOnClickListener {
                    openPhotoPicker()
                }
            }, LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                setMargins(0, dp(8), dp(6), 0)
            })
            addView(Button(this@EditMemoryActivity).apply {
                text = "Remove"
                isAllCaps = false
                textSize = 13f
                setTextColor(textPrimary)
                setOnClickListener {
                    selectedPhotoUri = null
                    updatePhotoPreview()
                }
            }, LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                setMargins(dp(6), dp(8), 0, 0)
            })
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(photoPreview, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(140),
            ))
            addView(buttonRow)
            updatePhotoPreview()
        }
    }

    private fun baseEditText(hintValue: String): EditText {
        return EditText(this).apply {
            hint = hintValue
            textSize = 16f
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
            setSingleLine(false)
            backgroundTintList = android.content.res.ColorStateList.valueOf(accentColor)
        }
    }

    private fun labeledInput(label: String, input: android.view.View, helperText: String? = null): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(14))
            addView(TextView(this@EditMemoryActivity).apply {
                text = label
                textSize = 14f
                setTextColor(textSecondary)
            })
            addView(input, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ))
            helperText?.let { textValue ->
                addView(TextView(this@EditMemoryActivity).apply {
                    text = textValue
                    textSize = 12f
                    setTextColor(textSecondary)
                    setPadding(0, dp(2), 0, 0)
                })
            }
        }
    }

    private fun createSaveButton(): Button {
        return Button(this).apply {
            text = "Save"
            isAllCaps = false
            textSize = 16f
            setTextColor(textPrimary)
            background = roundedBackground(primaryColor, 8, this@EditMemoryActivity)
            setOnClickListener {
                saveMemory()
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52),
            ).apply {
                setMargins(0, dp(8), 0, 0)
            }
        }
    }

    private fun bindMemory(memory: TravelMemory) {
        if (memory.type == MemoryType.City) {
            typeGroup.check(TYPE_CITY_ID)
        } else {
            typeGroup.check(TYPE_ATTRACTION_ID)
        }
        typeGroup.isEnabled = false
        cityRadio.isEnabled = false
        attractionRadio.isEnabled = false

        titleInput.setText(memory.title)
        countryInput.setText(memory.country)
        visitDateInput.setText(memory.visitDate)
        ratingSpinner.setSelection((memory.rating - 1).coerceIn(0, 4))
        noteInput.setText(memory.note)
        selectedPhotoUri = memory.photoUri
        updatePhotoPreview()
    }

    private fun saveMemory() {
        val title = titleInput.text.toString().trim()
        val country = countryInput.text.toString().trim()
        val visitDate = visitDateInput.text.toString().trim()
        val note = noteInput.text.toString().trim()

        if (title.isBlank() || country.isBlank() || visitDate.isBlank()) {
            Toast.makeText(this, "Title, country and visit date are required.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isWithinTextLimits(title, country, note)) {
            return
        }

        if (!isValidVisitDate(visitDate)) {
            Toast.makeText(this, "Visit date must be a real date in YYYY-MM-DD format.", Toast.LENGTH_SHORT).show()
            visitDateInput.requestFocus()
            return
        }

        val type = existingMemory?.type ?: if (typeGroup.checkedRadioButtonId == TYPE_ATTRACTION_ID) {
            MemoryType.Attraction
        } else {
            MemoryType.City
        }

        val memory = TravelMemory(
            id = existingMemory?.id ?: 0L,
            type = type,
            title = title,
            country = country,
            visitDate = visitDate,
            rating = ratingSpinner.selectedItem.toString().toInt(),
            note = note,
            photoUri = selectedPhotoUri,
        )

        repository.save(memory)
        skipDiscardPrompt = true
        Toast.makeText(this, "Memory saved.", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun isValidVisitDate(value: String): Boolean {
        if (!DATE_PATTERN.matches(value)) {
            return false
        }

        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }

        return try {
            formatter.parse(value)
            true
        } catch (_: ParseException) {
            false
        }
    }

    private fun isWithinTextLimits(title: String, country: String, note: String): Boolean {
        return when {
            title.length > MAX_TITLE_LENGTH -> {
                Toast.makeText(this, "Title can be up to $MAX_TITLE_LENGTH characters.", Toast.LENGTH_SHORT).show()
                titleInput.requestFocus()
                false
            }
            country.length > MAX_COUNTRY_LENGTH -> {
                Toast.makeText(this, "Country can be up to $MAX_COUNTRY_LENGTH characters.", Toast.LENGTH_SHORT).show()
                countryInput.requestFocus()
                false
            }
            note.length > MAX_NOTE_LENGTH -> {
                Toast.makeText(this, "Note can be up to $MAX_NOTE_LENGTH characters.", Toast.LENGTH_SHORT).show()
                noteInput.requestFocus()
                false
            }
            else -> true
        }
    }

    private fun formatVisitDateInput(value: String): String {
        val digits = value.filter { it.isDigit() }.take(8)
        return buildString {
            digits.forEachIndexed { index, char ->
                if (index == 4 || index == 6) {
                    append("-")
                }
                append(char)
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
        try {
            val parsedDate = formatter.parse(visitDateInput.text.toString())
            if (parsedDate != null) {
                calendar.time = parsedDate
            }
        } catch (_: ParseException) {
            // Keep today's date when the current input is incomplete.
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                visitDateInput.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun openPhotoPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_PHOTO)
    }

    private fun updatePhotoPreview() {
        val uri = selectedPhotoUri
        if (uri.isNullOrBlank()) {
            photoPreview.setImageDrawable(null)
            return
        }
        photoPreview.setImageURI(Uri.parse(uri))
    }

    private fun confirmDiscardOrFinish() {
        if (skipDiscardPrompt || initialSnapshot == currentSnapshot()) {
            finish()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Discard changes?")
            .setMessage("You have unsaved changes.")
            .setPositiveButton("Discard") { _, _ ->
                skipDiscardPrompt = true
                finish()
            }
            .setNegativeButton("Keep editing", null)
            .show()
    }

    private fun currentSnapshot(): FormSnapshot {
        val checkedType = if (typeGroup.checkedRadioButtonId == TYPE_ATTRACTION_ID) {
            MemoryType.Attraction
        } else {
            MemoryType.City
        }
        return FormSnapshot(
            type = checkedType,
            title = titleInput.text.toString(),
            country = countryInput.text.toString(),
            visitDate = visitDateInput.text.toString(),
            rating = ratingSpinner.selectedItem?.toString().orEmpty(),
            note = noteInput.text.toString(),
            photoUri = selectedPhotoUri,
        )
    }

    private data class FormSnapshot(
        val type: MemoryType,
        val title: String,
        val country: String,
        val visitDate: String,
        val rating: String,
        val note: String,
        val photoUri: String?,
    )

    companion object {
        const val EXTRA_MEMORY_ID = "memory_id"
        private const val TYPE_CITY_ID = 1001
        private const val TYPE_ATTRACTION_ID = 1002
        private const val REQUEST_PHOTO = 2001
        private const val MAX_TITLE_LENGTH = 60
        private const val MAX_COUNTRY_LENGTH = 40
        private const val MAX_NOTE_LENGTH = 500
        private const val DATE_FORMAT_HINT = "YYYY-MM-DD"
        private val DATE_PATTERN = Regex("""\d{4}-\d{2}-\d{2}""")
    }
}
