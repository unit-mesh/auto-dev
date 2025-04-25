package cc.unitmesh.devti.gui.component

import com.intellij.ui.JBColor
import java.awt.*
import javax.swing.JPanel
import javax.swing.Timer


class LoadingSpinner : JPanel() {
    private var step = 0
    private var timer: Timer? = null
    
    init {
        isOpaque = false
        startAnimation()
    }
    
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
                
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        val width = width.toFloat()
        val height = height.toFloat()
        val centerX = width / 2
        val centerY = height / 2
        val radius = minOf(width, height) / 2 - 5
        
        val oldStroke = g2d.stroke
        g2d.stroke = BasicStroke(3f)
        
        val currentStep = if (step >= 12) 0 else step
        for (i in 0 until 12) {
            g2d.color = JBColor(
                Color(47, 99, 162, 255 - ((i + 12 - currentStep) % 12) * 20),
                Color(88, 157, 246, 255 - ((i + 12 - currentStep) % 12) * 20)
            )
            
            val startAngle = i * 30
            g2d.drawArc(
                (centerX - radius).toInt(), 
                (centerY - radius).toInt(),
                (radius * 2).toInt(), 
                (radius * 2).toInt(),
                startAngle, 
                15
            )
        }
        
        g2d.stroke = oldStroke
    }
    
    override fun getPreferredSize(): Dimension {
        return Dimension(40, 40)
    }

    fun startAnimation() {
        stopAnimation()
        
        step = 0
        timer = Timer(100, null) // 100ms interval for smooth animation
        timer!!.addActionListener {
            step = (step + 1) % 12
            repaint()
        }
        
        timer!!.start()
    }

    fun stopAnimation() {
        timer?.stop()
        timer = null
    }
    
    override fun removeNotify() {
        stopAnimation()
        super.removeNotify()
    }
}
