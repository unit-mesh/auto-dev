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
 * - Rounded thumb
 * - Themed colors
 * - Hover & press feedback
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

                override fun getMaximumThumbSize(): Dimension = Dimension(16, 16)

                override fun getMinimumThumbSize(): Dimension = Dimension(16, 16)

                override fun paintTrack(g: Graphics, c: JComponent, trackBounds: Rectangle) {
                    val g2 = g as Graphics2D
                    g2.color = colors?.track ?: trackColor
                    g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height)
                }

                override fun paintThumb(g: Graphics, c: JComponent, thumbBounds: Rectangle) {
                    if (thumbBounds.isEmpty) return
                    val thumbColors = colors ?: return super.paintThumb(g, c, thumbBounds)

                    val g2 = g as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    val baseColor =
                        when {
                            pressing && thumbColors.thumbPressed != null -> thumbColors.thumbPressed
                            hovering && thumbColors.thumbHover != null -> thumbColors.thumbHover
                            else -> thumbColors.thumb
                        }
                    g2.color = baseColor
                    val arc = 8
                    g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, arc, arc)
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
