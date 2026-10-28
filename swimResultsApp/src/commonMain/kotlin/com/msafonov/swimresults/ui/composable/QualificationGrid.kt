package com.msafonov.swimresults.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msafonov.swimresults.data.model.TimeComponents
import com.msafonov.swimresults.ui.theme.Theme
import com.msafonov.swimresults.viewmodel.QualificationStandard
import org.jetbrains.compose.resources.stringResource
import swimresults.swimresultsapp.generated.resources.Res
import swimresults.swimresultsapp.generated.resources.standardMissing

@Composable
fun QualificationGrid(
    standards: List<QualificationStandard>,
    modifier: Modifier = Modifier
) {
    val minimumCardWidth = 104.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    val missing = stringResource(Res.string.standardMissing)
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            standards.forEach { standard ->
                val time = standard.time?.let {
                    TimeComponents.durationToComponents(it).toRankFormat()
                }
                Column(
                    modifier = Modifier.background(Theme.colors.backGround2, Theme.shapes.medium)
                        .clearAndSetSemantics {
                            contentDescription = "${standard.rank.text}, ${time ?: missing}"
                        }.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(standard.rank.text, color = Theme.colors.primary, fontWeight = FontWeight.Medium)
                    Text(time ?: "—", color = Theme.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { measurables, constraints ->
        val gap = 8.dp.roundToPx()
        val minimum = minimumCardWidth.roundToPx().coerceAtLeast(1)
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else {
            constraints.constrainWidth(minimum)
        }
        val columns = ((width + gap) / (minimum + gap)).coerceIn(1, 3)
        val cellWidth = ((width - gap * (columns - 1)) / columns).coerceAtLeast(0)
        val children = measurables.map { it.measure(Constraints.fixedWidth(cellWidth)) }
        val rows = children.chunked(columns)
        val heights = rows.map { row -> row.maxOf { it.height } }
        val height = heights.sum() + gap * (rows.size - 1).coerceAtLeast(0)
        layout(width, constraints.constrainHeight(height)) {
            var y = 0
            rows.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { column, child ->
                    child.placeRelative(column * (cellWidth + gap), y)
                }
                y += heights[rowIndex] + gap
            }
        }
    }
}
