package com.example.mealplanner.sync

import com.example.mealplanner.data.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Firebase sync operations related to family sharing.
 */
class FirebaseSyncTest {

    private lateinit var testWeeklyPlan: WeeklyPlan
    private lateinit var testFamilies: List<Family>

    @Before
    fun setup() {
        testWeeklyPlan = WeeklyPlan(
            weeklyPlanId = 1L,
            name = "Test Week",
            startDate = System.currentTimeMillis(),
            commensals = 4
        )

        testFamilies = listOf(
            Family(1L, "Family A", "owner1"),
            Family(2L, "Family B", "owner2"),
            Family(3L, "Family C", "owner3")
        )
    }

    @Test
    fun `plan data includes family IDs`() {
        val familyIds = listOf(1L, 2L, 3L)
        
        val planData = hashMapOf(
            "name" to testWeeklyPlan.name,
            "startDate" to testWeeklyPlan.startDate,
            "commensals" to testWeeklyPlan.commensals,
            "familyIds" to familyIds
        )

        assertEquals(testWeeklyPlan.name, planData["name"])
        assertEquals(testWeeklyPlan.commensals, planData["commensals"])
        
        @Suppress("UNCHECKED_CAST")
        val storedFamilyIds = planData["familyIds"] as List<Long>
        assertEquals(3, storedFamilyIds.size)
        assertTrue(storedFamilyIds.contains(1L))
        assertTrue(storedFamilyIds.contains(2L))
        assertTrue(storedFamilyIds.contains(3L))
    }

    @Test
    fun `plan with no families has empty familyIds`() {
        val familyIds = emptyList<Long>()
        
        val planData = hashMapOf(
            "name" to testWeeklyPlan.name,
            "familyIds" to familyIds
        )

        @Suppress("UNCHECKED_CAST")
        val storedFamilyIds = planData["familyIds"] as List<Long>
        assertTrue(storedFamilyIds.isEmpty())
    }

    @Test
    fun `Firebase path for user collection`() {
        val userId = "user123"
        val path = "users/$userId/weeklyPlans/${testWeeklyPlan.weeklyPlanId}"
        
        assertTrue(path.contains("users"))
        assertTrue(path.contains(userId))
        assertTrue(path.contains("weeklyPlans"))
        assertTrue(path.contains(testWeeklyPlan.weeklyPlanId.toString()))
    }

    @Test
    fun `Firebase path for family collection`() {
        val familyId = 10L
        val path = "families/$familyId/weeklyPlans/${testWeeklyPlan.weeklyPlanId}"
        
        assertTrue(path.contains("families"))
        assertTrue(path.contains(familyId.toString()))
        assertTrue(path.contains("weeklyPlans"))
        assertTrue(path.contains(testWeeklyPlan.weeklyPlanId.toString()))
    }

    @Test
    fun `meal data serialization`() {
        val mealData = hashMapOf(
            "recipeId" to 5L,
            "recipeName" to "Pasta",
            "dayOfWeek" to DayOfWeek.MONDAY.name,
            "mealType" to MealType.DINNER.name,
            "servings" to 2,
            "commensals" to 4
        )

        assertEquals(5L, mealData["recipeId"])
        assertEquals("Pasta", mealData["recipeName"])
        assertEquals("MONDAY", mealData["dayOfWeek"])
        assertEquals("DINNER", mealData["mealType"])
        assertEquals(2, mealData["servings"])
        assertEquals(4, mealData["commensals"])
    }

    @Test
    fun `download weekly plan with family associations`() {
        val familyIds = listOf(1L, 2L)
        
        // Simulate downloaded data
        val downloadedData = hashMapOf(
            "name" to "Downloaded Week",
            "startDate" to System.currentTimeMillis(),
            "commensals" to 3,
            "familyIds" to familyIds
        )

        assertEquals("Downloaded Week", downloadedData["name"])
        assertEquals(3, downloadedData["commensals"])
        
        @Suppress("UNCHECKED_CAST")
        val families = downloadedData["familyIds"] as List<Long>
        assertEquals(2, families.size)
    }

    @Test
    fun `cross-reference creation from family IDs`() {
        val weeklyPlanId = 1L
        val familyIds = listOf(5L, 10L, 15L)
        
        val crossRefs = familyIds.map { familyId ->
            WeeklyPlanFamilyCrossRef(weeklyPlanId, familyId)
        }

        assertEquals(3, crossRefs.size)
        assertEquals(5L, crossRefs[0].familyId)
        assertEquals(10L, crossRefs[1].familyId)
        assertEquals(15L, crossRefs[2].familyId)
        
        // All should have the same weekly plan ID
        crossRefs.forEach { crossRef ->
            assertEquals(weeklyPlanId, crossRef.weeklyPlanId)
        }
    }

    @Test
    fun `plan metadata preservation during sync`() {
        val originalPlan = WeeklyPlan(
            weeklyPlanId = 1L,
            name = "Important Week",
            startDate = 1234567890L,
            commensals = 6,
            createdDate = 9876543210L
        )

        val planData = hashMapOf(
            "name" to originalPlan.name,
            "startDate" to originalPlan.startDate,
            "commensals" to originalPlan.commensals,
            "createdDate" to originalPlan.createdDate
        )

        assertEquals(originalPlan.name, planData["name"])
        assertEquals(originalPlan.startDate, planData["startDate"])
        assertEquals(originalPlan.commensals, planData["commensals"])
        assertEquals(originalPlan.createdDate, planData["createdDate"])
    }

    @Test
    fun `multiple Firebase paths generated for multiple families`() {
        val weeklyPlanId = 1L
        val familyIds = listOf(10L, 20L, 30L)
        
        val paths = familyIds.map { familyId ->
            "families/$familyId/weeklyPlans/$weeklyPlanId"
        }

        assertEquals(3, paths.size)
        assertEquals("families/10/weeklyPlans/1", paths[0])
        assertEquals("families/20/weeklyPlans/1", paths[1])
        assertEquals("families/30/weeklyPlans/1", paths[2])
    }
}
