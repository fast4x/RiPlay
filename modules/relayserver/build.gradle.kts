plugins {
    kotlin("jvm")
}

sourceSets.all {
    java.srcDir("src/$name/kotlin")
}

dependencies {
    // Server di streaming locale RiPlay (Relay) come modulo condiviso JVM.
    //
    // Stesso engine usato dall'app Android fino alla v0.8.8 (Ktor CIO 3.6.0):
    // il modulo e' puro Kotlin/JVM, quindi compatibile sia con androidApp
    // sia con il target desktop (composeApp jvmMain).
    //
    // api (non implementation): le firme pubbliche espongono tipi Ktor
    // (es. Application.relayEndpoints) -> chi consuma il modulo vede anche
    // le classi Ktor senza ridichiarare la dipendenza.
    api(libs.ktor.server.cio)

    implementation(libs.kotlin.coroutines)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
