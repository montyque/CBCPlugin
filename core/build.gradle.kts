java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

plugins {
    `java-library`
    `maven-publish`
}

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

    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly(libs.com.comphenix.protocol.protocollib)

    // JUnit 5
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.20.0")

}

tasks.test {
    useJUnitPlatform()
}
