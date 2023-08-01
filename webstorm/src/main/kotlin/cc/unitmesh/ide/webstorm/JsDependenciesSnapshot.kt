package cc.unitmesh.ide.webstorm

import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.javascript.nodejs.PackageJsonData
import com.intellij.javascript.nodejs.packageJson.PackageJsonFileManager
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile

class JsDependenciesSnapshot(
    val packageJsonFiles: Set<VirtualFile>,
    val resolvedPackageJson: Boolean,
    val tsConfigs: Set<VirtualFile>,
    val packages: Map<String, PackageJsonData.PackageJsonDependencyEntry>
) {
    companion object {
        private const val TYPESCRIPT_PACKAGE = "typescript"
        val mostPopularPackages = setOf(
            "lodash",
            "request",
            "commander",
            "react",
            "express",
            "async",
            "moment",
            "prop-types",
            "react-dom",
            "bluebird",
            "underscore",
            "vue",
            "axios",
            "tslib",
            "glob",
            "yargs",
            "colors",
            "webpack",
            "uuid",
            "classnames",
            "minimist",
            "body-parser",
            "rxjs",
            "babel-runtime",
            "jquery",
            "babel-core",
            "core-js",
            "babel-loader",
            "cheerio",
            "rimraf",
            "eslint",
            "dotenv",
            TYPESCRIPT_PACKAGE,
            "@types/node",
            "@angular/core",
            "@angular/common",
            "redux",
            "gulp",
            "node-fetch",
            "@angular/platform-browser",
            "@babel/runtime",
            "handlebars",
            "@angular/compiler",
            "aws-sdk",
            "@angular/forms",
            "webpack-dev-server",
            "@angular/platform-browser-dynamic",
            "mocha",
            "socket.io",
            "ws",
            "node-sass",
            "@angular/router",
            "ramda",
            "react-redux",
            "@babel/core",
            "@angular/http",
            "ejs",
            "coffee-script",
            "mongodb",
            "chai",
            "mongoose",
            "xml2js",
            "bootstrap",
            "jest",
            "redis",
            "vue-router",
            "optimist",
            "promise",
            "@angular/animations",
            "postcss",
            "morgan",
            "less",
            "immutable"
        )

        fun create(
            project: Project,
            creationContext: ChatCreationContext
        ): JsDependenciesSnapshot {
            var packageJsonFiles = emptySet<VirtualFile>()
            var resolvedPackageJson = false
            val sourceFile = creationContext.sourceFile
            val virtualFile = sourceFile?.virtualFile
            if (virtualFile != null) {
                val packageJson = PackageJsonUtil.findUpPackageJson(virtualFile)
                if (packageJson != null) {
                    packageJsonFiles = setOf(packageJson)
                    resolvedPackageJson = true
                }
            }

            if (packageJsonFiles.isEmpty()) {
                packageJsonFiles = PackageJsonFileManager.getInstance(project).validPackageJsonFiles
            }

            val tsConfigs = findTsConfigs(project, packageJsonFiles)
            val packages = enumerateAllPackages(packageJsonFiles)
            return JsDependenciesSnapshot(packageJsonFiles, resolvedPackageJson, tsConfigs, packages)
        }

        private fun enumerateAllPackages(set: Set<VirtualFile>): Map<String, PackageJsonData.PackageJsonDependencyEntry> {
            return set.asSequence()
                .map { PackageJsonData.getOrCreate(it) }
                .flatMap { it.allDependencyEntries.entries }
                .associateBy({ it.key }, { it.value })
        }

        private fun findTsConfigs(project: Project, set: Set<VirtualFile>): Set<VirtualFile> {
            val mapNotNull = set.asSequence().mapNotNull { it ->
                it.parent?.findChild("tsconfig.json")
            }

            return mapNotNull.plus(project.guessProjectDir()?.findChild("tsconfig.json"))
                .filterNotNull().toSet()
        }
    }
}
