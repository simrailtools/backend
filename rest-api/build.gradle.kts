/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-2025 Pasqual Koschmieder and contributors
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

import com.google.protobuf.gradle.id

plugins {
  alias(libs.plugins.protobuf)
  alias(libs.plugins.downloadTask)
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.grpc:spring-grpc-dependencies:${libs.versions.springGrpc.get()}")
  }
}

dependencies {
  // include protobuf files from <project root>/.proto folder
  protobuf(rootProject.files(".proto"))

  implementation(projects.common)
  implementation(projects.externalApiClient)

  implementation(libs.feignCore)
  implementation(libs.fuzzywuzzy)
  implementation(libs.springDocOpenApi)
  implementation("org.postgresql:postgresql")
  implementation("com.github.ben-manes.caffeine:caffeine")

  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-websocket")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.grpc:spring-grpc-client-spring-boot-starter")

  developmentOnly("org.springframework.boot:spring-boot-devtools")
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:${dependencyManagement.importedProperties["protobuf-java.version"]}"
  }
  plugins {
    id("grpc") {
      artifact = "io.grpc:protoc-gen-grpc-java:${dependencyManagement.importedProperties["grpc.version"]}"
    }
  }
  generateProtoTasks {
    ofSourceSet("main").forEach {
      it.plugins {
        id("grpc") {
          option("jakarta_omit")
          option("@generated=omit")
        }
      }
    }
  }
}

// downloads rapidoc for displaying the rest api documentation
tasks.withType<ProcessResources> {
  doLast {
    download.run {
      overwrite(true)
      onlyIfModified(true)
      useETag("strongOnly")
      src("https://cdn.jsdelivr.net/npm/rapidoc/dist/rapidoc-min.js")
      dest(layout.buildDirectory.dir("resources/main/resources/docs/"))
    }
  }
}

// From StackOverflow: https://stackoverflow.com/a/53087407
tasks.register<Copy>("buildForDocker") {
  from(tasks.getByName("bootJar"))
  into("build/libs/docker")
  rename { fileName ->
    fileName.replace("-$version", "")
  }
}
