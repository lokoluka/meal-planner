package com.example.mealplanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [Recipe::class, Ingredient::class, IngredientPackage::class, RecipeIngredientCrossRef::class, MealPlan::class, WeeklyPlan::class, Family::class, FamilyMember::class, WeeklyPlanFamilyCrossRef::class],
    version = 13,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun familyDao(): FamilyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meal_planner_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromMeasurementUnit(value: MeasurementUnit): String = value.name

    @TypeConverter
    fun toMeasurementUnit(value: String): MeasurementUnit = MeasurementUnit.valueOf(value)

    @TypeConverter
    fun fromDayOfWeek(value: DayOfWeek): String = value.name

    @TypeConverter
    fun toDayOfWeek(value: String): DayOfWeek = DayOfWeek.valueOf(value)

    @TypeConverter
    fun fromMealType(value: MealType): String = value.name

    @TypeConverter
    fun toMealType(value: String): MealType = MealType.valueOf(value)
}
