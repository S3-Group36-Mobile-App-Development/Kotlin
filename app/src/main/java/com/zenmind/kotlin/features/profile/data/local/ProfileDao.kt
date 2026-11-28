package com.zenmind.kotlin.features.profile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE userId = :userId")
    suspend fun getProfile(userId: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE userId = :userId")
    fun observeProfile(userId: String): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: ProfileEntity)

    @Transaction
    suspend fun saveDownloadedProfile(previous: ProfileEntity?, downloaded: ProfileEntity): ProfileEntity {
        val current = getProfile(downloaded.userId)
        if (current == previous) {
            saveProfile(downloaded)
            return downloaded
        }
        return requireNotNull(current)
    }

    @Query("UPDATE profiles SET name = :name WHERE userId = :userId")
    suspend fun updateName(userId: String, name: String)

    @Query("UPDATE profiles SET institutionId = :institutionId, departmentId = :departmentId WHERE userId = :userId")
    suspend fun updateInstitution(userId: String, institutionId: String, departmentId: String?)

    @Query("SELECT * FROM pending_profile_changes WHERE userId = :userId")
    suspend fun getPending(userId: String): PendingProfileEntity?

    @Query("SELECT * FROM pending_profile_changes WHERE userId = :userId")
    fun observePending(userId: String): Flow<PendingProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePending(pending: PendingProfileEntity)

    @Query("DELETE FROM pending_profile_changes WHERE userId = :userId")
    suspend fun deletePending(userId: String)

    @Transaction
    suspend fun replacePending(pending: PendingProfileEntity) {
        if (pending.hasChanges) savePending(pending) else deletePending(pending.userId)
    }

    @Query("UPDATE pending_profile_changes SET pendingName = NULL WHERE userId = :userId AND pendingName = :sentName")
    suspend fun clearSentName(userId: String, sentName: String)

    @Query("""UPDATE pending_profile_changes SET pendingInstitutionId = NULL, pendingDepartmentId = NULL
        WHERE userId = :userId AND pendingInstitutionId = :sentInstitutionId
        AND pendingDepartmentId IS :sentDepartmentId""")
    suspend fun clearSentInstitution(userId: String, sentInstitutionId: String, sentDepartmentId: String?)

    @Query("DELETE FROM pending_profile_changes WHERE userId = :userId AND pendingName IS NULL AND pendingInstitutionId IS NULL")
    suspend fun deleteIfEmpty(userId: String)

    @Query("SELECT * FROM institutions ORDER BY name")
    suspend fun getInstitutions(): List<InstitutionEntity>

    @Query("SELECT * FROM departments WHERE institutionId = :institutionId ORDER BY name")
    suspend fun getDepartments(institutionId: String): List<DepartmentEntity>

    @Query("DELETE FROM departments")
    suspend fun clearDepartments()

    @Query("DELETE FROM institutions")
    suspend fun clearInstitutions()

    @Insert
    suspend fun saveInstitutions(institutions: List<InstitutionEntity>)

    @Insert
    suspend fun saveDepartments(departments: List<DepartmentEntity>)

    @Transaction
    suspend fun replaceCatalog(institutions: List<InstitutionEntity>, departments: List<DepartmentEntity>) {
        clearDepartments()
        clearInstitutions()
        saveInstitutions(institutions)
        saveDepartments(departments)
    }
}
