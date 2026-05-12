import com.google.protobuf.gradle.id

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("com.google.protobuf") version "0.9.5"
}

object Versions {
    const val GRPC = "1.75.0"
    const val GRPC_KOTLIN = "1.4.1"
    const val PROTOBUF = "4.34.1"
}

dependencies {
    implementation(project(":auth"))

    // user 라이브러리 — AuthGrpcControllerAdapter가 CreateUserCommand 등 직접 호출
    implementation("com.troica.msa:user:0.1.0")
    implementation("com.troica.msa:common:0.3.0")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE")
    implementation("com.google.protobuf:protobuf-java:${Versions.PROTOBUF}")
    implementation("com.google.protobuf:protobuf-kotlin:${Versions.PROTOBUF}")
    implementation("io.grpc:grpc-protobuf:${Versions.GRPC}")
    implementation("io.grpc:grpc-stub:${Versions.GRPC}")
    implementation("io.grpc:grpc-kotlin-stub:${Versions.GRPC_KOTLIN}")
    implementation("io.grpc:grpc-netty-shaded:${Versions.GRPC}")

    runtimeOnly("org.postgresql:postgresql:42.7.11")
}

sourceSets {
    main {
        java.srcDirs(
            "build/generated/source/proto/main/java",
            "build/generated/source/proto/main/kotlin",
        )
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${Versions.PROTOBUF}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${Versions.GRPC}"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${Versions.GRPC_KOTLIN}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("kotlin")
            }
            task.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}

springBoot {
    mainClass.set("dev.ktcloud.black.auth.service.AuthServiceApplicationKt")
}
