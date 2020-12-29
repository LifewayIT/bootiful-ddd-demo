import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.4.1"
	id("io.spring.dependency-management") version "1.0.8.RELEASE"
	kotlin("jvm") version "1.4.21"
	kotlin("plugin.spring") version "1.4.21"
}

group = "com.lifeway"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11
val artifactory_user = System.getenv("ARTIFACTORY_USER")
val artifactory_password = System.getenv("ARTIFACTORY_PASSWORD")

repositories {
	mavenCentral()
	maven {
		setUrl("https://artifactory.prod.lifeway.com/artifactory/bsolutions-repo-group")
		credentials {
			username = artifactory_user
			password = artifactory_password
		}
	}
}


val springDependencies = listOf(
	"org.springframework.boot:spring-boot-starter-data-mongodb",
	"org.springframework.boot:spring-boot-starter-data-redis-reactive",
	"org.springframework.data:spring-data-mongodb",
	"org.springframework.boot:spring-boot-starter-webflux",
	"org.springframework.kafka:spring-kafka"
)

val kotlinDependencies = listOf(
	"com.fasterxml.jackson.module:jackson-module-kotlin",
	"io.projectreactor.kotlin:reactor-kotlin-extensions",
	"org.jetbrains.kotlin:kotlin-reflect",
	"org.jetbrains.kotlinx:kotlinx-coroutines-reactor"
)

val axonDependencies = listOf(
	"org.axonframework:axon-spring-boot-starter:4.4",
	"org.axonframework.extensions.mongo:axon-mongo:4.4",
	"org.axonframework.extensions.reactor:axon-reactor-spring-boot-starter:4.4.2",
	"org.axonframework.extensions.reactor:axon-reactor-spring-boot-starter:4.4.2",
	"org.axonframework.extensions.kotlin:axon-kotlin:0.1.0"
)

dependencies {

	springDependencies.forEach { implementation(it) }
	kotlinDependencies.forEach { implementation(it) }
	axonDependencies.forEach {
		implementation(it) { exclude("org.axonframework","axon-server-connector") }
	}

	implementation("org.reflections:reflections:0.9.11")
	testImplementation("junit:junit:4.12")
	testImplementation("org.axonframework:axon-test:4.4.5")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.4.21")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
