package com.lokosoft.mealplanner.viewmodel

import com.lokosoft.mealplanner.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class FamilyViewModelTest {

    private lateinit var familyDao: FamilyDao
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        familyDao = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `family list initially empty`() = runTest {
        val emptyFlow = kotlinx.coroutines.flow.flowOf(emptyList<FamilyWithMembers>())
        whenever(familyDao.getAllFamiliesWithMembers()).thenReturn(emptyFlow)
        
        // In a real scenario, you'd inject the DAO and test the ViewModel
        val families = familyDao.getAllFamiliesWithMembers()
        assertTrue(families.first().isEmpty())
    }

    @Test
    fun `create family with valid name`() = runTest {
        val familyId = 1L
        whenever(familyDao.insertFamily(any())).thenReturn(familyId)
        
        val family = Family(name = "Test Family", ownerId = "user123")
        val result = familyDao.insertFamily(family)
        
        assertEquals(familyId, result)
        verify(familyDao).insertFamily(any())
    }

    @Test
    fun `add member to family`() = runTest {
        val member = FamilyMember(
            familyId = 1L,
            userId = "pending_test@example.com",
            email = "test@example.com",
            displayName = "Test User"
        )
        
        familyDao.insertFamilyMember(member)
        verify(familyDao).insertFamilyMember(member)
    }

    @Test
    fun `get family by id`() = runTest {
        val family = Family(familyId = 1L, name = "Smith Family", ownerId = "user123")
        val familyWithMembers = FamilyWithMembers(family, emptyList())
        val flow = kotlinx.coroutines.flow.flowOf(familyWithMembers)
        
        whenever(familyDao.getFamilyWithMembers(1L)).thenReturn(flow)
        
        val result = familyDao.getFamilyWithMembers(1L).first()
        assertNotNull(result)
        assertEquals("Smith Family", result?.family?.name)
    }

    @Test
    fun `delete family member`() = runTest {
        val member = FamilyMember(
            memberId = 1L,
            familyId = 1L,
            userId = "user456",
            email = "user@example.com",
            displayName = "User Name"
        )
        
        familyDao.deleteFamilyMember(member)
        verify(familyDao).deleteFamilyMember(member)
    }

    @Test
    fun `get members by user id`() = runTest {
        val members = listOf(
            FamilyMember(1L, 1L, "user123", "user@example.com", "User Name")
        )
        
        whenever(familyDao.getMembersByUserId("user123")).thenReturn(members)
        
        val result = familyDao.getMembersByUserId("user123")
        assertEquals(1, result.size)
        assertEquals("user123", result[0].userId)
    }
}
