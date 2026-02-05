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

import de.undercouch.gradle.tasks.download.Download

plugins {
  alias(libs.plugins.downloadTask)
}

dependencies {
  implementation(projects.common)
  implementation(projects.externalApiClient)

  implementation(libs.nats)
  implementation(libs.redisson)
  implementation(libs.feignCore)
  implementation(libs.fuzzywuzzy)
  implementation(libs.protobufJava)
  implementation(libs.springDocOpenApi)
  implementation("org.postgresql:postgresql")
  implementation("com.github.ben-manes.caffeine:caffeine")
  implementation("io.micrometer:micrometer-registry-prometheus")

  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-validation")

  developmentOnly("org.springframework.boot:spring-boot-devtools")
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

// copy over downloaded rapidoc into resources/docs folder
tasks.withType<ProcessResources> {
  dependsOn("downloadRapiDoc")
  from(layout.buildDirectory.dir("download")) {
    into("resources/docs")
    include("rapidoc-min.js")
  }
}

// From StackOverflow: https://stackoverflow.com/a/53087407
tasks.register<Copy>("buildForDocker") {
  from(tasks.getByName("bootJar"))
  into(layout.buildDirectory.dir("libs/docker"))

  val projectVersion = version.toString()
  rename { fileName ->
    fileName.replace("-$projectVersion", "")
  }
}

// downloads rapidoc for displaying the rest api documentation
tasks.register<Download>("downloadRapiDoc") {
  overwrite(true)
  onlyIfModified(true)
  useETag("strongOnly")
  src("https://cdn.jsdelivr.net/npm/rapidoc/dist/rapidoc-min.js")
  dest(layout.buildDirectory.file("download/rapidoc-min.js"))
}
