package com.qiaoyuang.movie.model.domain

/**
 * Which theme the user picked. Lives here rather than beside the colours it selects, because the
 * data layer has to persist it and must not depend on the UI layer to name it.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    FOLLOW_SYSTEM,
}
