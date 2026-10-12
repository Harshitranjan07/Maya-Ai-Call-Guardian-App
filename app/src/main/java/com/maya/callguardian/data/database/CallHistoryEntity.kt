package com.maya.callguardian.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistent record of an analyzed call session.
 * Stores only privacy-cleared metadata unless user enables local history storage.
 */
@Entity(tableName = "call_history")
data class CallHistoryEntity(
    @PrimaryKey
    val sessionId: String,
    val phoneNumber: String,
    val callerName: String,
    val contactStatus: String,
    val startTimeMs: Long,
    val durationSeconds: Long,
    val finalRiskScore: Int,
    val finalRiskLevel: String,
    val scamCategory: String,
    val detectedSignalsJson: String,
    val evidenceCount: Int,
    val primaryExplanation: String,
    val operationMode: String
)
