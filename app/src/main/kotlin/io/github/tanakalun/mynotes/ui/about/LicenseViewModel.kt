package io.github.tanakalun.mynotes.ui.about

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.tanakalun.mynotes.R
import io.github.tanakalun.mynotes.util.Library
import io.github.tanakalun.mynotes.util.SimpleJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LicenseUiState(
    val libraries: List<Library>? = null,
    val loading: Boolean = true,
)

class LicenseViewModel(
    private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LicenseUiState())
    val uiState: StateFlow<LicenseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val libs = withContext(Dispatchers.IO) {
                runCatching {
                    val jsonString = appContext.resources.openRawResource(R.raw.aboutlibraries)
                        .bufferedReader().use { it.readText() }
                    SimpleJsonParser(jsonString).parseLibs().libraries
                }.getOrNull()
            }
            _uiState.value = LicenseUiState(libraries = libs, loading = false)
        }
    }
}