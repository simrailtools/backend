/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-present Pasqual Koschmieder and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import org.apache.tools.ant.filters.ReplaceTokens
import org.springframework.boot.gradle.plugin.SpringBootPlugin
import org.springframework.boot.gradle.tasks.run.BootRun
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
  alias(libs.plugins.spotless)
  alias(libs.plugins.springBoot) apply false
  alias(libs.plugins.springDependencyManagement) apply false
}

allprojects {
  group = "tools.simrail.backend"

  // generates a TimeVer version for release builds (determined by gradle property 'version.release')
  // TimeVer specification: https://gist.github.com/twolfson/de1b004dd22536b8e668
  // if it is not a release build just use 'dev-SNAPSHOT' as the version
  val releaseBuild = findProperty("version.release") ?: "false"
  if (releaseBuild == "true") {
    val timeVerFormatter = DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss.n").withZone(ZoneOffset.UTC)
    version = timeVerFormatter.format(Instant.now())
  } else {
    version = "dev-SNAPSHOT"
  }
}

subprojects {
  apply(plugin = "java")
  apply(plugin = "checkstyle")
  apply(plugin = "com.diffplug.spotless")
  apply(plugin = "io.spring.dependency-management")

  // nothing to boot in shared
  if (name in arrayOf("rest-api", "info-collector")) {
    apply(plugin = "org.springframework.boot")
  }

  dependencies {
    "compileOnly"(rootProject.libs.lombok)
    "annotationProcessor"(rootProject.libs.lombok)

    "implementation"(platform(SpringBootPlugin.BOM_COORDINATES))

    // testing
    "testImplementation"(rootProject.libs.mockito)
    "testRuntimeOnly"(rootProject.libs.junitLauncher)
    "testImplementation"(rootProject.libs.bundles.junit)
    "testImplementation"(rootProject.libs.bundles.testContainers)
  }

  tasks.withType<JavaCompile> {
    // Hint for the CodeQL autobuilder: sourceCompatibility = 25
    sourceCompatibility = JavaVersion.VERSION_25.toString()
    targetCompatibility = JavaVersion.VERSION_25.toString()

    options.encoding = "UTF-8"
    options.isIncremental = true

    options.compilerArgs.add("-proc:full")
    options.compilerArgs.add("-parameters")
    options.compilerArgs.add("--enable-preview")
    options.compilerArgs.addAll(
      listOf(
        "-Xlint:all",         // enable all warnings
        "-Xlint:-serial",     // reduce warning size for the following warning types
        "-Xlint:-preview",
        "-Xlint:-unchecked",
        "-Xlint:-classfile",
        "-Xlint:-processing",
        "-Xlint:-deprecation",
      )
    )
  }

  tasks.withType<ProcessResources> {
    val tokens = mapOf("project.version" to version)
    inputs.properties(tokens)
    filesMatching("*.yaml") {
      filter(ReplaceTokens::class, mapOf("tokens" to tokens))
    }
  }

  tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
      events("started", "passed", "skipped", "failed")
    }

    jvmArgs(
      "--enable-preview", // enable preview features in tests as well
      "-XX:+EnableDynamicAgentLoading", // allow mockito to dynamically attach to the jvm
    )

    // forces the re-run of tests everytime the task is executed
    outputs.upToDateWhen { false }
  }

  tasks.withType<BootRun> {
    jvmArgs = mutableListOf("--enable-preview")
  }

  tasks.withType<Checkstyle> {
    maxErrors = 0
    maxWarnings = 0
    configFile = rootProject.file("checkstyle.xml")
  }

  extensions.configure<CheckstyleExtension> {
    toolVersion = rootProject.libs.versions.checkstyleTools.get()
  }

  extensions.configure<SpotlessExtension> {
    java {
      lineEndings = LineEnding.UNIX
      encoding = StandardCharsets.UTF_8
      licenseHeaderFile(rootProject.file("license_header.txt"))
      targetExclude("**/backend/common/proto/**")
    }
  }

  configurations.all {
    exclude("org.checkerframework", "checker-qual")
  }
}
