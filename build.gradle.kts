/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Java project to get you started.
 * For more details take a look at the Java Quickstart chapter in the Gradle
 * User Manual available at https://docs.gradle.org/5.5.1/userguide/tutorial_java_projects.html
 */

plugins {
    // Apply the java plugin to add support for Java
    java

    // Apply the application plugin to add support for building a CLI application
    application
}

repositories {
    jcenter()
    mavenCentral()
    maven("https://repo.spring.io/milestone")
}

java {
    sourceCompatibility = JavaVersion.VERSION_12
    targetCompatibility = JavaVersion.VERSION_12

}

dependencies {
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("com.google.guava:guava:27.1-jre")
    implementation("com.h2database:h2:1.4.199")
    implementation("io.projectreactor.netty:reactor-netty:0.9.0.RC1")
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    implementation("org.sql2o:sql2o:1.6.0")
    implementation("org.slf4j:slf4j-api:1.7.28")
    implementation("ch.qos.logback:logback-classic:1.2.3")


    compileOnly("org.projectlombok:lombok:1.18.10")
    annotationProcessor("org.projectlombok:lombok:1.18.10")

    // Use JUnit test framework
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:3.0.0")
    testImplementation("io.projectreactor:reactor-test:3.3.0.RELEASE")
}

application {
    // Define the main class for the application
    mainClassName = "revolut.App"
}
