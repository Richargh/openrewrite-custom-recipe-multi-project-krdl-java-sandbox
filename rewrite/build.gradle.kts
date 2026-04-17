plugins {
    java
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(platform(libs.openrewrite.bom))
    implementation(libs.openrewrite.yaml)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.openrewrite.test)
    testRuntimeOnly(libs.openrewrite.java21)
}

tasks.test {
    useJUnitPlatform()
}
