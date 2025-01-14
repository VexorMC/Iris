plugins {
    id("java")
    id("idea")
    id("fabric-loom") version ("1.7.3")
}

val MINECRAFT_VERSION: String by rootProject.extra
val PARCHMENT_VERSION: String? by rootProject.extra
val FABRIC_LOADER_VERSION: String by rootProject.extra
val FABRIC_API_VERSION: String by rootProject.extra
val MOD_VERSION: String by rootProject.extra

repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }

    maven("https://maven.legacyfabric.net/")
}

base {
    archivesName.set("iris-fabric")
}

dependencies {
    minecraft("com.mojang:minecraft:${MINECRAFT_VERSION}")
    mappings("net.legacyfabric:yarn:1.8.9+build.551:v2")
    modImplementation("net.fabricmc:fabric-loader:$FABRIC_LOADER_VERSION")

    fun implementAndInclude(name: String) {
        modImplementation(name)
        include(name)
    }



    //modImplementation("maven.modrinth", "sodium", "mc1.21.1-0.6.1-fabric")
    implementAndInclude("org.antlr:antlr4-runtime:4.13.1")
    implementAndInclude("io.github.douira:glsl-transformer:2.0.1")
    implementAndInclude("org.anarres:jcpp:1.4.14")
    implementAndInclude("org.joml:joml:1.10.8")
    implementAndInclude("it.unimi.dsi:fastutil:8.5.15")

    implementation(project.project(":common").sourceSets.getByName("vendored").output)
    implementation(project.project(":common").sourceSets.getByName("api").output)
    compileOnly(project.project(":common").sourceSets.getByName("headers").output)
    implementation(project.project(":common").sourceSets.getByName("main").output)

    compileOnly(files(rootDir.resolve("DHApi.jar")))
}

tasks.named("compileTestJava").configure {
    enabled = false
}

tasks.named("test").configure {
    enabled = false
}

loom {
    if (project(":common").file("src/main/resources/iris.accesswidener").exists())
        accessWidenerPath.set(project(":common").file("src/main/resources/iris.accesswidener"))

    @Suppress("UnstableApiUsage")
    mixin { defaultRefmapName.set("iris-fabric.refmap.json") }

    runs {
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}

tasks {
    processResources {
        from(project.project(":common").sourceSets.main.get().resources)
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to project.version))
        }
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(zipTree(project.project(":common").tasks.jar.get().archiveFile))

        manifest.attributes["Main-Class"] = "net.irisshaders.iris.LaunchWarn"
    }

    remapJar.get().destinationDirectory = rootDir.resolve("build").resolve("libs")
}
