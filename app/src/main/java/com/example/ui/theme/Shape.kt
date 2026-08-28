package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Shape and spacing scale.
 *
 * Corner radii are deliberately limited to five steps so the UI reads as one
 * family instead of a collection of arbitrary rounding values.
 */
val FairFareShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Layout rhythm. All screen padding and gaps come from here, which is what makes
 * the app feel evenly spaced rather than hand-tuned per screen.
 */
object Spacing {
    /** Screen edge gutter. */
    val gutter: Dp = 20.dp

    /** Vertical gap between major sections. */
    val section: Dp = 24.dp

    /** Gap between sibling cards in a list. */
    val card: Dp = 12.dp

    /** Internal card padding. */
    val cardPadding: Dp = 18.dp

    /** Gap between tightly related items (icon + label, chips). */
    val tight: Dp = 8.dp

    /** Smallest meaningful gap. */
    val hairline: Dp = 4.dp

    /**
     * Bottom padding for scrollable content so the last item clears the
     * navigation bar and does not sit flush against it.
     */
    val scrollBottom: Dp = 32.dp

    /** Minimum touch target, per accessibility guidance. */
    val minTouchTarget: Dp = 48.dp
}
