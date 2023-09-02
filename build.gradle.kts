
plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "com.friendly_machines.intellij.plugins"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
////        languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_17.toString()))
////        //vendor.set(determineJvmVendor(defaultFallbackSpec = JvmVendorSpec.ADOPTOPENJDK))
          languageVersion.set(JavaLanguageVersion.of(17))
          vendor.set(JvmVendorSpec.matching("JetBrains s.r.o"))
          // languageVersion = JavaLanguageVersion.of(17)
          // vendor = JvmVendorSpec.JETBRAINS
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.1") // ("2022.2.2") // 222.4167.29
    type.set("IC") // Target IDE Platform

    plugins.set(listOf("org.rust.lang:0.4.191.5293-231"))
}

dependencies {
    // https://mvnrepository.com/artifact/net.java.dev.jna/jna
    implementation("net.java.dev.jna:jna:5.12.1")
    implementation("net.java.dev.jna:jna-platform:5.12.1")
    implementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("231.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    runIde {
        autoReloadPlugins.set(true)
    }
    runIdeForUiTests {
    }
    runIdePerformanceTest {
    }
    buildSearchableOptions {
    }
}
