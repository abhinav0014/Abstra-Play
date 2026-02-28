package com.streamsphere.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.streamsphere.app.data.model.ChannelTab
import com.streamsphere.app.ui.theme.*

@Composable
fun CategoryTabRow(
    selectedTab: ChannelTab,
    counts: Map<ChannelTab, Int>,
    onTabSelected: (ChannelTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChannelTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val accentColor = tabColor(tab)

            val containerColor by animateColorAsState(
                targetValue = if (isSelected) accentColor.copy(alpha = 0.15f)
                              else Color.Transparent,
                animationSpec = spring(),
                label = "tab_container"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) accentColor
                              else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "tab_content"
            )

            FilterChip(
                selected = isSelected,
                onClick  = { onTabSelected(tab) },
                label    = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = tab.emoji)
                        Text(
                            text  = tab.label,
                            style = MaterialTheme.typography.labelLarge
                        )
                        counts[tab]?.let { count ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) accentColor.copy(0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text     = count.toString(),
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = contentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = containerColor,
                    selectedLabelColor     = contentColor,
                    labelColor             = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled          = true,
                    selected         = isSelected,
                    borderColor      = MaterialTheme.colorScheme.outline.copy(0.4f),
                    selectedBorderColor = accentColor.copy(0.4f)
                )
            )
        }
    }
}

fun tabColor(tab: ChannelTab): Color = when (tab) {
    ChannelTab.ALL     -> Primary
    ChannelTab.NEPAL   -> NepalRed
    ChannelTab.INDIA   -> IndiaOrange
    ChannelTab.SCIENCE -> ScienceBlue
    ChannelTab.MUSIC   -> MusicPurple
}
