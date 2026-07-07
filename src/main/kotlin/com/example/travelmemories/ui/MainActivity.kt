package com.example.travelmemories.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toolbar
import com.example.travelmemories.R
import com.example.travelmemories.data.MemoryType
import com.example.travelmemories.data.SqliteTravelMemoryRepository
import com.example.travelmemories.data.TravelMemory
import com.example.travelmemories.data.TravelMemoryRepository

class MainActivity : Activity() {
    private lateinit var repository: TravelMemoryRepository
    private lateinit var listContainer: LinearLayout
    private lateinit var statsText: TextView
    private lateinit var searchInput: EditText
    private lateinit var sortSpinner: Spinner
    private var selectedType: MemoryType? = null
    private var selectedSort = SortOption.NewestFirst

    private val primaryColor = Color.rgb(165, 214, 167)
    private val cardColor = Color.rgb(241, 248, 233)
    private val accentColor = Color.rgb(128, 203, 196)
    private val textPrimary = Color.rgb(33, 33, 33)
    private val textSecondary = Color.rgb(117, 117, 117)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = SqliteTravelMemoryRepository(this)
        configureEdgeToEdge()
        setContentView(createContent())
    }

    override fun onResume() {
        super.onResume()
        renderMemories()
    }

    private fun configureEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun createContent(): View {
        val frame = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        val toolbar = Toolbar(this).apply {
            title = getString(R.string.app_name)
            setTitleTextColor(textPrimary)
            setBackgroundColor(primaryColor)
            minimumHeight = dp(56)
            elevation = dp(4).toFloat()
            setPadding(dp(16), 0, dp(16), 0)
        }
        root.addView(toolbar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(56),
        ))

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
        }
        root.addView(scrollView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f,
        ))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(84))
        }
        scrollView.addView(content, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ))

        statsText = TextView(this).apply {
            textSize = 15f
            setTextColor(textSecondary)
        }
        content.addView(statsText)

        content.addView(createSearchInput())
        content.addView(createSortRow())
        content.addView(createFilterRow())

        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(listContainer, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ))

        frame.addView(root)
        val fab = createFab()
        frame.addView(fab)
        applyInsets(root, toolbar, fab)
        return frame
    }

    private fun applyInsets(root: LinearLayout, toolbar: Toolbar, fab: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root.setOnApplyWindowInsetsListener { _, insets ->
                val systemBars = insets.getInsets(WindowInsets.Type.systemBars())
                toolbar.setPadding(dp(16), systemBars.top, dp(16), 0)
                toolbar.layoutParams = toolbar.layoutParams.apply {
                    height = dp(56) + systemBars.top
                }
                root.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
                updateFabMargins(fab, systemBars.right, systemBars.bottom)
                insets
            }
        } else {
            @Suppress("DEPRECATION")
            root.setOnApplyWindowInsetsListener { _, insets ->
                toolbar.setPadding(dp(16), insets.systemWindowInsetTop, dp(16), 0)
                toolbar.layoutParams = toolbar.layoutParams.apply {
                    height = dp(56) + insets.systemWindowInsetTop
                }
                root.setPadding(
                    insets.systemWindowInsetLeft,
                    0,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom,
                )
                updateFabMargins(fab, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                insets
            }
        }
    }

    private fun updateFabMargins(fab: View, rightInset: Int, bottomInset: Int) {
        val params = fab.layoutParams as FrameLayout.LayoutParams
        params.setMargins(0, 0, dp(20) + rightInset, dp(20) + bottomInset)
        fab.layoutParams = params
    }

    private fun createSearchInput(): EditText {
        searchInput = EditText(this).apply {
            hint = "Search by title, country or note"
            textSize = 15f
            setSingleLine(true)
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
            backgroundTintList = android.content.res.ColorStateList.valueOf(accentColor)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(s: Editable?) {
                    if (::listContainer.isInitialized) {
                        renderMemories()
                    }
                }
            })
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(0, dp(8), 0, 0)
            }
        }
        return searchInput
    }

    private fun createSortRow(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)

            addView(TextView(this@MainActivity).apply {
                text = "Sort"
                textSize = 14f
                setTextColor(textSecondary)
            }, LinearLayout.LayoutParams(dp(54), ViewGroup.LayoutParams.WRAP_CONTENT))

            sortSpinner = Spinner(this@MainActivity).apply {
                adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_spinner_item,
                    SortOption.entries.map { it.label },
                ).also {
                    it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedSort = SortOption.entries[position]
                        if (::listContainer.isInitialized) {
                            renderMemories()
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                }
            }
            addView(sortSpinner, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
    }

    private fun createFilterRow(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(12))

            addView(filterButton("All", null))
            addView(filterButton("City", MemoryType.City))
            addView(filterButton("Attraction", MemoryType.Attraction))
        }
    }

    private fun filterButton(label: String, type: MemoryType?): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            setTextColor(textPrimary)
            setOnClickListener {
                selectedType = type
                renderMemories()
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                setMargins(dp(4), 0, dp(4), 0)
            }
        }
    }

    private fun createFab(): TextView {
        return TextView(this).apply {
            text = "+"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(textPrimary)
            typeface = Typeface.DEFAULT_BOLD
            background = circleBackground(accentColor)
            elevation = dp(8).toFloat()
            contentDescription = "Add memory"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, EditMemoryActivity::class.java))
            }
            layoutParams = FrameLayout.LayoutParams(dp(64), dp(64), Gravity.BOTTOM or Gravity.END).apply {
                setMargins(0, 0, dp(20), dp(20))
            }
        }
    }

    private fun renderMemories() {
        val allMemories = repository.getAll()
        val typedMemories = selectedType?.let { type ->
            allMemories.filter { it.type == type }
        } ?: allMemories
        val searchQuery = searchInput.text.toString().trim()
        val searchedMemories = if (searchQuery.isBlank()) {
            typedMemories
        } else {
            typedMemories.filter { memory ->
                memory.title.contains(searchQuery, ignoreCase = true) ||
                    memory.country.contains(searchQuery, ignoreCase = true) ||
                    memory.note.contains(searchQuery, ignoreCase = true)
            }
        }
        val memories = sortMemories(searchedMemories)

        val averageRating = allMemories.takeIf { it.isNotEmpty() }
            ?.map { it.rating }
            ?.average()
            ?.let { String.format("%.1f", it) }
            ?: "-"
        val countriesVisited = allMemories.map { it.country.trim().lowercase() }.filter { it.isNotBlank() }.distinct().size
        statsText.text = "Total: ${allMemories.size}    Countries: $countriesVisited    Rating: $averageRating"

        listContainer.removeAllViews()
        if (memories.isEmpty()) {
            listContainer.addView(emptyState())
            return
        }

        memories.forEach { memory ->
            listContainer.addView(memoryCard(memory))
        }
    }

    private fun sortMemories(memories: List<TravelMemory>): List<TravelMemory> {
        return when (selectedSort) {
            SortOption.NewestFirst -> memories.sortedWith(
                compareByDescending<TravelMemory> { it.visitDate }.thenBy { it.title.lowercase() },
            )
            SortOption.OldestFirst -> memories.sortedWith(
                compareBy<TravelMemory> { it.visitDate }.thenBy { it.title.lowercase() },
            )
            SortOption.HighestRating -> memories.sortedWith(
                compareByDescending<TravelMemory> { it.rating }
                    .thenByDescending { it.visitDate }
                    .thenBy { it.title.lowercase() },
            )
        }
    }

    private fun emptyState(): TextView {
        return TextView(this).apply {
            text = if (searchInput.text.isBlank() && selectedType == null) {
                "No memories yet. Tap + to add your first trip."
            } else {
                "No memories match your filters."
            }
            textSize = 16f
            setTextColor(textSecondary)
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(48), dp(24), dp(48))
        }
    }

    private fun memoryCard(memory: TravelMemory): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBackground(cardColor, 8, this@MainActivity)
            elevation = dp(2).toFloat()
            setPadding(dp(14), dp(12), dp(10), dp(12))
            setOnClickListener {
                startActivity(
                    Intent(this@MainActivity, EditMemoryActivity::class.java)
                        .putExtra(EditMemoryActivity.EXTRA_MEMORY_ID, memory.id),
                )
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }

        card.addView(memoryVisual(memory), LinearLayout.LayoutParams(dp(54), dp(54)).apply {
            setMargins(0, 0, dp(12), 0)
        })

        val textBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        textBlock.addView(TextView(this).apply {
            text = memory.title
            textSize = 18f
            setTextColor(textPrimary)
            typeface = Typeface.DEFAULT_BOLD
        })
        textBlock.addView(TextView(this).apply {
            text = "${memory.type.label} in ${memory.country}"
            textSize = 14f
            setTextColor(textSecondary)
        })
        textBlock.addView(TextView(this).apply {
            text = "${memory.visitDate}    Rating: ${memory.rating}/5"
            textSize = 14f
            setTextColor(textSecondary)
        })
        card.addView(textBlock, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val deleteButton = Button(this).apply {
            text = "Delete"
            textSize = 12f
            isAllCaps = false
            setTextColor(textPrimary)
            setOnClickListener {
                confirmDelete(memory)
            }
        }
        card.addView(deleteButton, LinearLayout.LayoutParams(dp(92), dp(44)))

        return card
    }

    private fun memoryVisual(memory: TravelMemory): View {
        val photoUri = memory.photoUri
        if (!photoUri.isNullOrBlank()) {
            return ImageView(this).apply {
                background = roundedBackground(Color.rgb(245, 245, 245), 8, this@MainActivity)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(Uri.parse(photoUri))
                contentDescription = "${memory.title} photo"
            }
        }

        val markerColor = if (memory.type == MemoryType.City) primaryColor else accentColor
        return TextView(this).apply {
            text = if (memory.type == MemoryType.City) "C" else "A"
            gravity = Gravity.CENTER
            setTextColor(textPrimary)
            typeface = Typeface.DEFAULT_BOLD
            background = circleBackground(markerColor)
        }
    }

    private fun confirmDelete(memory: TravelMemory) {
        AlertDialog.Builder(this)
            .setTitle("Delete memory")
            .setMessage("Delete \"${memory.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                repository.delete(memory.id)
                renderMemories()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private enum class SortOption(val label: String) {
        NewestFirst("Newest first"),
        OldestFirst("Oldest first"),
        HighestRating("Highest rating"),
    }
}
