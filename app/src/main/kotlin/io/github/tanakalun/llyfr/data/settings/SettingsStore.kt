package io.github.tanakalun.llyfr.data.settings

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 应用级设置存根：Compose snapshot state + SharedPreferences 双写。
 * 展示型全局配置（主题/模糊/导航栏）供 UI 直读；写入统一经 ViewModel 中转。
 */
object SettingsStore {

    private const val PREFS_NAME = "llyfr_settings"
    private const val KEY_COLOR_MODE = "colorMode"
    private const val KEY_KEY_COLOR_INDEX = "keyColorIndex"
    private const val KEY_PALETTE_STYLE = "paletteStyle"
    private const val KEY_COLOR_SPEC = "colorSpec"
    private const val KEY_ENABLE_BLUR = "enableBlur"
    private const val KEY_BLUR_STYLE = "blurStyle"
    private const val KEY_USE_FLOATING_NAVBAR = "useFloatingNavbar"
    private const val KEY_FLOATING_NAVBAR_STYLE = "floatingNavbarStyle"
    private const val KEY_FLOATING_NAVBAR_POSITION = "floatingNavbarPosition"
    private const val KEY_SHOW_SEARCH_BAR = "showSearchBar"
    private const val KEY_ENTER_CREATES_ITEM = "enterCreatesItem"
    private const val KEY_CODE_BLOCK_WRAP = "codeBlockWrap"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_ENCRYPT_NOTES = "encryptNotes"
    private const val KEY_SAVE_ON_BACK = "saveOnBack"
    private const val KEY_SWIPE_BACK = "swipeBackEnabled"

    const val LANG_SYSTEM = 0
    const val LANG_EN = 1
    const val LANG_ZH = 2
    const val LANG_ZH_TW = 3
    const val LANG_JA = 4

    private lateinit var prefs: SharedPreferences
    private var initialized = false

    var colorMode by mutableIntStateOf(0)
        private set
    var keyColorIndex by mutableIntStateOf(0)
        private set
    var paletteStyle by mutableIntStateOf(0)
        private set
    var colorSpec by mutableIntStateOf(0)
        private set
    var enableBlur by mutableStateOf(true)
        private set
    var blurStyle by mutableIntStateOf(0)
        private set
    var useFloatingNavbar by mutableStateOf(false)
        private set
    var floatingNavbarStyle by mutableIntStateOf(0)
        private set
    var floatingNavbarPosition by mutableIntStateOf(0)
        private set
    var showSearchBar by mutableStateOf(true)
        private set
    var enterCreatesItem by mutableStateOf(false)
        private set
    var codeBlockWrap by mutableStateOf(true)
        private set
    var encryptNotes by mutableStateOf(true)
        private set
    var saveOnBack by mutableStateOf(true)
        private set
    var swipeBackEnabled by mutableStateOf(false)
        private set
    var language by mutableIntStateOf(LANG_SYSTEM)
        private set

    /** 幂等初始化，Application/Activity 均可安全调用。 */
    fun init(context: Context) {
        if (initialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        colorMode = prefs.getInt(KEY_COLOR_MODE, 0)
        keyColorIndex = prefs.getInt(KEY_KEY_COLOR_INDEX, 0)
        paletteStyle = prefs.getInt(KEY_PALETTE_STYLE, 0)
        colorSpec = prefs.getInt(KEY_COLOR_SPEC, 0)
        enableBlur = prefs.getBoolean(KEY_ENABLE_BLUR, true)
        blurStyle = prefs.getInt(KEY_BLUR_STYLE, 0)
        useFloatingNavbar = prefs.getBoolean(KEY_USE_FLOATING_NAVBAR, false)
        floatingNavbarStyle = prefs.getInt(KEY_FLOATING_NAVBAR_STYLE, 0)
        floatingNavbarPosition = prefs.getInt(KEY_FLOATING_NAVBAR_POSITION, 0)
        showSearchBar = prefs.getBoolean(KEY_SHOW_SEARCH_BAR, true)
        enterCreatesItem = prefs.getBoolean(KEY_ENTER_CREATES_ITEM, false)
        codeBlockWrap = prefs.getBoolean(KEY_CODE_BLOCK_WRAP, true)
        encryptNotes = prefs.getBoolean(KEY_ENCRYPT_NOTES, true)
        saveOnBack = prefs.getBoolean(KEY_SAVE_ON_BACK, true)
        swipeBackEnabled = prefs.getBoolean(KEY_SWIPE_BACK, false)
        language = prefs.getInt(KEY_LANGUAGE, LANG_SYSTEM)
        initialized = true
    }

    fun updateColorMode(value: Int) {
        colorMode = value
        prefs.edit().putInt(KEY_COLOR_MODE, value).apply()
    }

    fun updateKeyColorIndex(value: Int) {
        keyColorIndex = value
        prefs.edit().putInt(KEY_KEY_COLOR_INDEX, value).apply()
    }

    fun updatePaletteStyle(value: Int) {
        paletteStyle = value
        prefs.edit().putInt(KEY_PALETTE_STYLE, value).apply()
    }

    fun updateColorSpec(value: Int) {
        colorSpec = value
        prefs.edit().putInt(KEY_COLOR_SPEC, value).apply()
    }

    fun updateEnableBlur(value: Boolean) {
        enableBlur = value
        prefs.edit().putBoolean(KEY_ENABLE_BLUR, value).apply()
    }

    fun updateBlurStyle(value: Int) {
        blurStyle = value
        prefs.edit().putInt(KEY_BLUR_STYLE, value).apply()
    }

    fun updateUseFloatingNavbar(value: Boolean) {
        useFloatingNavbar = value
        prefs.edit().putBoolean(KEY_USE_FLOATING_NAVBAR, value).apply()
    }

    fun updateFloatingNavbarStyle(value: Int) {
        floatingNavbarStyle = value
        prefs.edit().putInt(KEY_FLOATING_NAVBAR_STYLE, value).apply()
    }

    fun updateFloatingNavbarPosition(value: Int) {
        floatingNavbarPosition = value
        prefs.edit().putInt(KEY_FLOATING_NAVBAR_POSITION, value).apply()
    }

    fun updateShowSearchBar(value: Boolean) {
        showSearchBar = value
        prefs.edit().putBoolean(KEY_SHOW_SEARCH_BAR, value).apply()
    }

    fun updateEnterCreatesItem(value: Boolean) {
        enterCreatesItem = value
        prefs.edit().putBoolean(KEY_ENTER_CREATES_ITEM, value).apply()
    }

    fun updateCodeBlockWrap(value: Boolean) {
        codeBlockWrap = value
        prefs.edit().putBoolean(KEY_CODE_BLOCK_WRAP, value).apply()
    }

    fun updateEncryptNotes(value: Boolean) {
        encryptNotes = value
        prefs.edit().putBoolean(KEY_ENCRYPT_NOTES, value).apply()
    }

    fun updateSaveOnBack(value: Boolean) {
        saveOnBack = value
        prefs.edit().putBoolean(KEY_SAVE_ON_BACK, value).apply()
    }

    fun updateSwipeBackEnabled(value: Boolean) {
        swipeBackEnabled = value
        prefs.edit().putBoolean(KEY_SWIPE_BACK, value).apply()
    }

    fun updateLanguage(context: Context, value: Int) {
        language = value
        prefs.edit().putInt(KEY_LANGUAGE, value).apply()
        applyLocale(context)
    }

    private fun applyLocale(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val localeTag = when (language) {
                LANG_EN -> "en"
                LANG_ZH -> "zh-CN"
                LANG_ZH_TW -> "zh-TW"
                LANG_JA -> "ja"
                else -> ""
            }
            localeManager?.applicationLocales = if (localeTag.isEmpty()) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(localeTag)
            }
        }
    }
}