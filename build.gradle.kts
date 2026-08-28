plugins {
    kotlin("jvm") version "1.9.23"
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.5"
}

group   = "io.github.wordninja"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin stdlib — marked as api so Java consumers transitively get it
    api(kotlin("stdlib"))

    // Test
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.test {
    useJUnitPlatform()
}

// ── Fat jar (all-in-one, no external deps needed) ──────────────────────────
tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Implementation-Title"]   = project.name
        attributes["Implementation-Version"] = project.version
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.get()
        .filter { it.name.endsWith(".jar") }
        .map { zipTree(it) })
}

// Build fat jar automatically when running `./gradlew build`
tasks.build {
    dependsOn("fatJar")
}

// ── Maven publish (optional — for GitHub Packages / Maven Central) ─────────
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("wordninja-kt")
                description.set("Kotlin port of the Python wordninja word-segmentation library")
                url.set("https://github.com/khiz3r/wordninja-kt")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
            }
        }
    }
}