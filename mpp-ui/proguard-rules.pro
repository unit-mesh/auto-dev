# Suppress warnings for Logback's optional dependencies (Servlet, Mail)
-dontwarn ch.qos.logback.**
-dontwarn jakarta.servlet.**
-dontwarn jakarta.mail.**

# Suppress warnings for JCEF's Thrift usage (we might not be using the remote features or they are bundled differently)
-dontwarn com.jetbrains.cef.remote.**
-dontwarn org.apache.thrift.**

# JOGL / GlueGen (Transitive dependencies, likely unused in Compose Desktop)
-dontwarn com.jogamp.**
-dontwarn jogamp.**

# Lettuce (Redis client - likely unused or optional)
-dontwarn io.lettuce.core.**

# Netty (Used by Ktor/Lettuce, but has many optional integrations)
-dontwarn io.netty.**

# OpenTelemetry
-dontwarn io.opentelemetry.**

# OkHttp (GraalVM/Conscrypt integrations)
-dontwarn okhttp3.internal.graal.**
-dontwarn okhttp3.internal.platform.**

# Apache Commons (Compress, Pool, etc.)
-dontwarn org.apache.commons.**

# Reactor
-dontwarn reactor.**

# AI Koog Agents (MCP SDK internal warnings)
-dontwarn ai.koog.agents.mcp.**

# Kotlin Logging (GraalVM support)
-dontwarn io.github.oshai.kotlinlogging.**

# OkHttp SSE & Internal
-dontwarn okhttp3.internal.**

# Apache HttpClient 5 & Brotli
-dontwarn org.apache.hc.**
-dontwarn org.brotli.**

# AWS SDK (Android support)
-dontwarn aws.sdk.kotlin.**
-dontwarn aws.smithy.kotlin.**

# Ktor (Android/Platform support)
-dontwarn io.ktor.**

# SLF4J
-dontwarn org.slf4j.**

# General platform/optional dependencies
-dontwarn android.**
-dontwarn sun.awt.**
-dontwarn sun.security.**
-dontwarn jdk.internal.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.graalvm.**
-dontwarn com.oracle.svm.**

# Log4j2 (Used by Netty)
-dontwarn org.apache.logging.log4j.**

# Fix IncompleteClassHierarchyException for Netty Logging
-keep class io.netty.util.internal.logging.** { *; }



