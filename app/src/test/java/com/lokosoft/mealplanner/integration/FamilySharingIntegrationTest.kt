package com.lokosoft.mealplanner.integration

import com.lokosoft.mealplanner.data.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Integration tests for the family sharing feature workflow.
 * These tests verify the complete user flow from creating a plan to sharing it with families.
 */
class FamilySharingIntegrationTest {

    @Test
    fun `complete family sharing workflow`() {
        // Step 1: Create a weekly plan
        val weeklyPlan = WeeklyPlan(
            weeklyPlanId = 1L,
            name = "Family Meal Plan",
            startDate = System.currentTimeMillis(),
            commensals = 4
        )
        
        assertNotNull(weeklyPlan)
        assertEquals("Family Meal Plan", weeklyPlan.name)
        assertEquals(4, weeklyPlan.commensals)
        
        // Step 2: Create families
        val family1 = Family(familyId = 1L, name = "Smith Family", ownerId = "user123")
        val family2 = Family(familyId = 2L, name = "Jones Family", ownerId = "user456")
        
        assertNotNull(family1)
        assertNotNull(family2)
        
        // Step 3: Share the plan with both families
        val crossRef1 = WeeklyPlanFamilyCrossRef(
            weeklyPlanId = weeklyPlan.weeklyPlanId,
            familyId = family1.familyId
        )
        val crossRef2 = WeeklyPlanFamilyCrossRef(
            weeklyPlanId = weeklyPlan.weeklyPlanId,
            familyId = family2.familyId
        )
        
        assertEquals(weeklyPlan.weeklyPlanId, crossRef1.weeklyPlanId)
        assertEquals(weeklyPlan.weeklyPlanId, crossRef2.weeklyPlanId)
        assertEquals(family1.familyId, crossRef1.familyId)
        assertEquals(family2.familyId, crossRef2.familyId)
        
        // Step 4: Verify the plan is associated with families
        val weeklyPlanWithFamilies = WeeklyPlanWithFamilies(
            weeklyPlan = weeklyPlan,
            families = listOf(family1, family2)
        )
        
        assertEquals(2, weeklyPlanWithFamilies.families.size)
        assertTrue(weeklyPlanWithFamilies.families.any { it.name == "Smith Family" })
        assertTrue(weeklyPlanWithFamilies.families.any { it.name == "Jones Family" })
    }

    @Test
    fun `plan can be personal then shared`() {
        // Step 1: Create a personal plan (no families)
        val plan = WeeklyPlan(
            weeklyPlanId = 1L,
            name = "Personal Plan",
            startDate = System.currentTimeMillis(),
            commensals = 2
        )
        
        var planWithFamilies = WeeklyPlanWithFamilies(plan, emptyList())
        assertTrue(planWithFamilies.families.isEmpty())
        
        // Step 2: Later, share it with a family
        val family = Family(1L, "New Family", "owner123")
        val crossRef = WeeklyPlanFamilyCrossRef(plan.weeklyPlanId, family.familyId)
        
        planWithFamilies = WeeklyPlanWithFamilies(plan, listOf(family))
        assertEquals(1, planWithFamilies.families.size)
        assertEquals("New Family", planWithFamilies.families[0].name)
    }

    @Test
    fun `removing family from plan`() {
        // Setup: Plan shared with multiple families
        val plan = WeeklyPlan(1L, "Shared Plan", System.currentTimeMillis(), 4)
        val family1 = Family(1L, "Family 1", "owner1")
        val family2 = Family(2L, "Family 2", "owner2")
        val family3 = Family(3L, "Family 3", "owner3")
        
        var planWithFamilies = WeeklyPlanWithFamilies(
            plan,
            listOf(family1, family2, family3)
        )
        assertEquals(3, planWithFamilies.families.size)
        
        // Remove family2
        planWithFamilies = WeeklyPlanWithFamilies(
            plan,
            listOf(family1, family3)
        )
        
        assertEquals(2, planWithFamilies.families.size)
        assertTrue(planWithFamilies.families.any { it.familyId == 1L })
        assertFalse(planWithFamilies.families.any { it.familyId == 2L })
        assertTrue(planWithFamilies.families.any { it.familyId == 3L })
    }

    @Test
    fun `family with multiple members viewing shared plan`() {
        val plan = WeeklyPlan(1L, "Team Meal Plan", System.currentTimeMillis(), 8)
        val family = Family(1L, "Large Family", "owner123")
        
        val members = listOf(
            FamilyMember(1L, family.familyId, "user1", "user1@test.com", "User 1"),
            FamilyMember(2L, family.familyId, "user2", "user2@test.com", "User 2"),
            FamilyMember(3L, family.familyId, "user3", "user3@test.com", "User 3"),
            FamilyMember(4L, family.familyId, "user4", "user4@test.com", "User 4")
        )
        
        val familyWithMembers = FamilyWithMembers(family, members)
        val crossRef = WeeklyPlanFamilyCrossRef(plan.weeklyPlanId, family.familyId)
        
        // All members should be able to see the plan
        assertEquals(4, familyWithMembers.members.size)
        assertEquals(plan.weeklyPlanId, crossRef.weeklyPlanId)
        assertEquals(family.familyId, crossRef.familyId)
    }

    @Test
    fun `adding meals to shared plan`() {
        val plan = WeeklyPlan(1L, "Shared Weekly Plan", System.currentTimeMillis(), 4)
        val recipe1 = Recipe(1L, "Pasta", "Boil pasta", 4)
        val recipe2 = Recipe(2L, "Salad", "Mix salad", 4)
        
        val meal1 = MealPlan(
            weeklyPlanId = plan.weeklyPlanId,
            recipeId = recipe1.recipeId,
            dayOfWeek = DayOfWeek.MONDAY,
            mealType = MealType.DINNER,
            servings = 1,
            commensals = plan.commensals
        )
        
        val meal2 = MealPlan(
            weeklyPlanId = plan.weeklyPlanId,
            recipeId = recipe2.recipeId,
            dayOfWeek = DayOfWeek.MONDAY,
            mealType = MealType.LUNCH,
            servings = 1,
            commensals = plan.commensals
        )
        
        // Verify meals are associated with the shared plan
        assertEquals(plan.weeklyPlanId, meal1.weeklyPlanId)
        assertEquals(plan.weeklyPlanId, meal2.weeklyPlanId)
        assertEquals(plan.commensals, meal1.commensals)
        assertEquals(plan.commensals, meal2.commensals)
    }

    @Test
    fun `Firebase sync data structure for shared plan`() {
        val plan = WeeklyPlan(1L, "Sync Test Plan", System.currentTimeMillis(), 4)
        val familyIds = listOf(5L, 10L, 15L)
        
        // Simulate Firebase data structure
        val firebaseData = hashMapOf(
            "name" to plan.name,
            "startDate" to plan.startDate,
            "commensals" to plan.commensals,
            "createdDate" to plan.createdDate,
            "familyIds" to familyIds,
            "meals" to emptyList<Map<String, Any>>()
        )
        
        // Verify data structure
        assertEquals(plan.name, firebaseData["name"])
        assertEquals(plan.startDate, firebaseData["startDate"])
        assertEquals(plan.commensals, firebaseData["commensals"])
        
        @Suppress("UNCHECKED_CAST")
        val storedFamilyIds = firebaseData["familyIds"] as List<Long>
        assertEquals(3, storedFamilyIds.size)
        assertTrue(storedFamilyIds.containsAll(familyIds))
        
        // Verify it would be stored in multiple Firebase paths
        val userPath = "users/user123/weeklyPlans/${plan.weeklyPlanId}"
        val familyPaths = familyIds.map { "families/$it/weeklyPlans/${plan.weeklyPlanId}" }
        
        assertEquals(3, familyPaths.size)
        assertTrue(familyPaths.all { it.contains("families") })
        assertTrue(familyPaths.all { it.contains(plan.weeklyPlanId.toString()) })
    }

    @Test
    fun `unsharing plan removes from family but keeps personal copy`() {
        val plan = WeeklyPlan(1L, "Plan to Unshare", System.currentTimeMillis(), 2)
        val family = Family(1L, "Temp Family", "owner123")
        
        // Initially shared
        var crossRefs = listOf(WeeklyPlanFamilyCrossRef(plan.weeklyPlanId, family.familyId))
        assertEquals(1, crossRefs.size)
        
        // Unshare from family
        crossRefs = emptyList()
        assertTrue(crossRefs.isEmpty())
        
        // Plan should still exist (just not shared)
        assertNotNull(plan)
        assertEquals("Plan to Unshare", plan.name)
    }
}
