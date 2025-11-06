package cc.unitmesh.agent.tool.impl

/**
 * Result of URL parsing from a prompt
 */
data class ParsedUrls(
    val validUrls: List<String>,
    val errors: List<String>
)

/**
 * Utility object for URL parsing and validation
 */
object UrlParser {
    /**
     * Parses a prompt to extract valid URLs and identify malformed ones.
     */
    fun parsePrompt(text: String): ParsedUrls {
        val tokens = text.split(Regex("""\s+"""))
        val validUrls = mutableListOf<String>()
        val errors = mutableListOf<String>()

        for (token in tokens) {
            if (token.isEmpty()) continue

            // Heuristic to check if the token appears to contain URL-like chars
            if (token.contains("://")) {
                try {
                    // Clean up the token by removing trailing punctuation (but preserve URL-valid chars)
                    val cleanToken = token.trimEnd('.', ',', ')', ']', '}', '!', ';')

                    // Check protocol - only allow http and https
                    if (cleanToken.startsWith("http://") || cleanToken.startsWith("https://")) {
                        if (isValidHttpUrl(cleanToken)) {
                            val normalizedUrl = normalizeUrl(cleanToken)
                            validUrls.add(normalizedUrl)
                        } else {
                            errors.add("Malformed URL detected: \"$cleanToken\".")
                        }
                    } else {
                        // This looks like a URL with unsupported protocol
                        errors.add("Unsupported protocol in URL: \"$cleanToken\". Only http and https are supported.")
                    }
                } catch (e: Exception) {
                    errors.add("Malformed URL detected: \"$token\".")
                }
            } else if (token.contains(":") && !token.startsWith("http")) {
                // Handle protocols without ://
                errors.add("Unsupported protocol in URL: \"$token\". Only http and https are supported.")
            }
        }

        return ParsedUrls(validUrls, errors)
    }

    /**
     * Validates if a URL is a properly formed HTTP/HTTPS URL
     */
    private fun isValidHttpUrl(url: String): Boolean {
        try {
            // Basic structure validation
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return false
            }

            // Check for spaces in URL (not allowed in valid URLs)
            if (url.contains(" ")) {
                return false
            }

            // Check for minimum required parts: protocol + host
            val withoutProtocol = if (url.startsWith("https://")) {
                url.substring(8) // Remove "https://"
            } else {
                url.substring(7) // Remove "http://"
            }

            // Must have at least a host part
            if (withoutProtocol.isEmpty() || withoutProtocol.startsWith("/")) {
                return false
            }

            // Split into host and path parts
            val parts = withoutProtocol.split("/", limit = 2)
            val hostPart = parts[0]

            // Host cannot be empty
            if (hostPart.isEmpty()) {
                return false
            }

            // Check for invalid characters in host
            if (hostPart.contains("..") || hostPart.startsWith(".") || hostPart.endsWith(".")) {
                return false
            }

            // Check for IPv6 brackets (basic validation)
            if (hostPart.startsWith("[")) {
                if (!hostPart.contains("]")) {
                    return false
                }
            }

            // Check for valid port if present
            if (hostPart.contains(":")) {
                val hostAndPort = hostPart.split(":")
                if (hostAndPort.size != 2) return false

                val portStr = hostAndPort[1]
                if (portStr.isEmpty()) return false

                try {
                    val port = portStr.toInt()
                    if (port < 1 || port > 65535) return false
                } catch (e: NumberFormatException) {
                    return false
                }
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Normalize URL (basic validation and cleanup)
     */
    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()

        // Convert GitHub blob URLs to raw URLs
        if (normalized.contains("github.com") && normalized.contains("/blob/")) {
            normalized = normalized
                .replace("github.com", "raw.githubusercontent.com")
                .replace("/blob/", "/")
        }

        return normalized
    }
}