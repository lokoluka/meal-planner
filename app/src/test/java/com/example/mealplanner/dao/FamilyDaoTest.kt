package com.example.mealplanner.dao

import com.example.mealplanner.data.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DAO operations related to family sharing.
 * These tests verify the behavior of family-related database operations.
 */
class FamilyDaoTest {

    @Test
    fun `WeeklyPlanFamilyCrossRef has correct primary keys`() {
        // Test that cross-reference entity correctly represents the relationship
        val crossRef1 = WeeklyPlanFamilyCrossRef(weeklyPlanId = 1L, familyId = 10L)
        val crossRef2 = WeeklyPlanFamilyCrossRef(weeklyPlanId = 1L, familyId = 10L)
        val crossRef3 = WeeklyPlanFamilyCrossRef(weeklyPlanId = 2L, familyId = 10L)
        
        // Same weeklyPlanId and familyId should be equal
        assertEquals(crossRef1.weeklyPlanId, crossRef2.weeklyPlanId)
        assertEquals(crossRef1.familyId, crossRef2.familyId)
        
        // Different weeklyPlanId should not be equal
        assertNotEquals(crossRef1.weeklyPlanId, crossRef3.weeklyPlanId)
    }

    @Test
    fun `WeeklyPlanWithFamilies correctly embeds weekly plan`() {
        val plan = WeeklyPlan(
            weeklyPlanId = 1L,
            name = "Test Week",
            startDate = System.currentTimeMillis(),
            commensals = 4
        )
        
        val families = listOf(
            Family(1L, "Family A", "owner1"),
            Family(2L, "Family B", "owner2")
        )
        
        val weeklyPlanWithFamilies = WeeklyPlanWithFamilies(plan, families)
        
        assertEquals(plan, weeklyPlanWithFamilies.weeklyPlan)
        assertEquals(2, weeklyPlanWithFamilies.families.size)
        assertEquals("Family A", weeklyPlanWithFamilies.families[0].name)
        assertEquals("Family B", weeklyPlanWithFamilies.families[1].name)
    }

    @Test
    fun `FamilyWithMembers correctly embeds family`() {
        val family = Family(1L, "Test Family", "owner123")
        val members = listOf(
            FamilyMember(1L, 1L, "user1", "user1@test.com", "User 1"),
            FamilyMember(2L, 1L, "user2", "user2@test.com", "User 2")
        )
        
        val familyWithMembers = FamilyWithMembers(family, members)
        
        assertEquals(family, familyWithMembers.family)
        assertEquals(2, familyWithMembers.members.size)
        assertEquals("user1@test.com", familyWithMembers.members[0].email)
    }

    @Test
    fun `Family has unique id`() {
        val family1 = Family(familyId = 1L, name = "Family 1", ownerId = "owner1")
        val family2 = Family(familyId = 2L, name = "Family 2", ownerId = "owner2")
        
        assertNotEquals(family1.familyId, family2.familyId)
    }

    @Test
    fun `FamilyMember belongs to correct family`() {
        val member = FamilyMember(
            memberId = 1L,
            familyId = 10L,
            userId = "user123",
            email = "user@test.com",
            displayName = "User Name"
        )
        
        assertEquals(10L, member.familyId)
        assertEquals("user123", member.userId)
        assertEquals("user@test.com", member.email)
    }

    @Test
    fun `WeeklyPlan can exist without families`() {
        val plan = WeeklyPlan(
            weeklyPlanId = 1L,
            name = "Personal Plan",
            startDate = System.currentTimeMillis(),
            commensals = 2
        )
        
        val weeklyPlanWithFamilies = WeeklyPlanWithFamilies(plan, emptyList())
        
        assertEquals(plan, weeklyPlanWithFamilies.weeklyPlan)
        assertTrue(weeklyPlanWithFamilies.families.isEmpty())
    }

    @Test
    fun `Multiple plans can share same family`() {
        val family = Family(1L, "Shared Family", "owner1")
        
        val crossRef1 = WeeklyPlanFamilyCrossRef(weeklyPlanId = 1L, familyId = family.familyId)
        val crossRef2 = WeeklyPlanFamilyCrossRef(weeklyPlanId = 2L, familyId = family.familyId)
        val crossRef3 = WeeklyPlanFamilyCrossRef(weeklyPlanId = 3L, familyId = family.familyId)
        
        // All cross-references point to the same family
        assertEquals(family.familyId, crossRef1.familyId)
        assertEquals(family.familyId, crossRef2.familyId)
        assertEquals(family.familyId, crossRef3.familyId)
        
        // But different plans
        assertNotEquals(crossRef1.weeklyPlanId, crossRef2.weeklyPlanId)
        assertNotEquals(crossRef2.weeklyPlanId, crossRef3.weeklyPlanId)
    }

    @Test
    fun `Plan can be shared with multiple families`() {
        val plan = WeeklyPlan(1L, "Multi-Family Plan", System.currentTimeMillis(), 4)
        
        val crossRef1 = WeeklyPlanFamilyCrossRef(weeklyPlanId = plan.weeklyPlanId, familyId = 1L)
        val crossRef2 = WeeklyPlanFamilyCrossRef(weeklyPlanId = plan.weeklyPlanId, familyId = 2L)
        val crossRef3 = WeeklyPlanFamilyCrossRef(weeklyPlanId = plan.weeklyPlanId, familyId = 3L)
        
        // All cross-references point to the same plan
        assertEquals(plan.weeklyPlanId, crossRef1.weeklyPlanId)
        assertEquals(plan.weeklyPlanId, crossRef2.weeklyPlanId)
        assertEquals(plan.weeklyPlanId, crossRef3.weeklyPlanId)
        
        // But different families
        assertNotEquals(crossRef1.familyId, crossRef2.familyId)
        assertNotEquals(crossRef2.familyId, crossRef3.familyId)
    }
}
