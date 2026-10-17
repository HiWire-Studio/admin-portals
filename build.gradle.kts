allprojects {
  repositories {
    mavenCentral()

    maven("https://maven.hytale.com/release")
    maven("https://maven.hytale.com/pre-release")
  }
}

subprojects {

  apply(plugin = "java-library")

  extensions.configure<JavaPluginExtension> {
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(25))
    }
  }
}
