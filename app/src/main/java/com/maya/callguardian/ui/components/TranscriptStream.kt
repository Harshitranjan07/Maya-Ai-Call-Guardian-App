package com.maya.callguardian.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maya.callguardian.domain.models.Speaker
import com.maya.callguardian.domain.models.TranscriptEvent
import com.maya.callguardian.ui.theme.*

@Composable
fun TranscriptStream(
    transcripts: List<TranscriptEvent>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(transcripts.size) {
        if (transcripts.isNotEmpty()) {
            listState.animateScrollToItem(transcripts.size - 1)
        }
    }

    if (transcripts.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(NavySurfaceElevated, RoundedCornerShape(12.dp))
                .border(1.dp, NavyBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Listening for audio stream...\nTranscripts appear here as caller speaks.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transcripts) { event ->
                val isCaller = event.speaker == Speaker.CALLER
                val bubbleColor = if (isCaller) NavySurfaceElevated else VioletAccent.copy(alpha = 0.2f)
                val borderColor = if (isCaller) NavyBorder else VioletAccent.copy(alpha = 0.5f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isCaller) Arrangement.Start else Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .background(bubbleColor, RoundedCornerShape(12.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isCaller) "CALLER" else "YOU",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCaller) CyanAccent else VioletLight
                                )
                                if (!event.isFinal) {
                                    Text(
                                        text = "TRANSCRIPTION IN PROGRESS...",
                                        fontSize = 9.sp,
                                        color = TextTertiary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = event.text,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
