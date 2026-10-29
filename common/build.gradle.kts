plugins {
    id("hexanime.minecraft")
}

architectury {
    common("fabric", "forge")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(kotlin("reflect"))

    // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
    // Do NOT use other classes from fabric loader
    modImplementation(libs.fabric.loader)
    modApi(libs.architectury)

    modApi(libs.hexcasting.common)

    modApi(libs.clothConfig.common)

    // KosmX playerAnimator. Compile against the Fabric artifact (Loom remaps it).
    // Not bundled: Fabric and Forge redeclare modImplementation without include/shadow.
    modImplementation(libs.playerAnim.fabric)

    libs.mixinExtras.common.also {
        implementation(it)
        annotationProcessor(it)
    }

    testImplementation(kotlin("test"))
}
