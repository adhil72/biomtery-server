plugins {
    kotlin("jvm") version "2.0.0"
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.14"
    id("org.springframework.boot") version "3.3.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

group = "gecw.cse"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.mongodb:mongodb-driver-sync:5.1.2")
    implementation("com.machinezoo.sourceafis:sourceafis:3.18.1")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-test")

    implementation("org.apache.commons:commons-csv:1.9.0")
    implementation("com.machinezoo.sourceafis:sourceafis:3.18.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(18)
}

javafx {
    version = "22"
    modules = listOf("javafx.controls", "javafx.web")
}

application {
    mainClass.set("gecw.cse.MainKt")
}