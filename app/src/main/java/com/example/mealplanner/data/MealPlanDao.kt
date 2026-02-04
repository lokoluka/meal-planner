package com.example.mealplanner.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface MealPlanDao {
    // Weekly Plan operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyPlan(weeklyPlan: WeeklyPlan): Long

    @Update
    suspend fun updateWeeklyPlan(weeklyPlan: WeeklyPlan)

    @Delete
    suspend fun deleteWeeklyPlan(weeklyPlan: WeeklyPlan)

    @Query("SELECT * FROM weekly_plans ORDER BY createdDate DESC")
    suspend fun getAllWeeklyPlans(): List<WeeklyPlan>
    
    @Query("""
        SELECT DISTINCT wp.* FROM weekly_plans wp
        WHERE wp.userId = :userId
        OR wp.weeklyPlanId IN (
            SELECT wpf.weeklyPlanId FROM weekly_plan_family_cross_ref wpf
            INNER JOIN family_members fm ON wpf.familyId = fm.familyId
            WHERE fm.userId = :userId
        )
        ORDER BY wp.createdDate DESC
    """)
    suspend fun getWeeklyPlansForUser(userId: String): List<WeeklyPlan>
    
    @Query("SELECT * FROM weekly_plans WHERE weeklyPlanId = :weeklyPlanId")
    suspend fun getWeeklyPlanById(weeklyPlanId: Long): WeeklyPlan?

    @Transaction
    @Query("SELECT * FROM weekly_plans WHERE weeklyPlanId = :weeklyPlanId")
    suspend fun getWeeklyPlanWithMeals(weeklyPlanId: Long): WeeklyPlanWithMeals?

    @Transaction
    @Query("SELECT * FROM weekly_plans ORDER BY createdDate DESC")
    suspend fun getAllWeeklyPlansWithMeals(): List<WeeklyPlanWithMeals>

    @Query("""
        SELECT * FROM weekly_plans
        WHERE weeklyPlanId IN (
            SELECT weeklyPlanId FROM weekly_plan_family_cross_ref WHERE familyId = :familyId
        )
        ORDER BY createdDate DESC
    """)
    suspend fun getWeeklyPlansForFamily(familyId: Long): List<WeeklyPlan>

    // Weekly Plan Family CrossRef operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyPlanFamilyCrossRef(crossRef: WeeklyPlanFamilyCrossRef)

    @Delete
    suspend fun deleteWeeklyPlanFamilyCrossRef(crossRef: WeeklyPlanFamilyCrossRef)

    @Query("DELETE FROM weekly_plan_family_cross_ref WHERE weeklyPlanId = :weeklyPlanId")
    suspend fun deleteAllFamiliesForWeeklyPlan(weeklyPlanId: Long)

    @Transaction
    @Query("SELECT * FROM weekly_plans WHERE weeklyPlanId = :weeklyPlanId")
    suspend fun getWeeklyPlanWithFamilies(weeklyPlanId: Long): WeeklyPlanWithFamilies?

    @Transaction
    @Query("SELECT * FROM weekly_plans ORDER BY createdDate DESC")
    suspend fun getAllWeeklyPlansWithFamilies(): List<WeeklyPlanWithFamilies>

    @Query("SELECT familyId FROM weekly_plan_family_cross_ref WHERE weeklyPlanId = :weeklyPlanId")
    suspend fun getFamilyIdsForWeeklyPlan(weeklyPlanId: Long): List<Long>

    // Meal Plan operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(mealPlan: MealPlan): Long

    @Update
    suspend fun updateMealPlan(mealPlan: MealPlan)

    @Delete
    suspend fun deleteMealPlan(mealPlan: MealPlan)

    @Query("DELETE FROM meal_plans WHERE weeklyPlanId = :weeklyPlanId AND dayOfWeek = :dayOfWeek AND mealType = :mealType")
    suspend fun deleteMealPlanByDayAndType(weeklyPlanId: Long, dayOfWeek: DayOfWeek, mealType: MealType)

    @Transaction
    @Query("SELECT * FROM meal_plans WHERE weeklyPlanId = :weeklyPlanId")
    suspend fun getMealPlansByWeeklyPlan(weeklyPlanId: Long): List<MealPlanWithRecipe>

    @Transaction
    @Query("SELECT * FROM meal_plans WHERE weeklyPlanId = :weeklyPlanId AND dayOfWeek = :dayOfWeek")
    suspend fun getMealPlansByDay(weeklyPlanId: Long, dayOfWeek: DayOfWeek): List<MealPlanWithRecipe>

    @Transaction
    @Query("SELECT * FROM meal_plans WHERE weeklyPlanId = :weeklyPlanId AND dayOfWeek = :dayOfWeek AND mealType = :mealType LIMIT 1")
    suspend fun getMealPlan(weeklyPlanId: Long, dayOfWeek: DayOfWeek, mealType: MealType): MealPlanWithRecipe?

    @Query("DELETE FROM meal_plans WHERE weeklyPlanId = :weeklyPlanId")
    suspend fun clearWeeklyPlanMeals(weeklyPlanId: Long)
}
