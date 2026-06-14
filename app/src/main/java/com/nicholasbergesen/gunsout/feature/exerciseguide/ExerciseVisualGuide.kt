package com.nicholasbergesen.gunsout.feature.exerciseguide

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.nicholasbergesen.gunsout.data.entity.MovementPattern
import com.nicholasbergesen.gunsout.data.entity.MuscleGroup
import com.nicholasbergesen.gunsout.ui.components.SectionLabel
import com.nicholasbergesen.gunsout.ui.components.StatusChip
import com.nicholasbergesen.gunsout.ui.components.WrappingRow

@Composable
fun ExerciseVisualGuide(
    muscleGroup: MuscleGroup,
    movementPattern: MovementPattern,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val spec = remember(muscleGroup, movementPattern) {
        exerciseGuideSpecFor(muscleGroup, movementPattern)
    }
    val transition = rememberInfiniteTransition(label = "exercise guide")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "exercise guide progress"
    )
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant
    val highlightColor = MaterialTheme.colorScheme.primary
    val motionColor = MaterialTheme.colorScheme.secondary
    val backdropColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel("Visual guide")
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 132.dp else 158.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(backdropColor)
                .padding(8.dp)
        ) {
            drawExerciseGuide(
                spec = spec,
                progress = progress,
                bodyColor = bodyColor,
                highlightColor = highlightColor,
                motionColor = motionColor
            )
        }
        WrappingRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatusChip("Target: ${spec.targetLabel}", selected = true)
            StatusChip(spec.movementLabel)
        }
        Text(
            text = spec.movementCue,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!compact) {
            Text(
                text = spec.targetCue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun DrawScope.drawExerciseGuide(
    spec: ExerciseGuideSpec,
    progress: Float,
    bodyColor: Color,
    highlightColor: Color,
    motionColor: Color
) {
    val scale = minOf(size.width / 190f, size.height / 155f)
    val top = (size.height - 145f * scale) / 2f
    val centerX = size.width / 2f
    val stroke = 6f * scale
    fun point(x: Float, y: Float) = Offset(centerX + x * scale, top + y * scale)

    drawRoundRect(
        color = bodyColor.copy(alpha = 0.08f),
        topLeft = Offset(centerX - 70f * scale, top + 6f * scale),
        size = Size(140f * scale, 132f * scale),
        cornerRadius = CornerRadius(24f * scale, 24f * scale)
    )

    drawHighlights(spec.highlightedRegions, highlightColor, scale, ::point)
    drawMotionTrace(spec.motion, progress, motionColor, scale, ::point)
    drawBody(spec.motion, progress, bodyColor, scale, stroke, ::point)
}

private fun DrawScope.drawHighlights(
    regions: Set<BodyMuscleRegion>,
    highlightColor: Color,
    scale: Float,
    point: (Float, Float) -> Offset
) {
    val fill = highlightColor.copy(alpha = 0.58f)
    val line = highlightColor.copy(alpha = 0.72f)
    fun oval(cx: Float, cy: Float, width: Float, height: Float) {
        val center = point(cx, cy)
        drawOval(
            color = fill,
            topLeft = Offset(center.x - width * scale / 2f, center.y - height * scale / 2f),
            size = Size(width * scale, height * scale)
        )
    }
    fun rounded(cx: Float, cy: Float, width: Float, height: Float) {
        val center = point(cx, cy)
        drawRoundRect(
            color = fill,
            topLeft = Offset(center.x - width * scale / 2f, center.y - height * scale / 2f),
            size = Size(width * scale, height * scale),
            cornerRadius = CornerRadius(8f * scale, 8f * scale)
        )
    }
    fun thickLine(startX: Float, startY: Float, endX: Float, endY: Float) {
        drawLine(
            color = line,
            start = point(startX, startY),
            end = point(endX, endY),
            strokeWidth = 11f * scale,
            cap = StrokeCap.Round
        )
    }

    if (BodyMuscleRegion.UPPER_BACK in regions) rounded(0f, 51f, 58f, 24f)
    if (BodyMuscleRegion.CHEST in regions) {
        oval(-9f, 55f, 23f, 20f)
        oval(9f, 55f, 23f, 20f)
    }
    if (BodyMuscleRegion.SHOULDERS in regions) {
        oval(-30f, 45f, 18f, 18f)
        oval(30f, 45f, 18f, 18f)
    }
    if (BodyMuscleRegion.BICEPS in regions) {
        thickLine(-28f, 58f, -35f, 78f)
        thickLine(28f, 58f, 35f, 78f)
    }
    if (BodyMuscleRegion.TRICEPS in regions) {
        thickLine(-21f, 58f, -29f, 80f)
        thickLine(21f, 58f, 29f, 80f)
    }
    if (BodyMuscleRegion.CORE in regions) rounded(0f, 71f, 31f, 31f)
    if (BodyMuscleRegion.GLUTES in regions) {
        oval(-9f, 91f, 22f, 17f)
        oval(9f, 91f, 22f, 17f)
    }
    if (BodyMuscleRegion.QUADS in regions) {
        thickLine(-11f, 94f, -18f, 120f)
        thickLine(11f, 94f, 18f, 120f)
    }
    if (BodyMuscleRegion.HAMSTRINGS in regions) {
        thickLine(-5f, 94f, -12f, 120f)
        thickLine(5f, 94f, 12f, 120f)
    }
    if (BodyMuscleRegion.CALVES in regions) {
        thickLine(-18f, 122f, -20f, 141f)
        thickLine(18f, 122f, 20f, 141f)
    }
}

private fun DrawScope.drawMotionTrace(
    motion: ExerciseGuideMotion,
    progress: Float,
    motionColor: Color,
    scale: Float,
    point: (Float, Float) -> Offset
) {
    val color = motionColor.copy(alpha = 0.55f)
    val strokeWidth = 3f * scale
    when (motion) {
        ExerciseGuideMotion.PUSH -> {
            drawLine(color, point(-26f, 61f), point(-58f, 57f), strokeWidth, StrokeCap.Round)
            drawLine(color, point(26f, 61f), point(58f, 57f), strokeWidth, StrokeCap.Round)
        }
        ExerciseGuideMotion.PULL -> {
            drawLine(color, point(-55f, 24f), point(-34f, 58f), strokeWidth, StrokeCap.Round)
            drawLine(color, point(55f, 24f), point(34f, 58f), strokeWidth, StrokeCap.Round)
        }
        ExerciseGuideMotion.SQUAT -> drawLine(color, point(0f, 89f), point(0f, 119f), strokeWidth, StrokeCap.Round)
        ExerciseGuideMotion.HINGE -> drawArcTrace(color, scale, point)
        ExerciseGuideMotion.LUNGE -> drawLine(color, point(-28f, 101f), point(-58f, 138f), strokeWidth, StrokeCap.Round)
        ExerciseGuideMotion.ISOLATION -> {
            drawLine(color, point(-35f, 103f), point(-29f, 76f), strokeWidth, StrokeCap.Round)
            drawLine(color, point(35f, 103f), point(29f, 76f), strokeWidth, StrokeCap.Round)
        }
        ExerciseGuideMotion.CALVES -> drawLine(color, point(0f, 118f), point(0f, 103f), strokeWidth, StrokeCap.Round)
        ExerciseGuideMotion.CORE -> drawLine(color, point(-39f, 118f), point(-12f, 90f), strokeWidth, StrokeCap.Round)
    }

    val pulse = 0.4f + progress * 0.4f
    drawCircle(
        color = motionColor.copy(alpha = pulse),
        radius = 5f * scale,
        center = movingMarker(motion, progress, point)
    )
}

private fun DrawScope.drawArcTrace(
    color: Color,
    scale: Float,
    point: (Float, Float) -> Offset
) {
    val topLeft = point(-8f, 42f)
    drawArc(
        color = color,
        startAngle = 90f,
        sweepAngle = -68f,
        useCenter = false,
        topLeft = topLeft,
        size = Size(54f * scale, 54f * scale),
        style = Stroke(width = 3f * scale, cap = StrokeCap.Round)
    )
}

private fun movingMarker(
    motion: ExerciseGuideMotion,
    progress: Float,
    point: (Float, Float) -> Offset
): Offset = when (motion) {
    ExerciseGuideMotion.PUSH -> point(38f + progress * 20f, 60f - progress * 3f)
    ExerciseGuideMotion.PULL -> point(50f - progress * 18f, 24f + progress * 34f)
    ExerciseGuideMotion.SQUAT -> point(0f, 91f + progress * 28f)
    ExerciseGuideMotion.HINGE -> point(5f + progress * 28f, 46f + progress * 14f)
    ExerciseGuideMotion.LUNGE -> point(-29f - progress * 25f, 101f + progress * 35f)
    ExerciseGuideMotion.ISOLATION -> point(34f - progress * 8f, 104f - progress * 30f)
    ExerciseGuideMotion.CALVES -> point(0f, 118f - progress * 15f)
    ExerciseGuideMotion.CORE -> point(-39f + progress * 27f, 118f - progress * 28f)
}

private fun DrawScope.drawBody(
    motion: ExerciseGuideMotion,
    progress: Float,
    bodyColor: Color,
    scale: Float,
    stroke: Float,
    point: (Float, Float) -> Offset
) {
    val lift = if (motion == ExerciseGuideMotion.CALVES) -progress * 8f else 0f
    val squatDrop = if (motion == ExerciseGuideMotion.SQUAT) progress * 18f else 0f
    val hingeLean = if (motion == ExerciseGuideMotion.HINGE) progress * 27f else 0f
    val coreKnee = if (motion == ExerciseGuideMotion.CORE) progress * 32f else 0f
    val hipY = 88f + lift + squatDrop
    val neck = point(hingeLean, 34f + lift + squatDrop * 0.25f)
    val head = point(hingeLean, 16f + lift + squatDrop * 0.25f)
    val leftShoulder = point(-27f + hingeLean, 45f + lift + squatDrop * 0.25f)
    val rightShoulder = point(27f + hingeLean, 45f + lift + squatDrop * 0.25f)
    val hip = point(0f, hipY)
    val leftHip = point(-13f, hipY)
    val rightHip = point(13f, hipY)

    drawCircle(bodyColor, radius = 9f * scale, center = head, style = Stroke(width = stroke * 0.65f))
    drawLine(bodyColor, neck, hip, stroke, StrokeCap.Round)
    drawLine(bodyColor, leftShoulder, rightShoulder, stroke, StrokeCap.Round)
    drawLine(bodyColor, leftHip, rightHip, stroke, StrokeCap.Round)

    val arms = armPointsFor(motion, progress, lift, squatDrop, hingeLean, point)
    drawLine(bodyColor, leftShoulder, arms.leftElbow, stroke, StrokeCap.Round)
    drawLine(bodyColor, arms.leftElbow, arms.leftHand, stroke, StrokeCap.Round)
    drawLine(bodyColor, rightShoulder, arms.rightElbow, stroke, StrokeCap.Round)
    drawLine(bodyColor, arms.rightElbow, arms.rightHand, stroke, StrokeCap.Round)

    val legs = legPointsFor(motion, progress, lift, coreKnee, point)
    drawLine(bodyColor, leftHip, legs.leftKnee, stroke, StrokeCap.Round)
    drawLine(bodyColor, legs.leftKnee, legs.leftFoot, stroke, StrokeCap.Round)
    drawLine(bodyColor, rightHip, legs.rightKnee, stroke, StrokeCap.Round)
    drawLine(bodyColor, legs.rightKnee, legs.rightFoot, stroke, StrokeCap.Round)
}

private data class ArmPoints(
    val leftElbow: Offset,
    val leftHand: Offset,
    val rightElbow: Offset,
    val rightHand: Offset
)

private fun armPointsFor(
    motion: ExerciseGuideMotion,
    progress: Float,
    lift: Float,
    squatDrop: Float,
    hingeLean: Float,
    point: (Float, Float) -> Offset
): ArmPoints = when (motion) {
    ExerciseGuideMotion.PUSH -> ArmPoints(
        leftElbow = point(-37f - progress * 8f, 60f + lift),
        leftHand = point(-34f - progress * 27f, 76f + lift),
        rightElbow = point(37f + progress * 8f, 60f + lift),
        rightHand = point(34f + progress * 27f, 76f + lift)
    )
    ExerciseGuideMotion.PULL -> ArmPoints(
        leftElbow = point(-40f + progress * 7f, 44f + progress * 15f + lift),
        leftHand = point(-55f + progress * 25f, 25f + progress * 39f + lift),
        rightElbow = point(40f - progress * 7f, 44f + progress * 15f + lift),
        rightHand = point(55f - progress * 25f, 25f + progress * 39f + lift)
    )
    ExerciseGuideMotion.ISOLATION -> ArmPoints(
        leftElbow = point(-36f, 74f + lift),
        leftHand = point(-39f + progress * 10f, 106f - progress * 31f + lift),
        rightElbow = point(36f, 74f + lift),
        rightHand = point(39f - progress * 10f, 106f - progress * 31f + lift)
    )
    ExerciseGuideMotion.HINGE -> ArmPoints(
        leftElbow = point(-32f + hingeLean, 75f),
        leftHand = point(-27f + hingeLean, 105f),
        rightElbow = point(32f + hingeLean, 75f),
        rightHand = point(27f + hingeLean, 105f)
    )
    else -> ArmPoints(
        leftElbow = point(-35f, 68f + lift + squatDrop * 0.25f),
        leftHand = point(-27f, 92f + lift + squatDrop * 0.25f),
        rightElbow = point(35f, 68f + lift + squatDrop * 0.25f),
        rightHand = point(27f, 92f + lift + squatDrop * 0.25f)
    )
}

private data class LegPoints(
    val leftKnee: Offset,
    val leftFoot: Offset,
    val rightKnee: Offset,
    val rightFoot: Offset
)

private fun legPointsFor(
    motion: ExerciseGuideMotion,
    progress: Float,
    lift: Float,
    coreKnee: Float,
    point: (Float, Float) -> Offset
): LegPoints = when (motion) {
    ExerciseGuideMotion.SQUAT -> LegPoints(
        leftKnee = point(-21f - progress * 12f, 113f + progress * 8f),
        leftFoot = point(-30f, 142f),
        rightKnee = point(21f + progress * 12f, 113f + progress * 8f),
        rightFoot = point(30f, 142f)
    )
    ExerciseGuideMotion.LUNGE -> LegPoints(
        leftKnee = point(-34f - progress * 7f, 111f + progress * 12f),
        leftFoot = point(-58f, 142f),
        rightKnee = point(24f + progress * 8f, 114f + progress * 13f),
        rightFoot = point(56f, 142f)
    )
    ExerciseGuideMotion.CORE -> LegPoints(
        leftKnee = point(-22f, 118f - coreKnee),
        leftFoot = point(-35f, 142f - coreKnee),
        rightKnee = point(20f, 120f),
        rightFoot = point(28f, 142f)
    )
    else -> LegPoints(
        leftKnee = point(-17f, 118f + lift),
        leftFoot = point(-23f, 142f),
        rightKnee = point(17f, 118f + lift),
        rightFoot = point(23f, 142f)
    )
}
