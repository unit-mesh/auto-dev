import Foundation
import MCP

/// Swift bridge for MCP Client functionality
/// This class provides an Objective-C compatible interface for Kotlin/Native to use
@objc public class McpClientBridge: NSObject {
    
    // MARK: - Properties
    
    private var clients: [String: Client] = [:]
    private var serverStatuses: [String: String] = [:]
    private var discoveryState: String = "NOT_STARTED"
    
    // MARK: - Initialization
    
    @objc public override init() {
        super.init()
    }
    
    // MARK: - Configuration
    
    /// Initialize the MCP client manager with configuration
    /// - Parameter configJson: JSON string containing MCP server configurations
    @objc public func initialize(configJson: String) async throws {
        // Configuration is stored and used when discovering tools
        // No immediate action needed here
    }
    
    // MARK: - Tool Discovery
    
    /// Discover all tools from all configured MCP servers
    /// - Parameter configJson: JSON string containing MCP server configurations
    /// - Returns: JSON string containing discovered tools grouped by server
    @objc public func discoverAllTools(configJson: String) async throws -> String {
        discoveryState = "IN_PROGRESS"
        
        guard let configData = configJson.data(using: .utf8),
              let config = try? JSONSerialization.jsonObject(with: configData) as? [String: Any],
              let mcpServers = config["mcpServers"] as? [String: [String: Any]] else {
            discoveryState = "COMPLETED"
            return "{}"
        }
        
        var result: [String: [[String: Any]]] = [:]
        
        for (serverName, serverConfig) in mcpServers {
            // Skip disabled servers
            if let disabled = serverConfig["disabled"] as? Bool, disabled {
                continue
            }
            
            do {
                serverStatuses[serverName] = "CONNECTING"
                let tools = try await connectAndDiscoverTools(serverName: serverName, serverConfig: serverConfig)
                result[serverName] = tools
                serverStatuses[serverName] = "CONNECTED"
            } catch {
                print("Error connecting to MCP server '\(serverName)': \(error.localizedDescription)")
                serverStatuses[serverName] = "DISCONNECTED"
            }
        }
        
        discoveryState = "COMPLETED"
        
        // Convert result to JSON
        let jsonData = try JSONSerialization.data(withJSONObject: result, options: .prettyPrinted)
        return String(data: jsonData, encoding: .utf8) ?? "{}"
    }
    
    /// Discover tools from a specific MCP server
    /// - Parameters:
    ///   - serverName: Name of the MCP server
    ///   - serverConfigJson: JSON string containing server configuration
    /// - Returns: JSON string containing discovered tools
    @objc public func discoverServerTools(serverName: String, serverConfigJson: String) async throws -> String {
        guard let configData = serverConfigJson.data(using: .utf8),
              let serverConfig = try? JSONSerialization.jsonObject(with: configData) as? [String: Any] else {
            return "[]"
        }
        
        // Skip disabled servers
        if let disabled = serverConfig["disabled"] as? Bool, disabled {
            return "[]"
        }
        
        do {
            let tools = try await connectAndDiscoverTools(serverName: serverName, serverConfig: serverConfig)
            let jsonData = try JSONSerialization.data(withJSONObject: tools, options: .prettyPrinted)
            return String(data: jsonData, encoding: .utf8) ?? "[]"
        } catch {
            print("Error discovering tools for server '\(serverName)': \(error.localizedDescription)")
            return "[]"
        }
    }
    
    // MARK: - Tool Execution
    
    /// Execute a tool on a specific MCP server
    /// - Parameters:
    ///   - serverName: Name of the MCP server
    ///   - toolName: Name of the tool to execute
    ///   - arguments: JSON string containing tool arguments
    /// - Returns: JSON string containing tool execution result
    @objc public func executeTool(serverName: String, toolName: String, arguments: String) async throws -> String {
        guard let client = clients[serverName] else {
            throw NSError(
                domain: "McpClientBridge",
                code: 1,
                userInfo: [NSLocalizedDescriptionKey: "No client found for server '\(serverName)'"]
            )
        }
        
        // Parse arguments
        guard let argsData = arguments.data(using: .utf8),
              let argsDict = try? JSONSerialization.jsonObject(with: argsData) as? [String: Any] else {
            throw NSError(
                domain: "McpClientBridge",
                code: 2,
                userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"]
            )
        }
        
        // Call tool
        let (content, isError) = try await client.callTool(name: toolName, arguments: argsDict)
        
        // Convert result to JSON
        var resultDict: [String: Any] = [
            "isError": isError,
            "content": []
        ]
        
        var contentArray: [[String: Any]] = []
        for item in content {
            switch item {
            case .text(let text):
                contentArray.append(["type": "text", "text": text])
            case .image(let data, let mimeType, let metadata):
                var imageDict: [String: Any] = [
                    "type": "image",
                    "mimeType": mimeType,
                    "data": data.base64EncodedString()
                ]
                if let metadata = metadata {
                    imageDict["metadata"] = metadata
                }
                contentArray.append(imageDict)
            case .resource(let uri, let mimeType, let text):
                var resourceDict: [String: Any] = [
                    "type": "resource",
                    "uri": uri,
                    "mimeType": mimeType ?? ""
                ]
                if let text = text {
                    resourceDict["text"] = text
                }
                contentArray.append(resourceDict)
            default:
                break
            }
        }
        
        resultDict["content"] = contentArray
        
        let jsonData = try JSONSerialization.data(withJSONObject: resultDict, options: .prettyPrinted)
        return String(data: jsonData, encoding: .utf8) ?? "{}"
    }
    
    // MARK: - Server Status
    
    /// Get the status of a specific MCP server
    /// - Parameter serverName: Name of the MCP server
    /// - Returns: Status string (DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING)
    @objc public func getServerStatus(serverName: String) -> String {
        return serverStatuses[serverName] ?? "DISCONNECTED"
    }
    
    /// Get all server statuses
    /// - Returns: JSON string containing all server statuses
    @objc public func getAllServerStatuses() -> String {
        do {
            let jsonData = try JSONSerialization.data(withJSONObject: serverStatuses, options: .prettyPrinted)
            return String(data: jsonData, encoding: .utf8) ?? "{}"
        } catch {
            return "{}"
        }
    }
    
    /// Get discovery state
    /// - Returns: Discovery state string (NOT_STARTED, IN_PROGRESS, COMPLETED)
    @objc public func getDiscoveryState() -> String {
        return discoveryState
    }
    
    // MARK: - Cleanup
    
    /// Shutdown all MCP clients and clean up resources
    @objc public func shutdown() async {
        for (serverName, client) in clients {
            serverStatuses[serverName] = "DISCONNECTING"
            // Close client connection
            // Note: Swift MCP SDK may not have explicit close method
            serverStatuses[serverName] = "DISCONNECTED"
        }
        clients.removeAll()
    }
    
    // MARK: - Private Methods
    
    private func connectAndDiscoverTools(serverName: String, serverConfig: [String: Any]) async throws -> [[String: Any]] {
        // Create client
        let client = Client(name: serverName, version: "1.0.0")
        
        // Determine transport type
        if let command = serverConfig["command"] as? String {
            // Stdio transport
            let args = serverConfig["args"] as? [String] ?? []
            let transport = try createStdioTransport(command: command, args: args, serverConfig: serverConfig)
            try await client.connect(transport: transport)
        } else if let url = serverConfig["url"] as? String {
            // HTTP transport
            guard let serverUrl = URL(string: url) else {
                throw NSError(
                    domain: "McpClientBridge",
                    code: 3,
                    userInfo: [NSLocalizedDescriptionKey: "Invalid server URL: \(url)"]
                )
            }
            let transport = HTTPClientTransport(url: serverUrl)
            try await client.connect(transport: transport)
        } else {
            throw NSError(
                domain: "McpClientBridge",
                code: 4,
                userInfo: [NSLocalizedDescriptionKey: "Server configuration must have either 'command' or 'url'"]
            )
        }
        
        // Store client
        clients[serverName] = client
        
        // List tools
        let (tools, _) = try await client.listTools()
        
        // Convert tools to dictionary format
        return tools.map { tool in
            var toolDict: [String: Any] = [
                "name": tool.name,
                "description": tool.description ?? "",
                "serverName": serverName,
                "enabled": false
            ]
            
            // Convert input schema to JSON string
            if let inputSchema = tool.inputSchema {
                if let schemaData = try? JSONSerialization.data(withJSONObject: inputSchema, options: []),
                   let schemaString = String(data: schemaData, encoding: .utf8) {
                    toolDict["inputSchema"] = schemaString
                }
            }
            
            return toolDict
        }
    }
    
    private func createStdioTransport(command: String, args: [String], serverConfig: [String: Any]) throws -> StdioTransport {
        // Note: StdioTransport in Swift MCP SDK may have different initialization
        // This is a placeholder - actual implementation depends on SDK API
        
        // WARNING: iOS has strict limitations on process spawning
        // This will likely only work in simulator, not on real devices
        
        #if targetEnvironment(simulator)
        // Simulator can spawn processes
        return StdioTransport(command: command, arguments: args)
        #else
        // Real device - stdio transport not supported
        throw NSError(
            domain: "McpClientBridge",
            code: 5,
            userInfo: [NSLocalizedDescriptionKey: "Stdio transport is not supported on iOS devices. Use HTTP transport instead."]
        )
        #endif
    }
}
