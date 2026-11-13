plugins {
    java
}

group = "dev.lumina"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName = "LuminaUI"
    archiveVersion.set(project.version.toString())
    manifest {
        attributes(
            "Implementation-Title" to "LuminaUI",
            "Implementation-Version" to project.version
        )
    }
}
