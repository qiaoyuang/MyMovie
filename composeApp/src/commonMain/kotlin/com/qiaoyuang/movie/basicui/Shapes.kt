package com.qiaoyuang.movie.basicui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val rattingStar: ImageVector by lazy {
    ImageVector.Builder(
        name = "rattingStar",
        defaultWidth = 16.dp,
        defaultHeight = 16.dp,
        viewportWidth = 16f,
        viewportHeight = 16f
    ).apply {
        path(
            fill = null,
            fillAlpha = 1.0f,
            stroke = SolidColor(Color(0xFFFFFFFF)),
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(8.47592f, 2.29389f)
            lineTo(10.0199f, 5.39989f)
            curveTo(10.09730f, 5.55650f, 10.24660f, 5.66590f, 10.41990f, 5.69120f)
            lineTo(13.8766f, 6.19055f)
            curveTo(14.01660f, 6.20920f, 14.14260f, 6.28250f, 14.22860f, 6.39450f)
            curveTo(14.38990f, 6.60450f, 14.36530f, 6.90190f, 14.17190f, 7.08260f)
            lineTo(11.6666f, 9.50522f)
            curveTo(11.53930f, 9.62520f, 11.48260f, 9.80120f, 11.51590f, 9.97260f)
            lineTo(12.1159f, 13.3912f)
            curveTo(12.15790f, 13.67460f, 11.96460f, 13.93990f, 11.68130f, 13.98590f)
            curveTo(11.56390f, 14.00390f, 11.44390f, 13.98520f, 11.33730f, 13.93260f)
            lineTo(8.25859f, 12.3186f)
            curveTo(8.10390f, 12.23460f, 7.91860f, 12.23460f, 7.76390f, 12.31860f)
            lineTo(4.66259f, 13.9412f)
            curveTo(4.40330f, 14.07320f, 4.08590f, 13.97520f, 3.94460f, 13.72120f)
            curveTo(3.89060f, 13.61860f, 3.87190f, 13.50190f, 3.89060f, 13.38790f)
            lineTo(4.49059f, 9.96922f)
            curveTo(4.52060f, 9.79860f, 4.46390f, 9.62320f, 4.33990f, 9.50250f)
            lineTo(1.82126f, 7.08055f)
            curveTo(1.61590f, 6.87650f, 1.61460f, 6.54460f, 1.81930f, 6.33920f)
            curveTo(1.81990f, 6.33850f, 1.82060f, 6.33720f, 1.82130f, 6.33650f)
            curveTo(1.90590f, 6.25990f, 2.00990f, 6.20850f, 2.12260f, 6.18860f)
            lineTo(5.57992f, 5.68922f)
            curveTo(5.75260f, 5.66190f, 5.90130f, 5.55390f, 5.97990f, 5.39720f)
            lineTo(7.52259f, 2.29389f)
            curveTo(7.58460f, 2.16790f, 7.69460f, 2.07120f, 7.82790f, 2.02720f)
            curveTo(7.96190f, 1.98260f, 8.10860f, 1.99320f, 8.23460f, 2.05660f)
            curveTo(8.33790f, 2.10790f, 8.42260f, 2.19120f, 8.47590f, 2.29390f)
            close()
        }
    }.build()
}

val rightArrow: ImageVector by lazy {
    ImageVector.Builder(
        name = "rightArrow",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f
    ).apply {
        path(
            fill = null,
            fillAlpha = 1.0f,
            stroke = SolidColor(Color(0xFF9C2CF3)),
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(16.4585f, 9.77132f)
            horizontalLineTo(3.9585f)
        }
        path(
            fill = null,
            fillAlpha = 1.0f,
            stroke = SolidColor(Color(0xFF9C2CF3)),
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(11.417f, 4.75092f)
            lineTo(16.4587f, 9.77092f)
            lineTo(11.417f, 14.7917f)
        }
    }.build()
}

val filter: ImageVector by lazy {
    ImageVector.Builder(
        name = "Filter",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color(0xFF9C2CF3)),
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(13f, 12f)
            horizontalLineTo(4f)
        }
        path(
            stroke = SolidColor(Color(0xFF9C2CF3)),
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(20f, 12f)
            curveTo(20f, 13.105f, 19.105f, 14f, 18f, 14f)
            curveTo(16.895f, 14f, 16f, 13.105f, 16f, 12f)
            curveTo(16f, 10.895f, 16.895f, 10f, 18f, 10f)
            curveTo(19.105f, 10f, 20f, 10.895f, 20f, 12f)
            close()
        }
        path(
            stroke = SolidColor(Color(0xFF9C2CF3)),
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(11f, 6f)
            horizontalLineTo(20f)
        }
        path(
            stroke = SolidColor(Color(0xFF9C2CF3)),
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(4f, 6f)
            curveTo(4f, 7.105f, 4.895f, 8f, 6f, 8f)
            curveTo(7.105f, 8f, 8f, 7.105f, 8f, 6f)
            curveTo(8f, 4.895f, 7.105f, 4f, 6f, 4f)
            curveTo(4.895f, 4f, 4f, 4.895f, 4f, 6f)
            close()
        }
        path(
            stroke = SolidColor(Color(0xFF9C2CF3)),
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(11f, 18f)
            horizontalLineTo(20f)
        }
        path(
            stroke = SolidColor(Color(0xFF9C2CF3)),
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(4f, 18f)
            curveTo(4f, 19.105f, 4.895f, 20f, 6f, 20f)
            curveTo(7.105f, 20f, 8f, 19.105f, 8f, 18f)
            curveTo(8f, 16.895f, 7.105f, 16f, 6f, 16f)
            curveTo(4.895f, 16f, 4f, 16.895f, 4f, 18f)
            close()
        }
    }.build()
}

internal val search: ImageVector by lazy {
    ImageVector.Builder(
        name = "Search",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color(0xFF9C2CF3)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(11.767f, 11.767f)
            moveToRelative(-8.989f, 0f)
            arcToRelative(8.989f, 8.989f, 0f, isMoreThanHalf = true, isPositiveArc = true, 17.977f, 0f)
            arcToRelative(8.989f, 8.989f, 0f, isMoreThanHalf = true, isPositiveArc = true, -17.977f, 0f)
        }
        path(
            stroke = SolidColor(Color(0xFF9C2CF3)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(18.018f, 18.485f)
            lineTo(21.542f, 22f)
        }
    }.build()
}