package cc.unitmesh.devins.ui.compose.terminal

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicScrollBarUI

/**
 * Color configuration for a terminal scrollbar.
 */
data class TerminalScrollbarColors(
    val track: Color,
    val thumb: Color,
    val thumbHover: Color? = null,
    val thumbPressed: Color? = null
)

/**
 * A modern, minimal scrollbar inspired by IntelliJ's JBScrollBar styling.
 * - Ultra-thin design (6px width)
 * - Rounded thumb with transparency
 * - Hover & press feedback
 * - Auto-hide track (only thumb visible)
 *
 * Note: This class is open to allow anonymous subclass creation like IDEA's JBScrollBar.
 */
open class ModernTerminalScrollBar(
    orientation: Int,
    private val colors: TerminalScrollbarColors?
) : JScrollBar(orientation) {
    init {
        isOpaque = false
        putClientProperty("JComponent.sizeVariant", "mini")
        unitIncrement = 4
        blockIncrement = 48
        colors?.let { background = it.track }
    }

    override fun updateUI() {
        setUI(
            object : BasicScrollBarUI() {
                private var hovering = false
                private var pressing = false

                override fun configureScrollBarColors() {
                    colors?.let {
                        thumbColor = it.thumb
                        trackColor = it.track
                    }
                }

                // Ultra-thin scrollbar (6px width)
                override fun getMaximumThumbSize(): Dimension = Dimension(6, 6)

                override fun getMinimumThumbSize(): Dimension = Dimension(6, 6)

                override fun paintTrack(g: Graphics, c: JComponent, trackBounds: Rectangle) {
                    // Don't paint track - auto-hide for cleaner look
                    // Only thumb will be visible
                }

                override fun paintThumb(g: Graphics, c: JComponent, thumbBounds: Rectangle) {
                    if (thumbBounds.isEmpty) return
                    val thumbColors = colors ?: return super.paintThumb(g, c, thumbBounds)

                    val g2 = g as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                    // Calculate thumb color with transparency
                    val baseColor =
                        when {
                            pressing && thumbColors.thumbPressed != null -> thumbColors.thumbPressed
                            hovering && thumbColors.thumbHover != null -> thumbColors.thumbHover
                            else -> thumbColors.thumb
                        }

                    // Apply transparency: more opaque on hover/press
                    val alpha =
                        when {
                            pressing -> 200
                            hovering -> 160
                            else -> 100
                        }

                    g2.color = Color(baseColor.red, baseColor.green, baseColor.blue, alpha)

                    // Draw ultra-thin rounded thumb (4px width with 1px margin on each side)
                    val arc = 4
                    val margin = 1
                    val thumbWidth = 4
                    val thumbX = thumbBounds.x + (thumbBounds.width - thumbWidth) / 2

                    g2.fillRoundRect(
                        thumbX,
                        thumbBounds.y + margin,
                        thumbWidth,
                        thumbBounds.height - margin * 2,
                        arc,
                        arc
                    )
                }

                override fun createTrackListener(): TrackListener {
                    val tl = super.createTrackListener()
                    return object : TrackListener() {
                        override fun mouseEntered(e: java.awt.event.MouseEvent?) {
                            hovering = true
                            scrollbar.repaint()
                            tl.mouseEntered(e)
                        }

                        override fun mouseExited(e: java.awt.event.MouseEvent?) {
                            hovering = false
                            pressing = false
                            scrollbar.repaint()
                            tl.mouseExited(e)
                        }

                        override fun mousePressed(e: java.awt.event.MouseEvent?) {
                            pressing = true
                            scrollbar.repaint()
                            tl.mousePressed(e)
                        }

                        override fun mouseReleased(e: java.awt.event.MouseEvent?) {
                            pressing = false
                            scrollbar.repaint()
                            tl.mouseReleased(e)
                        }
                    }
                }
            }
        )
    }
}
