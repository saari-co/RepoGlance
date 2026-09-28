package co.saari.repoglance.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

enum class LabelRole { SECTION, CHIP, META, BUTTON }

@Immutable
data class LabelStyle(val style: TextStyle, val uppercase: Boolean = false)

@Immutable
data class LabelType(
    val section: LabelStyle? = null,
    val chip: LabelStyle? = null,
    val meta: LabelStyle? = null,
    val button: LabelStyle? = null,
) {
    fun of(role: LabelRole): LabelStyle? = when (role) {
        LabelRole.SECTION -> section
        LabelRole.CHIP -> chip
        LabelRole.META -> meta
        LabelRole.BUTTON -> button
    }

    companion object {
        val Material = LabelType()

        val Mono = LabelType(
            section = LabelStyle(mono(12f, FontWeight.SemiBold)),
            chip = LabelStyle(mono(12.5f, FontWeight.Normal)),
            meta = LabelStyle(mono(11f, FontWeight.Normal)),
            button = LabelStyle(mono(14f, FontWeight.Medium)),
        )

        val Default = Mono

        private fun mono(size: Float, weight: FontWeight) = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = size.sp,
            fontWeight = weight,
            letterSpacing = 0.sp,
        )
    }
}

val LocalLabelType = staticCompositionLocalOf { LabelType.Default }

@Composable
fun LabelText(
    text: String,
    role: LabelRole,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val label = LocalLabelType.current.of(role)
    Text(
        text = if (label?.uppercase == true) text.uppercase() else text,
        modifier = modifier,
        color = color,
        style = if (label == null) style else style.merge(label.style),
        maxLines = maxLines,
        overflow = overflow,
    )
}
