package com.example.mealplanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDao {
    
    @Query("SELECT * FROM families ORDER BY createdDate DESC")
    fun getAllFamilies(): Flow<List<Family>>
    
    @Query("SELECT * FROM families WHERE familyId = :familyId")
    suspend fun getFamilyById(familyId: Long): Family?
    
    @Query("SELECT * FROM families WHERE ownerId = :userId")
    fun getFamiliesByOwner(userId: String): Flow<List<Family>>
    
    @Transaction
    @Query("SELECT * FROM families WHERE familyId = :familyId")
    fun getFamilyWithMembers(familyId: Long): Flow<FamilyWithMembers?>
    
    @Transaction
    @Query("SELECT * FROM families ORDER BY createdDate DESC")
    fun getAllFamiliesWithMembers(): Flow<List<FamilyWithMembers>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamily(family: Family): Long
    
    @Update
    suspend fun updateFamily(family: Family)
    
    @Delete
    suspend fun deleteFamily(family: Family)
    
    // Family Members
    @Query("SELECT * FROM family_members WHERE familyId = :familyId")
    fun getMembersByFamily(familyId: Long): Flow<List<FamilyMember>>
    
    @Query("SELECT * FROM family_members WHERE userId = :userId")
    suspend fun getMembersByUserId(userId: String): List<FamilyMember>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMember): Long
    
    @Delete
    suspend fun deleteFamilyMember(member: FamilyMember)
    
    @Query("DELETE FROM family_members WHERE familyId = :familyId AND userId = :userId")
    suspend fun removeMemberFromFamily(familyId: Long, userId: String)
    
    @Query("SELECT COUNT(*) FROM family_members WHERE familyId = :familyId")
    suspend fun getMemberCount(familyId: Long): Int
}
