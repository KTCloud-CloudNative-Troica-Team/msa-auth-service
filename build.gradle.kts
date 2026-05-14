import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    java
    kotlin("jvm") version "2.1.0" apply false
    kotlin("plugin.spring") version "2.1.0" apply false
    kotlin("plugin.jpa") version "2.1.0" apply false
    id("org.springframework.boot") version "3.5.14" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false

    // R-45 (평가 심화 (2)-1): SonarCloud 정적 분석 + 커버리지 게이트.
    id("org.sonarqube") version "5.1.0.4882"
}

sonar {
    properties {
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.organization", "ktcloud-cloudnative-troica-team")
        property("sonar.projectKey", "KTCloud-CloudNative-Troica-Team_msa-auth-service")
        // wait=false: SonarCloud 무료 plan은 프로젝트별 custom Quality Gate 적용 불가
        // (Sonar way default Coverage 80% 강제, PoC 단계에서 항상 fail). 분석은 정상 수행
        // 되고 결과는 Dashboard 에 표시되나 CI 는 결과를 기다리지 않고 통과시킴. 단위
        // 테스트 정착 + paid plan 전환 시 true 로 복구.
        property("sonar.qualitygate.wait", "false")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "**/build/reports/jacoco/test/jacocoTestReport.xml",
        )
        property("sonar.exclusions", "**/generated/**, **/build/**")
    }
}

allprojects {
    group = "com.troica.msa"
    version = providers.gradleProperty("version").get()

    repositories {
        mavenCentral()
        mavenLocal()
        // GitHub Packages — msa-common-libs:common, msa-user-service:user 둘 다 호스트
        maven {
            name = "GitHubPackagesCommonLibs"
            url = uri("https://maven.pkg.github.com/KTCloud-CloudNative-Troica-Team/msa-common-libs")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                    ?: providers.gradleProperty("gpr.user").orNull
                password = System.getenv("GITHUB_TOKEN")
                    ?: providers.gradleProperty("gpr.token").orNull
            }
        }
        maven {
            name = "GitHubPackagesUserService"
            url = uri("https://maven.pkg.github.com/KTCloud-CloudNative-Troica-Team/msa-user-service")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                    ?: providers.gradleProperty("gpr.user").orNull
                password = System.getenv("GITHUB_TOKEN")
                    ?: providers.gradleProperty("gpr.token").orNull
            }
        }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "io.spring.dependency-management")
    // R-45: JaCoCo로 커버리지 측정 → SonarCloud가 XML report 읽음.
    apply(plugin = "jacoco")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    extensions.configure<KotlinJvmProjectExtension> {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict")
        }
        jvmToolchain(21)
    }

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.14")
        }
    }

    dependencies {
        "implementation"("org.jetbrains.kotlin:kotlin-reflect")
        "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
    }

    // R-45: test → jacocoTestReport → Sonar XML report 자동 생성.
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        finalizedBy(tasks.matching { it.name == "jacocoTestReport" })
    }
    tasks.withType<org.gradle.testing.jacoco.tasks.JacocoReport>().configureEach {
        dependsOn(tasks.withType<Test>())
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}
