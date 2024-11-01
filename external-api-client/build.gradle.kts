dependencies {
  // jackson
  implementation(libs.jacksonCore)
  implementation(libs.jacksonDatatypeJdk8)
  implementation(libs.jacksonDatatypeJsr310)

  // feign
  implementation(libs.feignCore)
  implementation(libs.feignJackson)
  implementation(libs.feignClientJava11)
  implementation(libs.feignLoggingSlf4j)

  // jetbrains annotations
  compileOnly(libs.annotations)
}
