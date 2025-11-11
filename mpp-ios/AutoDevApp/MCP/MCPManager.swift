//
//  MCPManager.swift
//  AutoDevApp
//
//  MCP (Model Context Protocol) Manager for iOS
//  Provides a Swift-native interface to MCP servers
//

import Foundation
import MCP
import Combine

/// MCP Server Configuration
public struct MCPServerConfig: Codable {
    let url: String?
    let command: String?
    let args: [String]
    let timeout: Int
    let headers: [String: String]?
    
    public init(
        url: String? = nil,
        command: String? = nil,
        args: [String] = [],
        timeout: Int = 30000,
        headers: [String: String]? = nil
    ) {
        self.url = url
        self.command = command
        self.args = args
        self.timeout = timeout
        self.headers = headers
    }
}

/// MCP Configuration
public struct MCPConfig: Codable {
    let servers: [String: MCPServerConfig]
    
    public init(servers: [String: MCPServerConfig]) {
        self.servers = servers
    }
}

/// MCP Tool Information
public struct MCPToolInfo: Identifiable {
    public let id = UUID()
    public let name: String
    public let description: String?
    public let inputSchema: [String: Any]?
    
    init(from tool: Tool) {
        self.name = tool.name
        self.description = tool.description
        
        // Convert inputSchema to dictionary if possible
        if case .object(let schema) = tool.inputSchema {
            self.inputSchema = schema.mapValues { value -> Any in
                switch value {
                case .string(let str): return str
                case .number(let num): return num
                case .boolean(let bool): return bool
                case .array(let arr): return arr
                case .object(let obj): return obj
                case .null: return NSNull()
                }
            }
        } else {
            self.inputSchema = nil
        }
    }
}

/// MCP Manager - Main interface for MCP operations
@MainActor
public class MCPManager: ObservableObject {
    // MARK: - Published Properties
    
    @Published public private(set) var isConnected = false
    @Published public private(set) var availableTools: [String: [MCPToolInfo]] = [:]
    @Published public private(set) var lastError: String?
    @Published public private(set) var connectionStatus: String = "Disconnected"
    
    // MARK: - Private Properties
    
    private var clients: [String: Client] = [:]
    private var transports: [String: any Transport] = [:]
    private let config: MCPConfig
    
    // MARK: - Initialization
    
    public init(config: MCPConfig) {
        self.config = config
    }
    
    // MARK: - Connection Management
    
    /// Initialize and connect to all configured MCP servers
    public func connect() async throws {
        connectionStatus = "Connecting..."
        lastError = nil
        
        for (serverName, serverConfig) in config.servers {
            do {
                try await connectToServer(name: serverName, config: serverConfig)
            } catch {
                lastError = "Failed to connect to \(serverName): \(error.localizedDescription)"
                print("⚠️ \(lastError!)")
                // Continue connecting to other servers
            }
        }
        
        isConnected = !clients.isEmpty
        connectionStatus = isConnected ? "Connected to \(clients.count) server(s)" : "Connection failed"
    }
    
    /// Connect to a single MCP server
    private func connectToServer(name: String, config: MCPServerConfig) async throws {
        let client = Client(
            name: "AutoDev-iOS",
            version: "1.0.0"
        )
        
        let transport: any Transport
        
        if let urlString = config.url, let url = URL(string: urlString) {
            // HTTP Transport
            transport = HTTPClientTransport(url: url)
        } else if let command = config.command {
            #if targetEnvironment(simulator)
            // Stdio Transport (only works in simulator)
            transport = StdioTransport(
                command: command,
                arguments: config.args
            )
            #else
            throw MCPError.invalidParams("Stdio transport is not supported on real devices. Use HTTP transport instead.")
            #endif
        } else {
            throw MCPError.invalidParams("Server configuration must specify either 'url' or 'command'")
        }
        
        try await client.connect(transport: transport)
        
        clients[name] = client
        transports[name] = transport
        
        print("✅ Connected to MCP server: \(name)")
    }
    
    /// Disconnect from all servers
    public func disconnect() async {
        for (name, client) in clients {
            await client.disconnect()
            print("🔌 Disconnected from \(name)")
        }
        
        clients.removeAll()
        transports.removeAll()
        availableTools.removeAll()
        isConnected = false
        connectionStatus = "Disconnected"
    }
    
    // MARK: - Tool Discovery
    
    /// Discover tools from all connected servers
    public func discoverAllTools() async throws {
        guard !clients.isEmpty else {
            throw MCPError.invalidRequest("No servers connected")
        }
        
        var allTools: [String: [MCPToolInfo]] = [:]
        
        for (serverName, client) in clients {
            do {
                let (tools, _) = try await client.listTools()
                let toolInfos = tools.map { MCPToolInfo(from: $0) }
                allTools[serverName] = toolInfos
                print("📋 Discovered \(tools.count) tools from \(serverName)")
            } catch {
                print("⚠️ Failed to discover tools from \(serverName): \(error)")
                lastError = "Failed to discover tools from \(serverName)"
            }
        }
        
        availableTools = allTools
    }
    
    /// Discover tools from a specific server
    public func discoverServerTools(serverName: String) async throws -> [MCPToolInfo] {
        guard let client = clients[serverName] else {
            throw MCPError.invalidParams("Server '\(serverName)' not connected")
        }
        
        let (tools, _) = try await client.listTools()
        let toolInfos = tools.map { MCPToolInfo(from: $0) }
        
        availableTools[serverName] = toolInfos
        
        return toolInfos
    }
    
    // MARK: - Tool Execution
    
    /// Execute a tool on a specific server
    public func executeTool(
        serverName: String,
        toolName: String,
        arguments: [String: Any]
    ) async throws -> String {
        guard let client = clients[serverName] else {
            throw MCPError.invalidParams("Server '\(serverName)' not connected")
        }
        
        // Convert arguments to MCP Value type
        let mcpArguments = arguments.mapValues { value -> Value in
            if let str = value as? String {
                return .string(str)
            } else if let num = value as? Double {
                return .number(num)
            } else if let bool = value as? Bool {
                return .boolean(bool)
            } else if let arr = value as? [Any] {
                return .array(arr.map { _ in .null }) // Simplified
            } else if let dict = value as? [String: Any] {
                return .object(dict.mapValues { _ in .null }) // Simplified
            } else {
                return .null
            }
        }
        
        let result = try await client.callTool(
            name: toolName,
            arguments: mcpArguments
        )
        
        // Extract text content from result
        var output = ""
        for content in result.content {
            switch content {
            case .text(let text):
                output += text
            case .image(let data, let mimeType, _):
                output += "[Image: \(mimeType), \(data.count) bytes]\n"
            case .audio(let data, let mimeType):
                output += "[Audio: \(mimeType), \(data.count) bytes]\n"
            case .resource(let uri, let mimeType, let text):
                output += "[Resource: \(uri), \(mimeType)]\n"
                if let text = text {
                    output += text
                }
            }
        }
        
        print("🔧 Executed tool '\(toolName)' on \(serverName)")
        return output
    }
    
    // MARK: - Server Status
    
    /// Get status of all connected servers
    public func getServerStatuses() -> [String: String] {
        var statuses: [String: String] = [:]
        for (name, _) in clients {
            statuses[name] = "Connected"
        }
        return statuses
    }
    
    /// Check if a specific server is connected
    public func isServerConnected(_ serverName: String) -> Bool {
        return clients[serverName] != nil
    }
}

// MARK: - Convenience Extensions

extension MCPManager {
    /// Create a default configuration for testing with a local HTTP server
    public static func defaultConfig(port: Int = 3000) -> MCPConfig {
        return MCPConfig(servers: [
            "local": MCPServerConfig(
                url: "http://localhost:\(port)/mcp",
                timeout: 30000,
                headers: ["Content-Type": "application/json"]
            )
        ])
    }
    
    /// Create a configuration for filesystem server (simulator only)
    public static func filesystemConfig(path: String = "/tmp") -> MCPConfig {
        return MCPConfig(servers: [
            "filesystem": MCPServerConfig(
                command: "npx",
                args: ["-y", "@modelcontextprotocol/server-filesystem", path],
                timeout: 30000
            )
        ])
    }
}

