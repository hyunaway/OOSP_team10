// 경로: com/example/habittracker/ui/avatar/AvatarGender.kt
package com.example.habittracker.ui.avatar

enum class AvatarGender {
    MALE, FEMALE;

    val label: String get() = when (this) {
        MALE   -> "남자"
        FEMALE -> "여자"
    }

    companion object {
        fun fromString(value: String): AvatarGender =
            entries.firstOrNull { it.name == value } ?: MALE
    }
}
