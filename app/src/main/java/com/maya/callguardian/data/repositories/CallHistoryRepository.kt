package com.maya.callguardian.data.repositories

import com.maya.callguardian.data.database.CallHistoryDao
import com.maya.callguardian.data.database.CallHistoryEntity
import com.maya.callguardian.domain.models.CallSessionState
import kotlinx.coroutines.flow.Flow

class CallHistoryRepository(
    private val dao: CallHistoryDao
) {
    val allSessions: Flow<List<CallHistoryEntity>> = dao.getAllSessions()

    suspend fun saveSession(session: CallSessionState) {
        val signalsString = session.scamAssessment.detectedSignals.joinToString(",") { it.name }
        val entity = CallHistoryEntity(
            sessionId = session.sessionId,
            phoneNumber = session.callerInfo.phoneNumber,
            callerName = session.callerInfo.displayName,
            contactStatus = session.callerInfo.contactStatus.name,
            startTimeMs = session.callStartTimeMs,
            durationSeconds = session.durationSeconds,
            finalRiskScore = session.riskAssessment.score,
            finalRiskLevel = session.riskAssessment.level.name,
            scamCategory = session.scamAssessment.category.displayName,
            detectedSignalsJson = signalsString,
            evidenceCount = session.scamAssessment.evidenceList.size,
            primaryExplanation = session.riskAssessment.primaryThreat,
            operationMode = session.operationMode.name
        )
        dao.insertSession(entity)
    }

    suspend fun getSession(id: String): CallHistoryEntity? {
        return dao.getSessionById(id)
    }

    suspend fun deleteSession(id: String) {
        dao.deleteSessionById(id)
    }

    suspend fun clearHistory() {
        dao.clearAllHistory()
    }

    suspend fun getCount(): Int {
        return dao.getSessionCount()
    }
}
