import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:_")

    implementation(compose.desktop.currentOs)

    implementation("commons-codec:commons-codec:_")
    implementation("io.ktor:ktor-client-cio:_")
    implementation("io.ktor:ktor-client-serialization:_")
    implementation("com.auth0:java-jwt:_")

    // HTTP Server
    implementation("io.ktor:ktor-server-core:_")
    implementation("io.ktor:ktor-server-netty:_")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

compose.desktop {
    application {
        mainClass = "com.seanproctor.auth.app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "AuthCompose"
            packageVersion = "1.0.0"
        }
    }
}