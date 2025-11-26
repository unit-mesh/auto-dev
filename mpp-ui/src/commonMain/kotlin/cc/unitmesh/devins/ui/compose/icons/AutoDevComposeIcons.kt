package cc.unitmesh.devins.ui.compose.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
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
    val Menu: ImageVector get() = Icons.Default.Menu
    val MenuOpen: ImageVector get() = Icons.Default.MenuOpen
    val Add: ImageVector get() = Icons.Default.Add
    val Close: ImageVector get() = Icons.Default.Close
    val ExpandMore: ImageVector get() = Icons.Default.ExpandMore
    val ExpandLess: ImageVector get() = Icons.Default.ExpandLess
    val ChevronRight: ImageVector get() = Icons.Default.ChevronRight
    val ChevronLeft: ImageVector get() = Icons.Default.ChevronLeft
    val KeyboardArrowUp: ImageVector get() = Icons.Default.KeyboardArrowUp
    val KeyboardArrowDown: ImageVector get() = Icons.Default.KeyboardArrowDown
    val AccountTree: ImageVector get() = Icons.Default.AccountTree
    val List: ImageVector get() = Icons.Default.List
    val CheckBox: ImageVector get() = Icons.Default.CheckBox
    val CheckBoxOutlineBlank: ImageVector get() = Icons.Default.CheckBoxOutlineBlank

    // Actions
    val Send: ImageVector get() = Icons.Default.Send
    val Settings: ImageVector get() = Icons.Default.Settings
    val Build: ImageVector get() = Icons.Default.Build
    val Tune: ImageVector get() = Icons.Default.Tune
    val Check: ImageVector get() = Icons.Default.Check
    val CheckCircle: ImageVector get() = Icons.Default.CheckCircle
    val ContentCopy: ImageVector get() = Icons.Default.ContentCopy
    val ContentPaste: ImageVector get() = Icons.Default.ContentPaste
    val Refresh: ImageVector get() = Icons.Default.Refresh
    val Sync: ImageVector get() = Icons.Default.Refresh // Use Refresh as Sync or import Sync if available
    val Stop: ImageVector get() = Icons.Default.Stop
    val PlayArrow: ImageVector get() = Icons.Default.PlayArrow
    val Clear: ImageVector get() = Icons.Default.Clear
    val Save: ImageVector get() = Icons.Default.Save
    val ArrowDropDown: ImageVector get() = Icons.Default.ArrowDropDown
    val History: ImageVector get() = Icons.Default.History
    val Edit: ImageVector get() = Icons.Default.Edit

    // Window Controls
    val Remove: ImageVector get() = Icons.Default.Remove  // Minimize button
    val Fullscreen: ImageVector get() = Icons.Default.Fullscreen  // Maximize/restore button
    val FullscreenExit: ImageVector get() = Icons.Default.FullscreenExit  // Exit fullscreen

    // Communication & AI
    val Chat: ImageVector get() = Icons.Default.Chat
    val SmartToy: ImageVector get() = Icons.Default.SmartToy
    val AlternateEmail: ImageVector get() = Icons.Default.AlternateEmail

    // Theme & Display
    val LightMode: ImageVector get() = Icons.Default.LightMode
    val DarkMode: ImageVector get() = Icons.Default.DarkMode
    val Brightness4: ImageVector get() = Icons.Default.Brightness4
    val AutoMode: ImageVector get() = Icons.Default.Brightness4 // Using Brightness4 as fallback for auto mode
    val Visibility: ImageVector get() = Icons.Default.Visibility
    val VisibilityOff: ImageVector get() = Icons.Default.VisibilityOff
    val MoreVert: ImageVector get() = Icons.Default.MoreVert

    // File & Code
    val InsertDriveFile: ImageVector get() = Icons.Default.InsertDriveFile
    val Description: ImageVector get() = Icons.Default.Description
    val Code: ImageVector get() = Icons.Default.Code
    val Javascript: ImageVector get() = Icons.Default.Javascript
    val Article: ImageVector get() = Icons.AutoMirrored.Filled.Article
    val RateReview: ImageVector get() = Icons.Default.RateReview

    // Status & Information
    val Error: ImageVector get() = Icons.Default.Error
    val Info: ImageVector get() = Icons.Default.Info
    val Schedule: ImageVector get() = Icons.Default.Schedule
    val BugReport: ImageVector get() = Icons.Outlined.BugReport

    // Cloud & Network (Note: CloudOff and CloudQueue need to be defined/imported separately if not in Default)
    val Cloud: ImageVector get() = Icons.Default.Cloud
    val CloudDownload: ImageVector get() = Icons.Default.CloudDownload
    val CloudOff: ImageVector get() = Icons.Default.Cloud // Fallback - CloudOff may not exist in Default
    val CloudQueue: ImageVector get() = Icons.Default.Cloud // Fallback - CloudQueue may not exist in Default
    val Computer: ImageVector get() = Icons.Default.Computer

    // Tools & Utilities
    val Search: ImageVector get() = Icons.Default.Search
    val Language: ImageVector get() = Icons.Default.Language
    val Help: ImageVector get() = Icons.AutoMirrored.Filled.Help
    val Delete: ImageVector get() = Icons.Default.Delete
    val ExitToApp: ImageVector get() = Icons.AutoMirrored.Filled.ExitToApp
    val RocketLaunch: ImageVector get() = Icons.Default.RocketLaunch
    val ArrowBack: ImageVector get() = Icons.Default.ArrowBack
    val Warning: ImageVector get() = Icons.Default.Warning
    val DriveFileRenameOutline: ImageVector get() = Icons.Default.Edit // Using Edit as fallback for rename
    val Analytics: ImageVector get() = Icons.Default.Analytics

    // Document Reader Icons
    val MenuBook: ImageVector get() = Icons.Default.MenuBook
    val Label: ImageVector get() = Icons.Default.Label
    val Api: ImageVector get() = Icons.Default.Api
    val DataObject: ImageVector get() = Icons.Default.DataObject
    val Functions: ImageVector get() = Icons.Default.Functions
    val KeyboardArrowRight: ImageVector get() = Icons.Default.KeyboardArrowRight

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
