package com.zenmind.kotlin.features.profile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val userId: String,
    val email: String,
    val name: String,
    val createdAt: String,
    val institutionId: String?,
    val departmentId: String?,
    val currentStreak: Int?,
    val bestStreak: Int?
)

@Entity(tableName = "institutions")
data class InstitutionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String
)

@Entity(tableName = "departments")
data class DepartmentEntity(
    @PrimaryKey val id: String,
    val institutionId: String,
    val name: String
)

@Entity(tableName = "pending_profile_changes")
data class PendingProfileEntity(
    @PrimaryKey val userId: String,
    val pendingName: String?,
    val pendingInstitutionId: String?,
    val pendingDepartmentId: String?
) {
    val hasChanges: Boolean get() = pendingName != null || pendingInstitutionId != null
}
