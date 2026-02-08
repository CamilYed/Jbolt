plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.1.1"
}

val javaVersion: String by extra { "25" }
val javafxVersion: String by extra { "25" }
val atlantaFxVersion: String by extra { "2.0.1" }
val jacksonVersion: String by extra { "2.18.2" }
val junitVersion: String by extra { "5.11.4" }
val assertjVersion: String by extra { "3.27.3" }
val testFxVersion: String by extra { "4.0.18" }
val hamcrestVersion: String by extra { "3.0" }
val ikonliVersion: String by extra { "12.4.0" }
val junitLauncherVersion: String by extra { "1.11.4" }

group = "com.camilyed.jbolt"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

javafx {
    version = javafxVersion
    modules("javafx.controls", "javafx.fxml", "javafx.graphics")
}

application {
    mainModule.set("github.com.camilyed.jbolt")
    mainClass.set("github.com.camilyed.jbolt.App")
}

dependencies {
    implementation("io.github.mkpaz:atlantafx-base:$atlantaFxVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("org.kordamp.ikonli:ikonli-javafx:${ikonliVersion}")
    implementation("org.kordamp.ikonli:ikonli-materialdesign2-pack:${ikonliVersion}")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("org.hamcrest:hamcrest:${hamcrestVersion}")
    testImplementation("org.testfx:testfx-core:$testFxVersion")
    testImplementation("org.testfx:testfx-junit5:$testFxVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:${junitLauncherVersion}")
    testRuntimeOnly("org.openjfx:javafx-swing:$javafxVersion")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }

    systemProperty("java.awt.headless", "false")
    systemProperty("testfx.robot", "glass")
    systemProperty("testfx.headless", "false")
    systemProperty("prism.order", "sw")
}