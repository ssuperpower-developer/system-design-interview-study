plugins {
    id("java")
}

group = "org"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("redis.clients:jedis:4.3.1")
}

tasks.test {
    useJUnitPlatform()
}