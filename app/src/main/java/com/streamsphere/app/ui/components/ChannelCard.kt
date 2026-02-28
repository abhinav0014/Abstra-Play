package com.streamsphere.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.streamsphere.app.data.model.ChannelUiModel
import com.streamsphere.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelCard(
    model: ChannelUiModel,
    onFavouriteClick: () -> Unit,
    onWidgetClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    val categoryColor = when {
        model.categories.any { it in listOf("music","entertainment") } -> MusicPurple
        model.categories.any { it in listOf("science","education","kids") } -> ScienceBlue
        model.country.contains("Nepal", ignoreCase = true) -> NepalRed
        model.country.contains("India", ignoreCase = true) -> IndiaOrange
        else -> Primary
    }

    Card(
        onClick = onCardClick,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 4.dp
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Logo
                LogoImage(
                    logoUrl = model.logoUrl,
                    name    = model.name,
                    color   = categoryColor
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text     = model.name,
                        style    = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = "${model.countryFlag} ${model.country}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Favourite button
                AnimatedFavButton(
                    isFavourite = model.isFavourite,
                    onClick     = onFavouriteClick
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                model.categories.take(2).forEach { cat ->
                    CategoryChip(cat, categoryColor)
                }
                Spacer(modifier = Modifier.weight(1f))

                // Widget button
                if (model.isFavourite) {
                    AnimatedWidgetButton(
                        isWidget = model.isWidget,
                        onClick  = onWidgetClick
                    )
                }

                // Stream indicator
                if (model.streamUrl != null) {
                    LiveBadge()
                }
            }
        }
    }
}

@Composable
fun LogoImage(logoUrl: String?, name: String, color: Color) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl != null) {
            AsyncImage(
                model             = logoUrl,
                contentDescription = name,
                contentScale      = ContentScale.Fit,
                modifier          = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            )
        } else {
            Text(
                text  = name.firstOrNull()?.toString() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                color = color
            )
        }
    }
}

@Composable
fun AnimatedFavButton(isFavourite: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isFavourite) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "fav_scale"
    )
    IconButton(
        onClick  = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector        = if (isFavourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Favourite",
            tint               = if (isFavourite) Color(0xFFFC8181) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier.scale(scale)
        )
    }
}

@Composable
fun AnimatedWidgetButton(isWidget: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(
        targetValue = if (isWidget) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label       = "widget_color"
    )
    IconButton(
        onClick  = onClick,
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            imageVector        = if (isWidget) Icons.Filled.Widgets else Icons.Outlined.Widgets,
            contentDescription = "Add to Widget",
            tint               = color,
            modifier           = Modifier.size(18.dp)
        )
    }
}

@Composable
fun CategoryChip(category: String, color: Color) {
    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = color.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text     = category.replaceFirstChar { it.uppercase() },
            style    = MaterialTheme.typography.labelSmall,
            color    = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun LiveBadge() {
    Row(
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .background(Color(0xFFE53E3E).copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .border(0.5.dp, Color(0xFFE53E3E).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue   = 1f,
            targetValue    = 0.3f,
            animationSpec  = infiniteRepeatable(
                animation  = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "live_alpha"
        )
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(Color(0xFFE53E3E).copy(alpha = alpha), CircleShape)
        )
        Text(
            text  = "LIVE",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFE53E3E)
        )
    }
}
