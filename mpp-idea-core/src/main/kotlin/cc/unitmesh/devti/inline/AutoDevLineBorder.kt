package cc.unitmesh.devti.inline

import java.awt.*
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.border.LineBorder

class AutoDevLineBorder(color: Color, thickness: Int, roundedCorners: Boolean, val radius: Int) :
    LineBorder(color, thickness, roundedCorners) {

    override fun paintBorder(component: Component?, graphics: Graphics?, x: Int, y: Int, width: Int, height: Int) {
        if (thickness > 0 && graphics is Graphics2D) {
            val oldColor = graphics.color
            graphics.color = lineColor
            val offs = thickness
            val size = offs + offs
            val outer: Shape
            val inner: Shape
            if (roundedCorners) {
                val arc: Float = (radius * 2).toFloat()
                outer = RoundRectangle2D.Float(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), arc, arc)
                inner = RoundRectangle2D.Float(
                    (x + offs).toFloat(), (y + offs).toFloat(), (width - size).toFloat(),
                    (height - size).toFloat(), arc, arc
                )
            } else {
                outer = Rectangle2D.Float(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
                inner = Rectangle2D.Float(
                    (x + offs).toFloat(),
                    (y + offs).toFloat(),
                    (width - size).toFloat(),
                    (height - size).toFloat()
                )
            }

            val shape = Path2D.Float(0)
            shape.append(outer, false)
            shape.append(inner, false)
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.fill(shape)
            graphics.color = oldColor
        }
    }
}