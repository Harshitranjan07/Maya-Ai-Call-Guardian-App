package com.maya.callguardian.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maya.callguardian.domain.models.EvidenceItem
import com.maya.callguardian.ui.theme.*

@Composable
fun EvidenceCard(
    evidence: EvidenceItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(NavySurfaceElevated, RoundedCornerShape(10.dp))
            .border(1.dp, NavyBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Signal Badge
                Box(
                    modifier = Modifier
                        .background(VioletAccent.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = evidence.signal.name.replace("_", " "),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VioletLight
                    )
                }

                // Source indicator
                Text(
                    text = "SRC: ${evidence.source}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Excerpt
            Text(
                text = "“${evidence.excerpt}”",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = TextPrimary
            )
        }
    }
}
