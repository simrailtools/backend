dependencies {
  // jackson
  implementation(libs.jacksonCore)

  // feign
  implementation(libs.feignCore)
  implementation(libs.feignClientJava11)
  implementation(libs.feignLoggingSlf4j)

  // nullability annotations
  compileOnly(libs.jspecify)
}
