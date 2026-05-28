// 경로: com/example/habittracker/data/AppTypeConverters.kt
package com.example.habittracker.data

import androidx.room.TypeConverter
import com.example.habittracker.data.model.BodyPartType
import com.example.habittracker.data.model.MealType

class AppTypeConverters {

    @TypeConverter
    fun fromMealType(value: MealType): String = value.name

    @TypeConverter
    fun toMealType(value: String): MealType = MealType.valueOf(value)

    @TypeConverter
    fun fromBodyPartType(value: BodyPartType): String = value.name

    @TypeConverter
    fun toBodyPartType(value: String): BodyPartType = BodyPartType.valueOf(value)
}
