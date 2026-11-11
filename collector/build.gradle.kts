import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// TourAPI 코스 수집 스크립트. 앱에 들어가지 않고 PC 에서 직접 실행한다. 사용법은 collector/README.md
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
}

dependencies {
    implementation(project(":shared"))
    testImplementation(libs.junit)
}

application {
    mainClass.set("com.example.acarive.collector.MainKt")
}

tasks.named<JavaExec>("run") {
    // 설정 파일·출력 경로를 프로젝트 루트 기준으로 쓰기 위해
    workingDir = rootProject.projectDir
}
