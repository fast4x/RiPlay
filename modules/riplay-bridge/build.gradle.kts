plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // riplay-bridge: API REST /api/v1 della libreria RiPlay per il provider
    // Music Assistant (Python, repo esterno riplay-music-assistant).
    //
    // Modulo JVM puro, stessa impronta del modulo relayserver: nessuna
    // dipendenza Android, consumabile da androidApp e dal target desktop
    // (composeApp jvmMain).
    //
    // Il modulo NON apre una porta propria in produzione: il modulo Ktor
    // bridgeModule() viene montato DALLA APPLICAZIONE HOST (RelayServer, che
    // lo passa a embeddedServer tramite il parametro extraModules): stessa
    // porta e stesso token del server di streaming. BridgeServer resta utile
    // solo per eseguire il bridge da solo in sviluppo/test.
    //
    // api (non implementation): le firme pubbliche espongono tipi Ktor
    // (es. Application.bridgeModule) -> chi consuma il modulo vede anche
    // le classi Ktor senza ridichiarare la dipendenza (stesso criterio di
    // relayserver con api(libs.ktor.server.cio)).
    api(libs.ktor.server.cio)

    // Autenticazione Bearer e ContentNegotiation (JSON kotlinx) lato server:
    // artefatti separati dal core in Ktor 3.
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.json)

    implementation(libs.kotlin.coroutines)

    testImplementation(kotlin("test"))
    // testApplication (E2E su JVM, senza porte/socket reali)
    testImplementation(libs.ktor.server.test.host)
}

tasks.test {
    useJUnitPlatform()
}
