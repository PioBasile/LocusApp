pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        val localProps = java.util.Properties().also { props ->
            rootProject.projectDir.resolve("local.properties")
                .takeIf { it.exists() }?.inputStream()?.use { props.load(it) }
        }
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication { create<HttpHeaderAuthentication>("header") }
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "Token ${localProps.getProperty("MAPBOX_DOWNLOADS_TOKEN", "")}"
            }
        }
    }
}

rootProject.name = "Locus"
include(":app")
 