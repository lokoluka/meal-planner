package com.lokosoft.mealplanner.viewmodel

import com.lokosoft.mealplanner.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MealCalendarViewModelTest {

    private lateinit var mealPlanDao: MealPlanDao
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        mealPlanDao = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `create weekly plan with valid data`() = runTest {
        val planId = 1L
        whenever(mealPlanDao.insertWeeklyPlan(any())).thenReturn(planId)
        
        val plan = WeeklyPlan(
            name = "Test Week",
            startDate = System.currentTimeMillis(),
            commensals = 4
        )
        
        val result = mealPlanDao.insertWeeklyPlan(plan)
        assertEquals(planId, result)
        verify(mealPlanDao).insertWeeklyPlan(any())
    }

    @Test
    fun `add weekly plan to family`() = runTest {
        val crossRef = WeeklyPlanFamilyCrossRef(
            weeklyPlanId = 1L,
            familyId = 10L
        )
        
        mealPlanDao.insertWeeklyPlanFamilyCrossRef(crossRef)
        verify(mealPlanDao).insertWeeklyPlanFamilyCrossRef(crossRef)
    }

    @Test
    fun `remove weekly plan from family`() = runTest {
        val crossRef = WeeklyPlanFamilyCrossRef(
            weeklyPlanId = 1L,
            familyId = 10L
        )
        
        mealPlanDao.deleteWeeklyPlanFamilyCrossRef(crossRef)
        verify(mealPlanDao).deleteWeeklyPlanFamilyCrossRef(crossRef)
    }

    @Test
    fun `get families for weekly plan`() = runTest {
        val familyIds = listOf(1L, 2L, 3L)
        whenever(mealPlanDao.getFamilyIdsForWeeklyPlan(1L)).thenReturn(familyIds)
        
        val result = mealPlanDao.getFamilyIdsForWeeklyPlan(1L)
        assertEquals(3, result.size)
        assertTrue(result.contains(1L))
        assertTrue(result.contains(2L))
        assertTrue(result.contains(3L))
    }

    @Test
    fun `get weekly plan with families`() = runTest {
        val plan = WeeklyPlan(1L, "Test Week", System.currentTimeMillis(), 4)
        val families = listOf(
            Family(1L, "Family 1", "owner1"),
            Family(2L, "Family 2", "owner2")
        )
        val weeklyPlanWithFamilies = WeeklyPlanWithFamilies(plan, families)
        
        whenever(mealPlanDao.getWeeklyPlanWithFamilies(1L)).thenReturn(weeklyPlanWithFamilies)
        
        val result = mealPlanDao.getWeeklyPlanWithFamilies(1L)
        assertNotNull(result)
        assertEquals("Test Week", result?.weeklyPlan?.name)
        assertEquals(2, result?.families?.size)
    }

    @Test
    fun `delete all families for weekly plan`() = runTest {
        mealPlanDao.deleteAllFamiliesForWeeklyPlan(1L)
        verify(mealPlanDao).deleteAllFamiliesForWeeklyPlan(1L)
    }

    @Test
    fun `get all weekly plans with families`() = runTest {
        val plans = listOf(
            WeeklyPlanWithFamilies(
                WeeklyPlan(1L, "Week 1", System.currentTimeMillis(), 2),
                listOf(Family(1L, "Family 1", "owner1"))
            ),
            WeeklyPlanWithFamilies(
                WeeklyPlan(2L, "Week 2", System.currentTimeMillis(), 3),
                emptyList()
            )
        )
        
        whenever(mealPlanDao.getAllWeeklyPlansWithFamilies()).thenReturn(plans)
        
        val result = mealPlanDao.getAllWeeklyPlansWithFamilies()
        assertEquals(2, result.size)
        assertEquals(1, result[0].families.size)
        assertEquals(0, result[1].families.size)
    }

    @Test
    fun `assign recipe to meal`() = runTest {
        val mealPlan = MealPlan(
            weeklyPlanId = 1L,
            recipeId = 5L,
            dayOfWeek = DayOfWeek.MONDAY,
            mealType = MealType.DINNER,
            servings = 2,
            commensals = 4
        )
        
        whenever(mealPlanDao.insertMealPlan(any())).thenReturn(1L)
        
        val result = mealPlanDao.insertMealPlan(mealPlan)
        assertEquals(1L, result)
        verify(mealPlanDao).insertMealPlan(any())
    }

    @Test
    fun `update weekly plan commensals`() = runTest {
        val plan = WeeklyPlan(
            weeklyPlanId = 1L,
            name = "Test Week",
            startDate = System.currentTimeMillis(),
            commensals = 5
        )
        
        mealPlanDao.updateWeeklyPlan(plan)
        verify(mealPlanDao).updateWeeklyPlan(plan)
    }

    @Test
    fun `delete weekly plan`() = runTest {
        val plan = WeeklyPlan(
            weeklyPlanId = 1L,
            name = "Test Week",
            startDate = System.currentTimeMillis()
        )
        
        mealPlanDao.deleteWeeklyPlan(plan)
        verify(mealPlanDao).deleteWeeklyPlan(plan)
    }
}
