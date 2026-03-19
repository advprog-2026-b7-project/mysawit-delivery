plugins {
    java
    jacoco
    checkstyle
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "4.4.1.3373"

}

group = "id.ac.ui.cs.advprog"
version = "0.0.1-SNAPSHOT"
description = "mysawit-delivery"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:postgresql")

    implementation("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.h2database:h2")
    testImplementation("org.projectlombok:lombok")
    runtimeOnly("com.h2database:h2")
    testImplementation("com.h2database:h2")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    filter {
        excludeTestsMatching("*FunctionalTest")
    }
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

checkstyle {
    toolVersion = "10.12.5"
    isIgnoreFailures = false
}