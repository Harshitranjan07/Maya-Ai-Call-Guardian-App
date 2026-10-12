package com.maya.callguardian.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maya.callguardian.domain.models.RiskLevel
import com.maya.callguardian.domain.models.SecurityPolicyDecision
import com.maya.callguardian.ui.theme.*

@Composable
fun SecurityBanner(
    policy: SecurityPolicyDecision,
    onTakeOverClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (borderColor, bgColor, title) = when (policy.level) {
        RiskLevel.LOW -> Triple(NavyBorder, NavySurfaceElevated, "PASSIVE GUARDIAN ACTIVE")
        RiskLevel.MEDIUM -> Triple(RiskMediumAmber, RiskMediumAmber.copy(alpha = 0.12f), "SUSPICIOUS ACTIVITY ADVISORY")
        RiskLevel.HIGH -> Triple(RiskHighOrange, RiskHighOrange.copy(alpha = 0.16f), "HIGH RISK FRAUD ADVISORY")
        RiskLevel.CRITICAL -> Triple(RiskCriticalRed, RiskCriticalRed.copy(alpha = 0.22f), "CRITICAL PROTECTION ENFORCED")
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = borderColor
                )

                if (policy.showTakeOverAction && onTakeOverClick != null) {
                    FilledTonalButton(
                        onClick = onTakeOverClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = borderColor,
                            contentColor = NavyDark
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Take Over", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = policy.immediateSafetyWarning ?: policy.bannerMessage,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = if (policy.level == RiskLevel.CRITICAL) FontWeight.Bold else FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}
