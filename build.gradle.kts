plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.1.1"
}

group = "com.camilyed.jbolt"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

javafx {
    version = "25"
    modules("javafx.controls", "javafx.fxml", "javafx.graphics")
}

application {
    mainModule.set("com.camilyed.jbolt")
    mainClass.set("com.camilyed.jbolt.App")
}

dependencies {
    implementation("io.github.mkpaz:atlantafx-base:2.0.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.testfx:testfx-core:4.0.18")
    testImplementation("org.testfx:testfx-junit5:4.0.18")
    testRuntimeOnly("org.openjfx:javafx-swing:25")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "false")
    systemProperty("testfx.robot", "glass")
    systemProperty("testfx.headless", "true")
}