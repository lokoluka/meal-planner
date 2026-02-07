package com.lokosoft.mealplanner.family

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lokosoft.mealplanner.data.AppDatabase
import com.lokosoft.mealplanner.data.Family
import com.lokosoft.mealplanner.data.FamilyMember
import com.lokosoft.mealplanner.data.FamilyWithMembers
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FamilyViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val familyDao = database.familyDao()
    private val mealPlanDao = database.mealPlanDao()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var familiesListener: ListenerRegistration? = null
    
    private val _families = MutableStateFlow<List<FamilyWithMembers>>(emptyList())
    val families: StateFlow<List<FamilyWithMembers>> = _families.asStateFlow()
    
    private val _selectedFamily = MutableStateFlow<FamilyWithMembers?>(null)
    val selectedFamily: StateFlow<FamilyWithMembers?> = _selectedFamily.asStateFlow()

    private val _familyPlans = MutableStateFlow<List<com.lokosoft.mealplanner.data.WeeklyPlan>>(emptyList())
    val familyPlans: StateFlow<List<com.lokosoft.mealplanner.data.WeeklyPlan>> = _familyPlans.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode.asStateFlow()

    private var familyPlansJob: Job? = null
    
    init {
        observeUserFamilies()
    }
    
    private fun observeUserFamilies() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _families.value = emptyList()
            return
        }

        familiesListener?.remove()
        familiesListener = firestore.collection("users")
            .document(currentUser.uid)
            .collection("families")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _errorMessage.value = error.message
                    return@addSnapshotListener
                }

                val familyIds = snapshot?.documents?.mapNotNull { doc ->
                    doc.getLong("familyId") ?: doc.id.toLongOrNull()
                } ?: emptyList()

                viewModelScope.launch {
                    _families.value = buildFamiliesList(familyIds)
                }
            }
    }

    private suspend fun buildFamiliesList(familyIds: List<Long>): List<FamilyWithMembers> {
        val result = mutableListOf<FamilyWithMembers>()
        familyIds.forEach { familyId ->
            val familyDoc = firestore.collection("families")
                .document(familyId.toString())
                .get()
                .await()

            if (!familyDoc.exists()) return@forEach

            val name = familyDoc.getString("name") ?: return@forEach
            val ownerId = familyDoc.getString("ownerId") ?: ""
            val createdDate = familyDoc.getLong("createdDate") ?: System.currentTimeMillis()

            val membersSnapshot = firestore.collection("families")
                .document(familyId.toString())
                .collection("members")
                .get()
                .await()

            val members = membersSnapshot.documents.map { doc ->
                val email = doc.getString("email") ?: ""
                val displayName = doc.getString("displayName")
                val joinedDate = doc.getLong("joinedDate") ?: System.currentTimeMillis()
                FamilyMember(
                    familyId = familyId,
                    userId = doc.id,
                    email = email,
                    displayName = displayName,
                    joinedDate = joinedDate
                )
            }

            val family = Family(
                familyId = familyId,
                name = name,
                ownerId = ownerId,
                createdDate = createdDate
            )

            // Keep local Room table in sync to satisfy foreign keys.
            familyDao.insertFamily(family)

            result.add(
                FamilyWithMembers(
                    family = family,
                    members = members
                )
            )
        }

        return result.sortedByDescending { it.family.createdDate }
    }
    
    fun selectFamily(familyId: Long) {
        loadFamilyPlans(familyId)
        _selectedFamily.value = _families.value.firstOrNull { it.family.familyId == familyId }
    }
    
    fun deselectFamily() {
        _selectedFamily.value = null
        _familyPlans.value = emptyList()
    }

    private fun loadFamilyPlans(familyId: Long) {
        familyPlansJob?.cancel()
        familyPlansJob = viewModelScope.launch {
            mealPlanDao.getWeeklyPlansForFamilyFlow(familyId).collect { plans ->
                _familyPlans.value = plans
            }
        }
    }
    
    fun createFamily(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _errorMessage.value = "You must be logged in to create a family"
                    _isLoading.value = false
                    return@launch
                }
                
                val familyId = System.currentTimeMillis()
                val familyData = hashMapOf(
                    "name" to name,
                    "ownerId" to currentUser.uid,
                    "createdDate" to System.currentTimeMillis()
                )

                firestore.collection("families")
                    .document(familyId.toString())
                    .set(familyData)
                    .await()

                val memberData = hashMapOf(
                    "email" to (currentUser.email ?: ""),
                    "displayName" to currentUser.displayName,
                    "joinedDate" to System.currentTimeMillis()
                )

                firestore.collection("families")
                    .document(familyId.toString())
                    .collection("members")
                    .document(currentUser.uid)
                    .set(memberData)
                    .await()

                val userFamilyData = hashMapOf(
                    "familyId" to familyId,
                    "joinedDate" to System.currentTimeMillis()
                )

                firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("families")
                    .document(familyId.toString())
                    .set(userFamilyData)
                    .await()

                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun createInviteCode(familyId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _inviteCode.value = null

            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _errorMessage.value = "You must be logged in to create an invite"
                    _isLoading.value = false
                    return@launch
                }

                var code = generateInviteCode()
                var attempts = 0
                while (attempts < 5) {
                    val existing = firestore.collection("familyInvites")
                        .document(code)
                        .get()
                        .await()
                    if (!existing.exists()) break
                    code = generateInviteCode()
                    attempts += 1
                }

                if (attempts >= 5) {
                    _errorMessage.value = "Unable to generate invite code"
                    _isLoading.value = false
                    return@launch
                }

                val inviteData = hashMapOf(
                    "familyId" to familyId,
                    "createdBy" to currentUser.uid,
                    "createdAt" to System.currentTimeMillis(),
                    "usedBy" to null,
                    "usedAt" to null
                )

                firestore.collection("familyInvites")
                    .document(code)
                    .set(inviteData)
                    .await()

                _inviteCode.value = code
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun clearInviteCode() {
        _inviteCode.value = null
    }

    fun joinFamilyWithCode(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _errorMessage.value = "You must be logged in to join a family"
                    _isLoading.value = false
                    return@launch
                }

                val normalizedCode = code.trim().uppercase()
                val inviteRef = firestore.collection("familyInvites").document(normalizedCode)
                val inviteSnapshot = inviteRef.get().await()
                if (!inviteSnapshot.exists()) {
                    _errorMessage.value = "Invite code not found"
                    _isLoading.value = false
                    return@launch
                }

                val usedBy = inviteSnapshot.getString("usedBy")
                if (!usedBy.isNullOrBlank()) {
                    _errorMessage.value = "Invite code already used"
                    _isLoading.value = false
                    return@launch
                }

                val familyId = inviteSnapshot.getLong("familyId")
                if (familyId == null) {
                    _errorMessage.value = "Invalid invite code"
                    _isLoading.value = false
                    return@launch
                }

                val memberData = hashMapOf(
                    "email" to (currentUser.email ?: ""),
                    "displayName" to currentUser.displayName,
                    "joinedDate" to System.currentTimeMillis()
                )

                firestore.collection("families")
                    .document(familyId.toString())
                    .collection("members")
                    .document(currentUser.uid)
                    .set(memberData, SetOptions.merge())
                    .await()

                val userFamilyData = hashMapOf(
                    "familyId" to familyId,
                    "joinedDate" to System.currentTimeMillis()
                )

                firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("families")
                    .document(familyId.toString())
                    .set(userFamilyData, SetOptions.merge())
                    .await()

                inviteRef.update(
                    mapOf(
                        "usedBy" to currentUser.uid,
                        "usedAt" to System.currentTimeMillis()
                    )
                ).await()

                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }
    
    fun removeMember(member: FamilyMember) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                firestore.collection("families")
                    .document(member.familyId.toString())
                    .collection("members")
                    .document(member.userId)
                    .delete()
                    .await()

                firestore.collection("users")
                    .document(member.userId)
                    .collection("families")
                    .document(member.familyId.toString())
                    .delete()
                    .await()

                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }
    
    fun deleteFamily(family: Family) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val familyId = family.familyId.toString()
                val membersSnapshot = firestore.collection("families")
                    .document(familyId)
                    .collection("members")
                    .get()
                    .await()

                val batch = firestore.batch()
                membersSnapshot.documents.forEach { doc ->
                    val memberId = doc.id
                    batch.delete(doc.reference)
                    batch.delete(
                        firestore.collection("users")
                            .document(memberId)
                            .collection("families")
                            .document(familyId)
                    )
                }

                batch.delete(
                    firestore.collection("families")
                        .document(familyId)
                )

                batch.commit().await()
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        familiesListener?.remove()
        familiesListener = null
        super.onCleared()
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val codeLength = 6
        return (1..codeLength)
            .map { chars.random() }
            .joinToString("")
    }
}
