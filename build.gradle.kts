plugins {
    alias(libs.plugins.rewrite)
}

group = "com.example"
version = "0.1.0-SNAPSHOT"

rewrite {
    activeRecipe("de.richargh.sandbox.openrewrite.gitlabci.GitLabCiPropertyOrderRecipe")
}

dependencies {
    rewrite(project(":rewrite"))
}
