import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 앱과 수집 스크립트(:collector)가 함께 쓰는 순수 Kotlin 모듈: TourAPI 응답, 코스 카탈로그 모델, 변환 함수
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
