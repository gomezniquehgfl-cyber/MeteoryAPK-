package com.meteory.optimizer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meteory.optimizer.navigation.Screen
import com.meteory.optimizer.ui.theme.*

@Composable
fun MeteoryBottomNavBar(
    items: List<Screen>,
    currentRoute: String?,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = Neutral900,
        contentColor   = Neutral300,
        tonalElevation = 0.dp,
        modifier       = Modifier.height(72.dp)
    ) {
        items.forEach { screen ->
            val selected = currentRoute == screen.route
            val iconColor by animateColorAsState(
                targetValue = if (selected) Primary400 else Neutral400,
                animationSpec = tween(200),
                label = "navColor"
            )
            NavigationBarItem(
                selected = selected,
                onClick  = { onNavigate(screen) },
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (selected) {
                            Box(
                                Modifier
                                    .width(32.dp)
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(Primary400)
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.label,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text  = screen.label,
                        color = iconColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor        = Color.Transparent,
                    selectedIconColor     = Primary400,
                    unselectedIconColor   = Neutral400,
                    selectedTextColor     = Primary400,
                    unselectedTextColor   = Neutral400
                )
            )
        }
    }
}
