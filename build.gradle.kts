plugins {
    id("java")
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.gradleup.shadow") version "8.3.0"
}

group = "neonique"
version = "2.0.0-SNAPSHOT"
description = "CBCPlugin"
java.sourceCompatibility = JavaVersion.VERSION_25

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }

    maven {
        url = uri("https://repo.dmulloy2.net/repository/public/")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {

    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.116.1")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly("dev.jorel:commandapi-paper-core:12.0.0")
    compileOnly("io.papermc.paper:paper-api:26.2.build.115-stable")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    testImplementation("dev.jorel:commandapi-paper-core:12.0.0")
    testImplementation("io.papermc.paper:paper-api:26.2.build.115-stable")
    testImplementation("net.dmulloy2:ProtocolLib:5.4.0")
    testImplementation("com.google.code.findbugs:jsr305:3.0.2")

}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

sourceSets {
    main {
        java {
            exclude("neonique/cbcplugin_new/lobby/**")
            exclude("neonique/cbcplugin_new/gamemodes/**")
            exclude("neonique/cbcplugin_new/managers/**")
            exclude("neonique/cbcplugin_new/listeners/**")
            exclude("neonique/cbcplugin_new/commands_old/**")
            exclude("neonique/cbcplugin_new/cbcevents/**")
            exclude("neonique/cbcplugin_new/misc/**")
            exclude("neonique/cbcplugin_new/combat/CombatManager.java")
        }
    }
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.2")
    }

    shadowJar {
    }

    build {
        dependsOn("shadowJar")
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    test {
        useJUnitPlatform()
    }
}