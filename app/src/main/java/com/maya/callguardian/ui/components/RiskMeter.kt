package com.maya.callguardian.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maya.callguardian.domain.models.RiskLevel
import com.maya.callguardian.ui.theme.*

@Composable
fun RiskMeter(
    score: Int,
    level: RiskLevel,
    modifier: Modifier = Modifier,
    size: Int = 160
) {
    val targetColor = when (level) {
        RiskLevel.LOW -> RiskLowGreen
        RiskLevel.MEDIUM -> RiskMediumAmber
        RiskLevel.HIGH -> RiskHighOrange
        RiskLevel.CRITICAL -> RiskCriticalRed
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(500),
        label = "RiskColor"
    )

    val progress = (score / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600),
        label = "RiskProgress"
    )

    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val strokeWidth = 14.dp.toPx()

            // Background Track
            drawArc(
                color = NavyBorder,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Dynamic Risk Arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(NavyBorder, animatedColor, animatedColor)
                ),
                startAngle = 140f,
                sweepAngle = 260f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$score",
                fontSize = if (size > 140) 38.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                color = animatedColor
            )
            Text(
                text = "/ 100",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .background(
                        color = animatedColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = level.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = animatedColor
                )
            }
        }
    }
}
