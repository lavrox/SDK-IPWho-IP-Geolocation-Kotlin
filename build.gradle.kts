plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
    signing
    id("com.vanniktech.maven.publish") version "0.34.0"
}

group = "com.lavrox"
version = "1.0.0"

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

repositories {
    mavenCentral()
}

dependencies {
    api("io.ktor:ktor-client-core:2.3.12")
    api("io.ktor:ktor-client-cio:2.3.12")
    api("io.ktor:ktor-client-content-negotiation:2.3.12")
    api("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

signing {
    useGpgCmd()
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("com.lavrox", "ipwho-kotlin", "1.0.0")
    pom {
        name.set("IPWho Kotlin SDK")
        description.set("Official Kotlin client for the IPWho IP geolocation API.")
        inceptionYear.set("2026")
        url.set("https://github.com/lavrox/SDK-IPWho-IP-Geolocation-Kotlin")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("lavrox")
                name.set("Lavrox")
                url.set("https://lavrox.com")
            }
        }
        scm {
            url.set("https://github.com/lavrox/SDK-IPWho-IP-Geolocation-Kotlin")
            connection.set("scm:git:git://github.com/lavrox/SDK-IPWho-IP-Geolocation-Kotlin.git")
            developerConnection.set("scm:git:ssh://git@github.com/lavrox/SDK-IPWho-IP-Geolocation-Kotlin.git")
        }
    }
}
