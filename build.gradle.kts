import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "com.pineypiney"
version = "1.0-SNAPSHOT"

val lwjglVersion = "3.2.3"
val lwjglNatives = "natives-windows"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))

    // LWJGL, needed by GLM
    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-assimp:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-bgfx:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-cuda:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-egl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-jawt:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-jemalloc:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-libdivide:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-llvm:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-lmdb:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-lz4:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-meow:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-nanovg:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-nfd:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-nuklear:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-odbc:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-openal:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opencl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengles:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-openvr:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opus:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-ovr:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-par:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-remotery:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-rpmalloc:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-shaderc:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-sse:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-stb:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-tinyexr:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-tinyfd:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-tootle:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-vma:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-vulkan:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-xxhash:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-yoga:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-zstd:$lwjglVersion")

    // GLM
    implementation("com.github.kotlin-graphics.glm:glm:375708cf1c0942b0df9d624acddb1c9993f6d92d")

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

application {
    mainClass.set("MainKt")
}