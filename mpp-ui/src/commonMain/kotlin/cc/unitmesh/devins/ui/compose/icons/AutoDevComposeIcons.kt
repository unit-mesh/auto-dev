package cc.unitmesh.devins.ui.compose.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Centralized icon management for AutoDev Compose UI
 * All icons used across the application should be referenced through this object
 */
object AutoDevComposeIcons {
    // Navigation & Layout
    val FolderOpen: ImageVector get() = Icons.Default.FolderOpen
    val Folder: ImageVector get() = Icons.Default.Folder
    val Add: ImageVector get() = Icons.Default.Add
    val Close: ImageVector get() = Icons.Default.Close
    val ExpandMore: ImageVector get() = Icons.Default.ExpandMore
    val ExpandLess: ImageVector get() = Icons.Default.ExpandLess
    val ChevronRight: ImageVector get() = Icons.Default.ChevronRight
    val KeyboardArrowUp: ImageVector get() = Icons.Default.KeyboardArrowUp
    val KeyboardArrowDown: ImageVector get() = Icons.Default.KeyboardArrowDown
    
    // Actions
    val Send: ImageVector get() = Icons.Default.Send
    val Settings: ImageVector get() = Icons.Default.Settings
    val Build: ImageVector get() = Icons.Default.Build
    val Check: ImageVector get() = Icons.Default.Check
    val CheckCircle: ImageVector get() = Icons.Default.CheckCircle
    val ContentCopy: ImageVector get() = Icons.Default.ContentCopy
    val Refresh: ImageVector get() = Icons.Default.Refresh
    val Stop: ImageVector get() = Icons.Default.Stop
    val PlayArrow: ImageVector get() = Icons.Default.PlayArrow
    val Clear: ImageVector get() = Icons.Default.Clear
    val Save: ImageVector get() = Icons.Default.Save
    val ArrowDropDown: ImageVector get() = Icons.Default.ArrowDropDown
    
    // Communication & AI
    val Chat: ImageVector get() = Icons.Default.Chat
    val SmartToy: ImageVector get() = Icons.Default.SmartToy
    val AlternateEmail: ImageVector get() = Icons.Default.AlternateEmail
    
    // Theme & Display
    val LightMode: ImageVector get() = Icons.Default.LightMode
    val DarkMode: ImageVector get() = Icons.Default.DarkMode
    val Brightness4: ImageVector get() = Icons.Default.Brightness4
    val Visibility: ImageVector get() = Icons.Default.Visibility
    val VisibilityOff: ImageVector get() = Icons.Default.VisibilityOff
    val MoreVert: ImageVector get() = Icons.Default.MoreVert
    
    // File & Code
    val InsertDriveFile: ImageVector get() = Icons.Default.InsertDriveFile
    val Description: ImageVector get() = Icons.Default.Description
    val Code: ImageVector get() = Icons.Default.Code
    val Javascript: ImageVector get() = Icons.Default.Javascript
    val Article: ImageVector get() = Icons.Default.Article
    
    // Status & Information
    val Error: ImageVector get() = Icons.Default.Error
    val Info: ImageVector get() = Icons.Default.Info
    val Schedule: ImageVector get() = Icons.Default.Schedule
    val BugReport: ImageVector get() = Icons.Outlined.BugReport
    
    // Cloud & Network (Note: CloudOff and CloudQueue need to be defined/imported separately if not in Default)
    val Cloud: ImageVector get() = Icons.Default.Cloud
    val CloudOff: ImageVector get() = Icons.Default.Cloud // Fallback - CloudOff may not exist in Default
    val CloudQueue: ImageVector get() = Icons.Default.Cloud // Fallback - CloudQueue may not exist in Default
    
    // Tools & Utilities
    val Search: ImageVector get() = Icons.Default.Search
    val Language: ImageVector get() = Icons.Default.Language
    
    /**
     * Custom icons converted from SVG resources
     * These icons are converted from ai.svg and mcp.svg to Compose ImageVector format
     */
    object Custom {
        /**
         * AI icon - a sparkle/star representing AI functionality
         * Converted from resources/ai.svg
         */
        val AI: ImageVector get() = CustomIcons.AI
        
        /**
         * MCP (Model Context Protocol) icon - representing MCP integration
         * Converted from resources/mcp.svg
         */
        val MCP: ImageVector get() = CustomIcons.MCP
    }
}

