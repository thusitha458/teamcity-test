import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinScript
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2022.10"

project {

    buildType(GitTags)
}

object GitTags : BuildType({
    name = "Git Tags"

    params {
        password("env.GITHUB_TOKEN", "credentialsJSON:643c4fa5-223a-4e42-a7dd-2dfbbd35c3a7")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "Create Tag"
            scriptContent = """
                echo "Team city"
                curl --location 'https://api.github.com/repos/thusitha458/teamcity-test/git/refs/tags' \
                                --header 'X-GitHub-Api-Version: 2022-11-28' \
                                --header 'Authorization: Bearer %env.GITHUB_TOKEN%'
            """.trimIndent()
        }
        kotlinScript {
            name = "Kotlin FTW"
            content = """
                #!/usr/bin/env kotlin
                
                @file:Repository("https://repo1.maven.org/maven2/")
                @file:DependsOn("com.squareup.okhttp3:okhttp:4.9.1")
                @file:DependsOn("com.google.code.gson:gson:2.8.6")
                
                import okhttp3.*;
                import com.google.gson.Gson;
                
                val githubToken = args[0];
                val client = OkHttpClient()
                
                data class GithubTag (
                    val ref: String
                )
                
                var gson = Gson()
                
                fun run() {
                    val request = Request.Builder()
                        .url("https://api.github.com/repos/thusitha458/teamcity-test/git/refs/tags")
                        .header("X-GitHub-Api-Version", "2022-11-28")
                        .header("Authorization", "Bearer ${'$'}githubToken")
                        .build()
                    
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw java.io.IOException("Unexpected code ${'$'}response")
                        
                        println(response.body!!.string())
                    }
                }
                
                run()
            """.trimIndent()
            arguments = "%env.GITHUB_TOKEN%"
        }
    }

    triggers {
        vcs {
        }
    }

    features {
        perfmon {
        }
    }
})
