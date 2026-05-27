import com.diffplug.gradle.spotless.SpotlessExtension
import nl.littlerobots.vcu.plugin.resolver.VersionSelectors
import org.jetbrains.gradle.ext.ProjectSettings
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

idea {
    project {
        this as ExtensionAware
        configure<ProjectSettings> {
            runConfigurations {
                register<org.jetbrains.gradle.ext.Gradle>("HytaleServer") {
                    taskNames = listOf(":plugin:runServer")
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
