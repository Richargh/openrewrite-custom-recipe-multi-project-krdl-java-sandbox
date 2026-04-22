rootProject.name = "openrewrite-custom-recipe-multi-project-sandbox-krdl-java-sandbox"

include("rewrite")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}
