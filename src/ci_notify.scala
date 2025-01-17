/* Author: Fabian Huch, TU Muenchen

Build job to trigger ci pipelines.
 */
package isabelle.tools_collection


import isabelle.*

import java.net.{HttpURLConnection, URL}
import java.io.{BufferedReader, IOException, InputStreamReader, OutputStreamWriter}


object CI_Notify {
  def post(url: URL, params: Map[String, String], body: String): String =
    try {
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setRequestMethod("POST")

      conn.setDoOutput(true)
      params.foreach { case (param, value) => conn.setRequestProperty(param, value) }
      val writer = new OutputStreamWriter(conn.getOutputStream)
      writer.write(body)
      writer.flush()

      File.read_stream(conn.getInputStream)
    } catch {
      case _: IOException => error("Could not read from " + url.toString)
    }

  def dispatch(options: Options): CI_Build.Result = {
    try {
      // linter (github action)
      post(
        new URL("https://api.github.com/repos/isabelle-prover/isabelle-linter/dispatches"),
        Map(
          "Accept" -> "application/vnd.github+json",
          "Authorization" -> ("Bearer " + options.string("ci_github_dispatch_token"))),
        "{\"event_type\":\"isabelle-update\"}")

      // context build (github action)
      post(
        new URL("https://api.github.com/repos/isabelle-prover/isabelle-context-build/dispatches"),
        Map(
          "Accept" -> "application/vnd.github+json",
          "Authorization" -> ("Bearer " + options.string("ci_github_dispatch_token"))),
        "{\"event_type\":\"isabelle-update\"}")

      // findfacts
      post(
        new URL("https://api.github.com/repos/Dacit/findfacts/dispatches"),
        Map(
          "Accept" -> "application/vnd.github+json",
          "Authorization" -> ("Bearer " + options.string("ci_github_dispatch_token"))),
        "{\"event_type\":\"isabelle-update\"}")

      // new tools here

      CI_Build.Result.ok
    } catch {
      case ERROR(msg) =>
        println("POST request failed: " + quote(msg))
        CI_Build.Result.error
    }
  }

  val ci_notify = CI_Build.Job(
    "notify", "notifies external ci pipelines",
    CI_Build.Local("notify", 1, 1, false),
    CI_Build.Build_Config(
      documents = false, clean = false, post_hook = (_, options, _) => dispatch(options))
  )
}

class CI_Builds extends Isabelle_CI_Builds(
  CI_Notify.ci_notify)