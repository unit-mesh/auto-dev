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
        val validUrls = mutableListOf<String>()
        val errors = mutableListOf<String>()

        // Use broader regex to find all potential URLs (any protocol)
        // This matches both "protocol://" and "protocol:" patterns
        val urlPattern = Regex("""[a-zA-Z][a-zA-Z0-9+.-]*:(?://)?[^\s]+""")
        val matches = urlPattern.findAll(text)

        for (match in matches) {
            val potentialUrl = match.value
            try {
                // Clean up the URL (remove trailing punctuation that might not be part of URL)
                val cleanUrl = potentialUrl.trimEnd('.', ',', ')', ']', '}', '!', '?', ';', ':')

                // Check for URLs with spaces by looking at the context around the match
                val matchStart = match.range.first
                val matchEnd = match.range.last + 1

                // Look for a pattern like "protocol://domain/path with spaces" where our regex only matched up to the space
                // We do this by checking if there are non-whitespace characters immediately after our match that could be part of a URL
                var extendedUrl = cleanUrl
                if (matchEnd < text.length) {
                    val remainingText = text.substring(matchEnd)
                    // Check if the remaining text starts with a space followed by URL-like characters (not just any text)
                    // We want to catch cases like "https://example.com/path with spaces" but not "https://example.com 获取更多信息"
                    val spacePattern = Regex("""^(\s+[a-zA-Z0-9._~:/?#[\]@!$&'()*+,;=-]+)+""")
                    val spaceMatch = spacePattern.find(remainingText)
                    if (spaceMatch != null && spaceMatch.value.contains(" ")) {
                        // This looks like a URL with spaces in it (not just followed by other text)
                        extendedUrl = cleanUrl + spaceMatch.value
                        errors.add("Malformed URL detected: \"$extendedUrl\".")
                        continue
                    }
                }

                // Check protocol first
                if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                    // Validate the URL structure
                    if (isValidHttpUrl(cleanUrl)) {
                        val url = normalizeUrl(cleanUrl)
                        validUrls.add(url)
                    } else {
                        errors.add("Malformed URL detected: \"$cleanUrl\".")
                    }
                } else {
                    // Extract protocol for error message
                    val protocolEnd = cleanUrl.indexOf("://")
                    val protocol = if (protocolEnd > 0) cleanUrl.substring(0, protocolEnd + 3) else "unknown"
                    errors.add("Unsupported protocol in URL: \"$cleanUrl\". Only http and https are supported.")
                }
            } catch (e: Exception) {
                errors.add("Malformed URL detected: \"$potentialUrl\".")
            }
        }

        // Fallback: if no URLs found with regex, try the old token-based approach
        if (validUrls.isEmpty() && errors.isEmpty()) {
            val tokens = text.split(Regex("\\s+"))
            for (token in tokens) {
                if (token.isBlank()) continue

                // Heuristic to check if the token appears to contain URL-like chars
                if (token.contains("://")) {
                    try {
                        // Clean up the token
                        val cleanToken = token.trimEnd('.', ',', ')', ']', '}', '!', '?', ';', ':')

                        // Check protocol
                        if (cleanToken.startsWith("http://") || cleanToken.startsWith("https://")) {
                            if (isValidHttpUrl(cleanToken)) {
                                val url = normalizeUrl(cleanToken)
                                validUrls.add(url)
                            } else {
                                errors.add("Malformed URL detected: \"$cleanToken\".")
                            }
                        } else {
                            errors.add("Unsupported protocol in URL: \"$cleanToken\". Only http and https are supported.")
                        }
                    } catch (e: Exception) {
                        errors.add("Malformed URL detected: \"$token\".")
                    }
                }
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

            // Basic host validation
            if (hostPart.contains("..") || hostPart.startsWith(".") || hostPart.endsWith(".")) {
                return false
            }

            // Check for invalid characters in host
            if (hostPart.contains(" ")) {
                return false
            }

            // Check for malformed IPv6 addresses (must be properly bracketed)
            if (hostPart.contains("[") || hostPart.contains("]")) {
                // IPv6 addresses must be fully enclosed in brackets
                if (!hostPart.startsWith("[") || !hostPart.endsWith("]")) {
                    return false
                }
                // Basic check for IPv6 format (simplified)
                val ipv6Content = hostPart.substring(1, hostPart.length - 1)
                if (ipv6Content.isEmpty() || ipv6Content.contains("[") || ipv6Content.contains("]")) {
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