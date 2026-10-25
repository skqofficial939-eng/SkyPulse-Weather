package com.skypulse.weather.ui.components

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skypulse.weather.domain.WeatherKind
import com.skypulse.weather.ui.theme.Sky

@Composable
fun rememberReduceMotion(): Boolean {
    val ctx = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

fun iconFor(kind: WeatherKind, isDay: Boolean): ImageVector = when (kind) {
    WeatherKind.CLEAR -> if (isDay) Icons.Filled.WbSunny else Icons.Filled.NightsStay
    WeatherKind.PARTLY_CLOUDY -> if (isDay) Icons.Filled.WbCloudy else Icons.Filled.Cloud
    WeatherKind.CLOUDY -> Icons.Filled.Cloud
    WeatherKind.FOG -> Icons.Filled.BlurOn
    WeatherKind.DRIZZLE -> Icons.Filled.Grain
    WeatherKind.RAIN -> Icons.Filled.WaterDrop
    WeatherKind.SNOW -> Icons.Filled.AcUnit
    WeatherKind.STORM -> Icons.Filled.Thunderstorm
    WeatherKind.UNKNOWN -> Icons.Filled.CloudOff
}

@Composable
fun WeatherIcon(kind: WeatherKind, isDay: Boolean, size: Dp, description: String?, tint: Color? = null) {
    val color = tint ?: if (kind == WeatherKind.CLEAR && isDay) Sky.Sun else Color.White
    Icon(iconFor(kind, isDay), contentDescription = description, tint = color, modifier = Modifier.size(size))
}

@Composable
fun SkyCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) { content() }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

/** Loading placeholder; pulses unless the user has turned animations off. */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier) {
    val reduce = rememberReduceMotion()
    val alpha = if (reduce) 0.5f else {
        val t = rememberInfiniteTransition(label = "skeleton")
        t.animateFloat(0.35f, 0.7f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alpha").value
    }
    Box(
        modifier
            .alpha(alpha)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
    )
}

@Composable
fun HomeSkeleton() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonBox(Modifier.fillMaxWidth().height(260.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(110.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBox(Modifier.weight(1f).height(90.dp))
            SkeletonBox(Modifier.weight(1f).height(90.dp))
        }
        SkeletonBox(Modifier.fillMaxWidth().height(200.dp))
    }
}

@Composable
fun InfoBanner(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    SkyCard(modifier) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            if (action != null) { Spacer(Modifier.size(8.dp)); action() }
        }
    }
}

/**
 * Simple line chart drawn with Canvas (no third-party chart dependency). Missing values break the line.
 * [summary] is exposed to TalkBack because a chart has no other textual representation.
 */
@Composable
fun LineChart(
    values: List<Double?>,
    labels: List<String>,
    summary: String,
    modifier: Modifier = Modifier,
    lineColor: Color = Sky.Cyan,
) {
    val present = values.filterNotNull()
    if (present.size < 2) return
    val min = present.min(); val max = present.max()
    val range = (max - min).takeIf { it > 0.0 } ?: 1.0
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier.semantics { contentDescription = summary }) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val padV = 12.dp.toPx()
            val w = size.width; val h = size.height - 2 * padV
            val stepX = if (values.size > 1) w / (values.size - 1) else w
            var path = Path(); var started = false
            values.forEachIndexed { i, v ->
                if (v == null) { started = false; return@forEachIndexed }
                val x = i * stepX
                val y = padV + h - ((v - min) / range * h).toFloat()
                if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
            }
            drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            values.forEachIndexed { i, v ->
                if (v != null && (i == 0 || i == values.lastIndex || v == min || v == max)) {
                    val y = padV + h - ((v - min) / range * h).toFloat()
                    drawCircle(lineColor, 4.dp.toPx(), Offset(i * stepX, y))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { Text(it, style = MaterialTheme.typography.labelMedium, color = labelColor) }
        }
    }
}
