// Read properties from gradle.properties
val pluginGroup: String by project
val pluginVersion: String by project
val platformType: String by project
val platformVersion: String by project
val platformPlugins: String by project
val pluginSinceBuild: String by project
val pluginUntilBuild: String by project

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.2.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = pluginGroup
version = pluginVersion

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create(platformType, platformVersion)
        bundledPlugins(platformPlugins.split(',').map { it.trim() })

        // Required for testing
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Plugin Verifier
        pluginVerifier()
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    patchPluginXml {
        sinceBuild.set(pluginSinceBuild)
        // Don't set untilBuild to support all future versions
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

intellijPlatform {
    pluginVerification {
        ides {
            recommended()
        }
    }
}
