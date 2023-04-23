import com.fasterxml.jackson.databind.ObjectMapper
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import java.net.URL

//import java.net.URI
//import java.net.http.HttpClient
//import java.net.http.HttpRequest
//import java.net.http.HttpResponse

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
            fun testItOut(capitalize: Boolean): String {
//                val client = HttpClient.newBuilder().build()
//                val request = HttpRequest.newBuilder()
//                    .uri(URI.create("https://api.github.com/repos/thusitha458/teamcity-test/git/refs/tags"))
//                    .header("X-GitHub-Api-Version", "2022-11-28")
//                    .header("Authorization", "Bearer %env.GITHUB_TOKEN%")
//                    .build()
//
//                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
//                println(response)

                val value = URL("https://api.github.com/repos/thusitha458/teamcity-test/git/refs/tags").openConnection().apply {
                    readTimeout = 800
                    connectTimeout = 200
                    setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                    setRequestProperty("Authorization", "Bearer %env.GITHUB_TOKEN%")
                }.getInputStream().use {
                    val result = ObjectMapper().readTree(it).map { node ->
                        node.get("ref").asText()
                    }
                    result
                }

                return if (capitalize) "CREATE TAG ($value)" else "Create tag"
            }
            name = testItOut(true)
            scriptContent = """
                echo "Team city"
                curl --location 'https://api.github.com/repos/thusitha458/teamcity-test/git/refs/tags' \
                                --header 'X-GitHub-Api-Version: 2022-11-28' \
                                --header 'Authorization: Bearer %env.GITHUB_TOKEN%'
            """.trimIndent()
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
