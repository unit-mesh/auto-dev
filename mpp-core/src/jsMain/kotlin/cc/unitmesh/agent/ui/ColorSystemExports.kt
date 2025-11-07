@file:JsExport

package cc.unitmesh.agent.ui

import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * JS exports for color system
 */

@JsName("ColorSystem")
object JsColorSystem {
    // Indigo Scale
    @JsName("indigoScale")
    val indigoScale = object {
        val c50 = IndigoScale.c50
        val c100 = IndigoScale.c100
        val c200 = IndigoScale.c200
        val c300 = IndigoScale.c300
        val c400 = IndigoScale.c400
        val c500 = IndigoScale.c500
        val c600 = IndigoScale.c600
        val c700 = IndigoScale.c700
        val c800 = IndigoScale.c800
        val c900 = IndigoScale.c900
    }
    
    // Cyan Scale
    @JsName("cyanScale")
    val cyanScale = object {
        val c50 = CyanScale.c50
        val c100 = CyanScale.c100
        val c200 = CyanScale.c200
        val c300 = CyanScale.c300
        val c400 = CyanScale.c400
        val c500 = CyanScale.c500
        val c600 = CyanScale.c600
        val c700 = CyanScale.c700
        val c800 = CyanScale.c800
        val c900 = CyanScale.c900
    }
    
    // Neutral Scale
    @JsName("neutralScale")
    val neutralScale = object {
        val c50 = NeutralScale.c50
        val c100 = NeutralScale.c100
        val c200 = NeutralScale.c200
        val c300 = NeutralScale.c300
        val c400 = NeutralScale.c400
        val c500 = NeutralScale.c500
        val c600 = NeutralScale.c600
        val c700 = NeutralScale.c700
        val c800 = NeutralScale.c800
        val c900 = NeutralScale.c900
    }
    
    // Green Scale
    @JsName("greenScale")
    val greenScale = object {
        val c50 = GreenScale.c50
        val c100 = GreenScale.c100
        val c200 = GreenScale.c200
        val c300 = GreenScale.c300
        val c400 = GreenScale.c400
        val c500 = GreenScale.c500
        val c600 = GreenScale.c600
        val c700 = GreenScale.c700
        val c800 = GreenScale.c800
        val c900 = GreenScale.c900
    }
    
    // Amber Scale
    @JsName("amberScale")
    val amberScale = object {
        val c50 = AmberScale.c50
        val c100 = AmberScale.c100
        val c200 = AmberScale.c200
        val c300 = AmberScale.c300
        val c400 = AmberScale.c400
        val c500 = AmberScale.c500
        val c600 = AmberScale.c600
        val c700 = AmberScale.c700
        val c800 = AmberScale.c800
        val c900 = AmberScale.c900
    }
    
    // Red Scale
    @JsName("redScale")
    val redScale = object {
        val c50 = RedScale.c50
        val c100 = RedScale.c100
        val c200 = RedScale.c200
        val c300 = RedScale.c300
        val c400 = RedScale.c400
        val c500 = RedScale.c500
        val c600 = RedScale.c600
        val c700 = RedScale.c700
        val c800 = RedScale.c800
        val c900 = RedScale.c900
    }
    
    // Blue Scale
    @JsName("blueScale")
    val blueScale = object {
        val c50 = BlueScale.c50
        val c100 = BlueScale.c100
        val c200 = BlueScale.c200
        val c300 = BlueScale.c300
        val c400 = BlueScale.c400
        val c500 = BlueScale.c500
        val c600 = BlueScale.c600
        val c700 = BlueScale.c700
        val c800 = BlueScale.c800
        val c900 = BlueScale.c900
    }
    
    // Light Theme
    @JsName("lightTheme")
    val lightTheme = LightTheme.colors
    
    // Dark Theme
    @JsName("darkTheme")
    val darkTheme = DarkTheme.colors
}

