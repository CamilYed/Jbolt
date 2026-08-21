import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("application")
    alias(libs.plugins.javafx)
    alias(libs.plugins.jlink)
    id("jacoco")
    alias(libs.plugins.spotless)
    alias(libs.plugins.sonarqube)
}

group = "com.camilyed.jbolt"
version = providers.gradleProperty("releaseVersion").orElse("1.0.0-SNAPSHOT").get()

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

javafx {
    version = libs.versions.javafx.get()
    modules("javafx.controls", "javafx.graphics")
}

application {
    mainModule.set("github.com.camilyed.jbolt")
    mainClass.set("github.com.camilyed.jbolt.App")
}

dependencies {
    implementation(libs.atlantafx.base)
    implementation(libs.jackson.databind)
    implementation(libs.ikonli.javafx)
    implementation(libs.ikonli.materialdesign2)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.awaitility)
    testImplementation(libs.assertj.core)
    testImplementation(libs.wiremock)
    testImplementation(libs.hamcrest)
    testImplementation(libs.testfx.core)
    testImplementation(libs.testfx.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.javafx.swing)
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("*.gradle.kts")
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("misc") {
        target("*.md", ".gitignore")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

sonar {
    properties {
        property("sonar.projectKey", "SoftwareJCompany_Jbolt")
        property("sonar.organization", "softwarejcompany")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.exclusions", "**/App.java,**/module-info.java")
    }
}

tasks.test {
    useJUnitPlatform()
    include("**/*Test.class", "**/*IT.class")

    systemProperty("java.awt.headless", "false")
    systemProperty("testfx.robot", "glass")
    systemProperty("testfx.headless", "false")
    systemProperty("prism.order", "sw")

    testLogging {
        events = setOf(
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED
        )
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = false
    }

    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) = Unit

        override fun beforeTest(testDescriptor: TestDescriptor) = Unit

        override fun afterTest(
            testDescriptor: TestDescriptor,
            result: TestResult
        ) = Unit

        override fun afterSuite(
            suite: TestDescriptor,
            result: TestResult
        ) {
            if (suite.parent == null) {
                println()
                println("Test result: ${result.resultType}")
                println(
                    "Tests: ${result.testCount}, " +
                        "passed: ${result.successfulTestCount}, " +
                        "failed: ${result.failedTestCount}, " +
                        "skipped: ${result.skippedTestCount}"
                )
                println()
            }
        }
    })

    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}
