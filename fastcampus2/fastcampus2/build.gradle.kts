//plugins {
//    id("java")
//    id "io.spring.dependency-management" version "1.0.7.RELEASE"
//}
//
//group = "org"
//version = "1.0-SNAPSHOT"
//
//repositories {
//    mavenCentral()
//}
//
//dependencyManagement {
//    imports {
//        mavenBom 'io.projectreactor:reactor-bom:2022.0.8'
//    }
//}
//
//dependencies {
//    testImplementation(platform("org.junit:junit-bom:5.10.0"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
//
//    implementation 'redis.clients:jedis'
//    implementation 'io.projectreactor:reactor-core'
//    testImplementation 'io.projectreactor:reactor-test'
//}
//
//tasks.test {
//    useJUnitPlatform()
//}