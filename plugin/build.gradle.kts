
// Read Hytale settings from gradle.properties and pass to hytale-server.gradle.kts
extra["hytalePatchline"] = project.findProperty("plugin.hytalePatchline") ?: "release"
extra["hytaleIncludesAssetPack"] = (project.findProperty("plugin.hytaleIncludesAssetPack") as String?)?.toBoolean() ?: true
extra["hytaleLoadUserMods"] = (project.findProperty("plugin.hytaleLoadUserMods") as String?)?.toBoolean() ?: false
project.findProperty("plugin.hytaleHome")?.let { extra["hytaleHome"] = it }

apply(from = "../hytale-server.gradle.kts")

// Retrieve serverJar location from hytale-server.gradle.kts
val serverJar: FileCollection by extra

dependencies {
  compileOnly(serverJar)

  compileOnly(libs.lombok)
  annotationProcessor(libs.lombok)

  // Testing
  testImplementation(serverJar)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.mockito.core)
  testImplementation(libs.mockito.junit.jupiter)
  testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.jar {
  archiveBaseName.set("hiwire-adminportals-plugin")
}

tasks.test {
  useJUnitPlatform()
  systemProperty("java.util.logging.manager", "com.hypixel.hytale.logger.backend.HytaleLogManager")
  jvmArgs("-XX:+EnableDynamicAgentLoading")
  testLogging {
    showStandardStreams = true
    events("passed", "skipped", "failed")
  }
}

