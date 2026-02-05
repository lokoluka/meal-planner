package com.lokosoft.mealplanner.data

import org.junit.Assert.*
import org.junit.Test
import java.util.*

class ModelTest {

    @Test
    fun `WeeklyPlan creation with default values`() {
        val plan = WeeklyPlan(
            name = "Test Week",
            startDate = System.currentTimeMillis()
        )
        
        assertEquals("Test Week", plan.name)
        assertEquals(2, plan.commensals)
        assertEquals(0L, plan.weeklyPlanId)
    }

    @Test
    fun `WeeklyPlan creation with custom commensals`() {
        val plan = WeeklyPlan(
            name = "Family Week",
            startDate = System.currentTimeMillis(),
            commensals = 5
        )
        
        assertEquals(5, plan.commensals)
    }

    @Test
    fun `WeeklyPlanFamilyCrossRef creation`() {
        val crossRef = WeeklyPlanFamilyCrossRef(
            weeklyPlanId = 1L,
            familyId = 10L
        )
        
        assertEquals(1L, crossRef.weeklyPlanId)
        assertEquals(10L, crossRef.familyId)
    }

    @Test
    fun `Family creation`() {
        val family = Family(
            name = "Smith Family",
            ownerId = "user123"
        )
        
        assertEquals("Smith Family", family.name)
        assertEquals("user123", family.ownerId)
        assertEquals(0L, family.familyId)
    }

    @Test
    fun `FamilyMember creation`() {
        val member = FamilyMember(
            familyId = 1L,
            userId = "user456",
            email = "user@example.com",
            displayName = "User Name"
        )
        
        assertEquals(1L, member.familyId)
        assertEquals("user456", member.userId)
        assertEquals("user@example.com", member.email)
        assertEquals("User Name", member.displayName)
    }

    @Test
    fun `DayOfWeek enum values`() {
        val allDays = DayOfWeek.entries
        assertEquals(7, allDays.size)
        assertTrue(allDays.contains(DayOfWeek.MONDAY))
        assertTrue(allDays.contains(DayOfWeek.SUNDAY))
    }

    @Test
    fun `MealType enum values`() {
        val allMealTypes = MealType.entries
        assertEquals(2, allMealTypes.size)
        assertTrue(allMealTypes.contains(MealType.LUNCH))
        assertTrue(allMealTypes.contains(MealType.DINNER))
    }

    @Test
    fun `MeasurementUnit enum values`() {
        val units = MeasurementUnit.entries
        assertTrue(units.contains(MeasurementUnit.CUP))
        assertTrue(units.contains(MeasurementUnit.TABLESPOON))
        assertTrue(units.contains(MeasurementUnit.GRAM))
    }

    @Test
    fun `Recipe creation with servings`() {
        val recipe = Recipe(
            name = "Pasta",
            instructions = "Cook pasta",
            servings = 4
        )
        
        assertEquals("Pasta", recipe.name)
        assertEquals("Cook pasta", recipe.instructions)
        assertEquals(4, recipe.servings)
    }

    @Test
    fun `Ingredient creation with default unit`() {
        val ingredient = Ingredient(
            name = "Flour",
            defaultUnit = MeasurementUnit.CUP
        )
        
        assertEquals("Flour", ingredient.name)
        assertEquals(MeasurementUnit.CUP, ingredient.defaultUnit)
    }

    @Test
    fun `MealPlan creation with commensals`() {
        val mealPlan = MealPlan(
            weeklyPlanId = 1L,
            recipeId = 5L,
            dayOfWeek = DayOfWeek.MONDAY,
            mealType = MealType.DINNER,
            servings = 2,
            commensals = 4
        )
        
        assertEquals(1L, mealPlan.weeklyPlanId)
        assertEquals(5L, mealPlan.recipeId)
        assertEquals(DayOfWeek.MONDAY, mealPlan.dayOfWeek)
        assertEquals(MealType.DINNER, mealPlan.mealType)
        assertEquals(2, mealPlan.servings)
        assertEquals(4, mealPlan.commensals)
    }

    @Test
    fun `RecipeIngredientCrossRef creation`() {
        val crossRef = RecipeIngredientCrossRef(
            recipeId = 1L,
            ingredientId = 2L,
            amount = 2.5,
            unit = MeasurementUnit.CUP
        )
        
        assertEquals(1L, crossRef.recipeId)
        assertEquals(2L, crossRef.ingredientId)
        assertEquals(2.5, crossRef.amount, 0.001)
        assertEquals(MeasurementUnit.CUP, crossRef.unit)
    }
}
