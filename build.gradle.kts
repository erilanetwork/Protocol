import org.gradle.kotlin.dsl.compileJava
import org.gradle.kotlin.dsl.invoke

@Suppress("DSL_SCOPE_VIOLATION") // https://youtrack.jetbrains.com/issue/IDEA-262280

plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
    alias(libs.plugins.lombok)
}

tasks.jar {
    enabled = false
}

subprojects {

    apply {
        plugin("java-library")
        plugin("maven-publish")
        plugin("signing")
        plugin(rootProject.libs.plugins.lombok.get().pluginId)
    }

    group = "com.erilanetwork.protocol"

    tasks {
        compileJava {
            options.encoding = Charsets.UTF_8.name();
            options.compilerArgs.add("-parameters")
        }
        test {
            useJUnitPlatform()
        }
    }

    dependencies {
        compileOnly(rootProject.libs.checker.qual)
    }

    java {
        withJavadocJar()
        withSourcesJar()
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }

        repositories {
            maven {
                name = "erila"
                val repoType = if (version.toString().endsWith("SNAPSHOT")) "snapshots" else "releases"
                url = uri("http://api.erilanetwork.com:8080/$repoType")
                isAllowInsecureProtocol = true
                credentials {
                    username = providers.gradleProperty("erilaUsername")
                        .orElse(providers.environmentVariable("ERILA_REPO_USERNAME"))
                        .orNull
                    password = providers.gradleProperty("erilaPassword")
                        .orElse(providers.environmentVariable("ERILA_REPO_PASSWORD"))
                        .orNull
                }
            }
        }
    }

    signing {
        if (System.getenv("PGP_SECRET") != null && System.getenv("PGP_PASSPHRASE") != null) {
            useInMemoryPgpKeys(System.getenv("PGP_SECRET"), System.getenv("PGP_PASSPHRASE"))
            sign(publishing.publications["maven"])
        }
    }
}
