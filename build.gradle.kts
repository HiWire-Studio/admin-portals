import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.gradle.ext.ProjectSettings
import org.jetbrains.gradle.ext.runConfigurations

plugins {
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.idea.ext)
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")

    repositories {
        mavenCentral()
        maven {
            url = uri("https://nexus.hiwire.studio/repository/hiwire-group-internal")
            credentials {
                username = findProperty("hiwireMavenRepoUser") as String? ?: System.getenv("HIWIRE_MAVEN_REPO_USER")
                password =
                    findProperty("hiwireMavenRepoPassword") as String? ?: System.getenv("HIWIRE_MAVEN_REPO_PASSWORD")
            }
        }
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
