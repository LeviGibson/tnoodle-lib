import configurations.Languages.attachRemoteRepositories
import configurations.Languages.configureJava
import configurations.Publications.configureMavenPublication
import configurations.Publications.configureSignatures

description = "A three-phase solver for the FTO puzzle."

plugins {
    `java-library`
    `maven-publish`
    signing
}

configureJava()
configureMavenPublication("scrambler-fto3phase")
configureSignatures(publishing)

attachRemoteRepositories()

dependencies {
    implementation(libs.slf4j.api)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.test {
    useJUnitPlatform()
}
