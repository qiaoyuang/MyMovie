package com.qiaoyuang.movie.basicui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class MovieColors(
    val containerColor: Color,
    val scrolledContainerColor: Color,
    val commonBlueTextColor: Color,
    val commonBlueIconColor: Color,
    val sectionTitleColor: Color,
    val contentTextColor: Color,
    val mainTitleColor: Color,
    val lightContentColor: Color,
    val hintTextColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val onSurfaceColor: Color,
    val backdropPlaceholderColor: Color,
    val ratingBackgroundColor: Color,
    val popWindowBackground: Color,
)

val LightColors = MovieColors(
    containerColor = Color(0xFF87CEFA),
    scrolledContainerColor = Color(0xFF6495ED),
    commonBlueTextColor = Color(0xFFC766FF),
    commonBlueIconColor = Color(0xFF9C2CF3),
    sectionTitleColor = Color(0xFF2B2B2B),
    contentTextColor = Color(0xFF2E3333),
    mainTitleColor = Color(0xFF161616),
    lightContentColor = Color.White,
    hintTextColor = Color(0xFF515153),
    backgroundColor = Color(0xFFFAFAFA),
    surfaceColor = Color.White,
    onSurfaceColor = Color(0xFF161616),
    backdropPlaceholderColor = Color(0xFFFF4081),
    ratingBackgroundColor = Color(0xFF161616),
    popWindowBackground = Color(0xFFE4E4E4),
)

val DarkColors = MovieColors(
    containerColor = Color(0xFF2D2D30),
    scrolledContainerColor = Color(0xFF1E1E1E),
    commonBlueTextColor = Color(0xFFE0AAFF),
    commonBlueIconColor = Color(0xFFC77DFF),
    sectionTitleColor = Color(0xFFE4E4E4),
    contentTextColor = Color(0xFFBEBEBE),
    mainTitleColor = Color.White,
    lightContentColor = Color(0xFFE4E4E4),
    hintTextColor = Color(0xFF9E9E9E),
    backgroundColor = Color(0xFF121212),
    surfaceColor = Color(0xFF1E1E1E),
    onSurfaceColor = Color(0xFFE4E4E4),
    backdropPlaceholderColor = Color(0xFF7B1FA2),
    ratingBackgroundColor = Color(0xFF4A4A4A),
    popWindowBackground = Color(0xFF2D2D30),
)

val LocalMovieColors = staticCompositionLocalOf { LightColors }

enum class ThemeMode {
    LIGHT,
    DARK,
    FOLLOW_SYSTEM
}

object MovieTheme {
    private var _themeMode by mutableStateOf(ThemeMode.FOLLOW_SYSTEM)
        
    val themeMode: ThemeMode
        get() = _themeMode
        
    fun setThemeMode(mode: ThemeMode) {
        _themeMode = mode
    }

    @Composable
    @ReadOnlyComposable
    fun shouldUseDarkTheme(): Boolean {
        return when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        }
    }
}

@Composable
fun MovieTheme(
    content: @Composable () -> Unit
) {
    val shouldUseDark = MovieTheme.shouldUseDarkTheme()
    val colors = if (shouldUseDark) DarkColors else LightColors
    
    CompositionLocalProvider(
        value = LocalMovieColors provides colors,
        content = content
    )
}

@get:Composable
@get:ReadOnlyComposable
val colors: MovieColors
    get() = LocalMovieColors.current

@get:Composable
@get:ReadOnlyComposable
val containerColor: Color
    get() = colors.containerColor

@get:Composable
@get:ReadOnlyComposable
val scrolledContainerColor: Color
    get() = colors.scrolledContainerColor

@get:Composable
@get:ReadOnlyComposable
val commonBlueTextColor: Color
    get() = colors.commonBlueTextColor

@get:Composable
@get:ReadOnlyComposable
val commonBlueIconColor: Color
    get() = colors.commonBlueIconColor

@get:Composable
@get:ReadOnlyComposable
val sectionTitleColor: Color
    get() = colors.sectionTitleColor

@get:Composable
@get:ReadOnlyComposable
val contentTextColor: Color
    get() = colors.contentTextColor

@get:Composable
@get:ReadOnlyComposable
val mainTitleColor: Color
    get() = colors.mainTitleColor

@get:Composable
@get:ReadOnlyComposable
val lightContentColor: Color
    get() = colors.lightContentColor

@get:Composable
@get:ReadOnlyComposable
val hintTextColor: Color
    get() = colors.hintTextColor

@get:Composable
@get:ReadOnlyComposable
val backgroundColor: Color
    get() = colors.backgroundColor

@get:Composable
@get:ReadOnlyComposable
val surfaceColor: Color
    get() = colors.surfaceColor

@get:Composable
@get:ReadOnlyComposable
val onSurfaceColor: Color
    get() = colors.onSurfaceColor

@get:Composable
@get:ReadOnlyComposable
val backdropPlaceholderColor: Color
    get() = colors.backdropPlaceholderColor

@get:Composable
@get:ReadOnlyComposable
val ratingBackgroundColor: Color
    get() = colors.ratingBackgroundColor

@get:Composable
@get:ReadOnlyComposable
val popWindowBackground: Color
    get() = colors.popWindowBackground