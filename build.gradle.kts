val kotlin_version: String by project
val logback_version: String by project
val mongo_version: String by project
val bcrypt_version: String by project
val kmongo_version: String by project
val dotenv_version: String by project
val ktor_version: String by project
val kotlinx_serialization_json: String by project

plugins {
    kotlin("jvm") version "2.0.21"
    id("io.ktor.plugin") version "3.0.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

group = "com.example"
version = "0.0.2"

application {
    mainClass.set("com.savewatchserver.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("org.litote.kmongo:kmongo:$kmongo_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")

    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_json")

    implementation("io.github.cdimascio:dotenv-kotlin:$dotenv_version")
    implementation("org.mindrot:jbcrypt:$bcrypt_version")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("org.mongodb:mongodb-driver-core:$mongo_version")
    implementation("org.mongodb:mongodb-driver-sync:$mongo_version")
    implementation("org.mongodb:bson:$mongo_version")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")
    implementation("io.ktor:ktor-server-cio-jvm")
    implementation("io.ktor:ktor-server-cors-jvm")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-utils-jvm")
    implementation("org.litote.kmongo:kmongo-serialization:$kmongo_version")
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
//    testImplementation("io.ktor:ktor-server-tests-jvm")
}

// Для Docker
tasks.create<Jar>("fatJar") {
    group = "build"
    description = "Assembles a fat JAR file containing all dependencies."
    manifest {
        attributes["Main-Class"] = "com.savewatchserver.ApplicationKt" 
    }
    val runtimeClasspath = configurations.runtimeClasspath.get()
    from(runtimeClasspath.map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
