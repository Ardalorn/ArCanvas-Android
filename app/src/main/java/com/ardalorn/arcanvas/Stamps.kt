package com.ardalorn.arcanvas

import android.graphics.Path
import androidx.core.graphics.PathParser

/** The four stamp shapes, as SVG path data on a 100 x 100 grid (same shapes as the web version). */
object Stamps {
    class Def(val name: String, val pathData: String) {
        val path: Path by lazy { requireNotNull(PathParser.createPathFromPathData(pathData)) }
    }

    val all = listOf(
        Def(
            "star",
            "M50,6 Q56,20 58,34 Q75,24 90,30 Q70,42 64,50 Q73,72 78,92 Q60,76 50,68 Q40,80 16,88 Q30,64 34,48 Q14,42 4,32 Q26,34 40,32 Q46,18 50,6 Z",
        ),
        Def(
            "happy face",
            "M50,8 Q80,10 88,38 Q94,64 68,86 Q46,98 20,80 Q2,58 10,32 Q18,10 50,8 Z",
        ),
        Def(
            "paw",
            "M50,58 Q72,56 74,74 Q76,92 50,92 Q26,92 26,74 Q26,58 50,58 Z " +
                "M16,28 Q26,30 26,42 Q26,52 16,50 Q6,48 6,38 Q6,28 16,28 Z " +
                "M34,6 Q46,8 45,20 Q45,32 33,30 Q22,28 22,18 Q22,6 34,6 Z " +
                "M64,4 Q76,6 76,18 Q76,30 64,29 Q52,28 52,17 Q52,5 64,4 Z " +
                "M84,26 Q94,28 94,38 Q94,48 84,48 Q74,48 74,38 Q74,28 84,26 Z",
        ),
        Def(
            "cat",
            "M22,10 Q30,24 34,30 Q50,22 66,28 Q72,20 80,8 Q92,26 90,55 Q88,80 60,92 Q46,98 38,90 Q14,82 8,52 Q6,26 22,10 Z",
        ),
    )
}
