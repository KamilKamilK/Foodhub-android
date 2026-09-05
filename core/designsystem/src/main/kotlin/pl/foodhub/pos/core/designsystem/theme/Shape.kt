package pl.foodhub.pos.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Radius scale from foodhub-app's tokens.css (--radius-sm/md/lg/xl: 14/18/24/32).
val FoodHubShapes =
    Shapes(
        extraSmall = RoundedCornerShape(14.dp),
        small = RoundedCornerShape(18.dp),
        medium = RoundedCornerShape(24.dp),
        large = RoundedCornerShape(32.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )
