/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024 Pasqual Koschmieder and contributors
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
import org.springframework.boot.gradle.plugin.SpringBootPlugin
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
  alias(libs.plugins.spotless)
  alias(libs.plugins.springBoot) apply false
  alias(libs.plugins.springDependencyManagement) apply false
}

allprojects {
  version = "1.0.0-SNAPSHOT"
  group = "tools.simrail.backend"
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

  repositories {
    mavenCentral()
    maven {
      name = "Spring Milestones"
      url = uri("https://repo.spring.io/milestone")
    }
    maven {
      name = "Spring Snapshots"
      url = uri("https://repo.spring.io/snapshot")
    }
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
    sourceCompatibility = JavaVersion.VERSION_23.toString()
    targetCompatibility = JavaVersion.VERSION_23.toString()

    options.encoding = "UTF-8"
    options.isIncremental = true

    options.compilerArgs.add("-proc:full")
    options.compilerArgs.add("-parameters")
    options.compilerArgs.add("-Xlint:-preview")
    options.compilerArgs.add("--enable-preview")
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
      licenseHeaderFile(rootProject.file("license_header.txt"))
    }
  }

  configurations.all {
    exclude("org.checkerframework", "checker-qual")
  }
}
