import java.util.Properties

plugins {
    alias(libsSubmodule.plugins.kotlin.multiplatform)
    alias(libsSubmodule.plugins.jetbrains.compose)
    alias(libsSubmodule.plugins.android.library)
    id("maven-publish")
    id("signing")
}

group = "moe.tlaster"
version = rootProject.extra.get("precomposeVersion") as String

kotlin {
    applyDefaultHierarchyTemplate()
    macosArm64()
    macosX64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    androidTarget {
        publishLibraryVariants("release", "debug")
    }
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = rootProject.extra.get("jvmTarget") as String
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(IR) {
        browser()
    }
    wasmJs {
        browser()
        binaries.executable()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                compileOnly(compose.foundation)
                compileOnly(compose.animation)
                compileOnly(compose.material)
                api(libsSubmodule.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                api(compose.foundation)
                api(compose.animation)
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libsSubmodule.kotlinx.coroutines.test)
                // @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                // implementation(compose.uiTestJUnit4)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libsSubmodule.foundation)
                implementation(libsSubmodule.animation)
                implementation(libsSubmodule.androidx.material)
                api(libsSubmodule.androidx.activity.ktx)
                api(libsSubmodule.androidx.appcompat)
                implementation(libsSubmodule.androidx.lifecycle.runtime.ktx)
                api(libsSubmodule.androidx.savedstate.ktx)
                implementation(libsSubmodule.androidx.lifecycle.viewmodel.compose)
                implementation(libsSubmodule.androidx.activity.compose)
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(libsSubmodule.junit)
            }
        }
        val macosMain by getting {
            dependencies {
                implementation(compose.foundation)
                implementation(compose.animation)
                implementation(compose.material)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.foundation)
                implementation(compose.animation)
                implementation(compose.material)
                api(libsSubmodule.kotlinx.coroutines.swing)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libsSubmodule.junit.jupiter.api)
                runtimeOnly(libsSubmodule.junit.jupiter.engine)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(compose.foundation)
                implementation(compose.animation)
                implementation(compose.material)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        val iosMain by getting {
            dependencies {
                implementation(compose.foundation)
                implementation(compose.animation)
                implementation(compose.material)
            }
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(compose.foundation)
                implementation(compose.animation)
                implementation(compose.material)
            }
        }
    }
}
// adding it here to make sure skiko is unpacked and available in web tests
compose.experimental {
    web.application {}
}
android {
    compileSdk = rootProject.extra.get("android-compile") as Int
    buildToolsVersion = rootProject.extra.get("android-build-tools") as String
    namespace = "moe.tlaster.precompose"
    defaultConfig {
        minSdk = rootProject.extra.get("androidMinSdk") as Int
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(rootProject.extra.get("jvmTarget") as String)
        targetCompatibility = JavaVersion.toVersion(rootProject.extra.get("jvmTarget") as String)
    }
}
extra.apply {
    val publishPropFile = rootProject.file("publish.properties")
    if (publishPropFile.exists()) {
        Properties().apply {
            load(publishPropFile.inputStream())
        }.forEach { name, value ->
            if (name == "signing.secretKeyRingFile") {
                set(name.toString(), rootProject.file(value.toString()).absolutePath)
            } else {
                set(name.toString(), value)
            }
        }
    } else {
        set("signing.keyId", System.getenv("SIGNING_KEY_ID"))
        set("signing.password", System.getenv("SIGNING_PASSWORD"))
        set("signing.secretKeyRingFile", System.getenv("SIGNING_SECRET_KEY_RING_FILE"))
        set("ossrhUsername", System.getenv("OSSRH_USERNAME"))
        set("ossrhPassword", System.getenv("OSSRH_PASSWORD"))
    }
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}
// https://github.com/gradle/gradle/issues/26091
val signingTasks = tasks.withType<Sign>()
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(signingTasks)
}
publishing {
    if (rootProject.file("publish.properties").exists()) {
        signing {
            sign(publishing.publications)
        }
        repositories {
            maven {
                val releasesRepoUrl =
                    "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                val snapshotsRepoUrl =
                    "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                url = if (version.toString().endsWith("SNAPSHOT")) {
                    uri(snapshotsRepoUrl)
                } else {
                    uri(releasesRepoUrl)
                }
                credentials {
                    username = project.ext.get("ossrhUsername").toString()
                    password = project.ext.get("ossrhPassword").toString()
                }
            }
        }
    }

    publications.withType<MavenPublication> {
        artifact(javadocJar)
        pom {
            name.set("PreCompose")
            description.set("A third-party Jetbrains Compose library with ViewModel, LiveData and Navigation support.")
            url.set("https://github.com/Tlaster/PreCompose")

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("Tlaster")
                    name.set("James Tlaster")
                    email.set("tlaster@outlook.com")
                }
            }
            scm {
                url.set("https://github.com/Tlaster/PreCompose")
                connection.set("scm:git:git://github.com/Tlaster/PreCompose.git")
                developerConnection.set("scm:git:git://github.com/Tlaster/PreCompose.git")
            }
        }
    }
}
