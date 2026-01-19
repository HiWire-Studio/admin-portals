import org.gradle.internal.os.OperatingSystem
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

// Read configuration from extra properties (with defaults)
val patchline: String = extra.properties["hytalePatchline"] as String? ?: "release"
val includesAssetPack: Boolean = extra.properties["hytaleIncludesAssetPack"] as Boolean? ?: true
val loadUserMods: Boolean = extra.properties["hytaleLoadUserMods"] as Boolean? ?: false

// Resolve Hytale installation directory
val hytaleHome: String = if (extra.has("hytaleHome")) {
  extra["hytaleHome"] as String
} else if (project.hasProperty("hytaleHome")) {
  project.findProperty("hytaleHome") as String
} else {
  val os = OperatingSystem.current()
  val userHome = System.getProperty("user.home")
  when {
    os.isWindows -> "$userHome/AppData/Roaming/Hytale"
    os.isMacOsX -> "$userHome/Library/Application Support/Hytale"
    os.isLinux -> {
      val flatpakPath = "$userHome/.var/app/com.hypixel.HytaleLauncher/data/Hytale"
      if (file(flatpakPath).exists()) flatpakPath else "$userHome/.local/share/Hytale"
    }
    else -> throw GradleException(
      "Your Hytale install could not be detected automatically. " +
      "If you are on an unsupported platform or using a custom install location, " +
      "please set extra[\"hytaleHome\"] or use the -PhytaleHome property."
    )
  }
}

if (!file(hytaleHome).exists()) {
  throw GradleException(
    "Failed to find Hytale at the expected location. " +
    "Please make sure you have installed the game. " +
    "The expected location can be changed with extra[\"hytaleHome\"] or -PhytaleHome. " +
    "Currently looking in $hytaleHome"
  )
}

// Export serverJar for use in dependencies
val serverJar = files("$hytaleHome/install/$patchline/package/game/latest/Server/HytaleServer.jar")
extra["serverJar"] = serverJar

// Updates the manifest.json file with the latest properties.
// Currently we update the version and if packs are included with the plugin.
tasks.register("updatePluginManifest") {
  val manifestFile = file("src/main/resources/manifest.json")
  val projectVersion = project.version.toString()

  inputs.property("version", projectVersion)
  inputs.property("includesAssetPack", includesAssetPack)
  outputs.file(manifestFile)

  doLast {
    if (!manifestFile.exists()) {
      throw GradleException("Could not find manifest.json at ${manifestFile.path}!")
    }
    @Suppress("UNCHECKED_CAST")
    val manifestJson = JsonSlurper().parseText(manifestFile.readText()) as MutableMap<String, Any>
    manifestJson["Version"] = projectVersion
    manifestJson["IncludesAssetPack"] = includesAssetPack
    manifestFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(manifestJson)))
  }
}

// Makes sure the plugin manifest is up to date.
tasks.named("processResources") {
  dependsOn("updatePluginManifest")
}

tasks.register<JavaExec>("runServer") {
  group = "application"
  mainClass.set("com.hypixel.hytale.Main")
  classpath = project.the<SourceSetContainer>()["main"].runtimeClasspath + serverJar
  workingDir = file("${rootProject.projectDir}/run")
  standardInput = System.`in`

  doFirst {
    workingDir.mkdirs()
  }

  val modPaths = mutableListOf<String>()
  if (includesAssetPack) {
    modPaths += file("src").absolutePath
  }
  if (loadUserMods) {
    modPaths += "$hytaleHome/UserData/Mods"
  }

  args = buildList {
    add("--allow-op")
    add("--disable-sentry")
    add("--assets=$hytaleHome/install/$patchline/package/game/latest/Assets.zip")
    if (modPaths.isNotEmpty()) {
      add("--mods=${modPaths.joinToString(",")}")
    }
  }
}
