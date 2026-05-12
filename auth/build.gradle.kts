plugins {
    kotlin("plugin.spring")
}

object Versions {
    const val JWT = "0.12.6"
}

dependencies {
    // GitHub Packages (D1/Q2 결정)
    implementation("com.troica.msa:user:0.1.0")     // user 도메인 — auth가 CreateUserCommand 등 in-process 호출
    implementation("com.troica.msa:common:0.3.1")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    implementation("io.jsonwebtoken:jjwt-api:${Versions.JWT}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${Versions.JWT}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${Versions.JWT}")
}
