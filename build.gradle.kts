import com.diffplug.gradle.spotless.SpotlessExtension
import nl.littlerobots.vcu.plugin.resolver.VersionSelectors
import org.jetbrains.gradle.ext.ProjectSettings
import org.jetbrains.gradle.ext.delegateActions
import org.jetbrains.gradle.ext.runConfigurations

plugins {
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.idea.ext)
    alias(libs.plugins.version.catalog.update)
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")

    repositories {
        mavenCentral()
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    configure<SpotlessExtension> {
        format("misc") {
            target("*.gradle", ".gitattributes", ".gitignore")
            trimTrailingWhitespace()
            leadingSpacesToTabs()
            endWithNewline()
        }
        java {
            googleJavaFormat(rootProject.libs.versions.google.java.format.get()).reflowLongStrings().skipJavadocFormatting()
            formatAnnotations()
        }
    }
}

// The IDE run configuration reads serverArgs exported by hytale-server.gradle.kts.
evaluationDependsOn(":mod")

idea {
    project {
        this as ExtensionAware
        configure<ProjectSettings> {
            // Build and run with IntelliJ itself. When delegated to Gradle, IntelliJ turns the
            // Application config into a JavaExec task, which drops the compileOnly server jar
            // from the classpath and reports Stop/Ctrl+C as a failed build.
            delegateActions {
                delegateBuildRunToGradle = false
            }
            runConfigurations {
                // Plain Application run configuration: stopping it is a normal process exit,
                // unlike a Gradle-backed run where Stop/Ctrl+C is reported as a failed build.
                register<org.jetbrains.gradle.ext.Application>("HytaleServer") {
                    val mod = project(":mod")
                    mainClass = "com.hypixel.hytale.Main"
                    moduleName = "${rootProject.name}.${mod.name}.main"
                    // Puts the compileOnly HytaleServer.jar on the runtime classpath.
                    includeProvidedDependencies = true
                    workingDirectory = file("run").absolutePath
                    @Suppress("UNCHECKED_CAST")
                    programParameters = (mod.extra["serverArgs"] as List<String>).joinToString(" ")
                    beforeRun {
                        register<org.jetbrains.gradle.ext.GradleTask>("processResources") {
                            task = mod.tasks.getByName("processResources")
                        }
                    }
                }
            }
        }
    }
}

versionCatalogUpdate {
  versionSelector(VersionSelectors.STABLE)
  sortByKey = false

  keep {
    keepUnusedVersions = true
  }
}
