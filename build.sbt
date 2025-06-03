import play.sbt.routes.RoutesKeys
import sbt.Def
import scoverage.ScoverageKeys
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val appName: String = "ioss-netp-registration-frontend"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.5"

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(inConfig(Test)(testSettings) *)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(itSettings): _*)
  .settings(ThisBuild / useSuperShell := false)
  .settings(scalacOptions += "-Wconf:msg=Flag.*repeatedly:s")
  .settings(
    name := appName,
    RoutesKeys.routesImport ++= Seq(
      "models._",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl",
      "pages.Waypoints",
      "pages.EmptyWaypoints",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
    ),
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.config._",
      "views.ViewUtils._",
      "models.Mode",
      "pages.Waypoints",
      "controllers.routes._",
      "viewmodels.govuk.all._",
      "pages.Waypoints"
    ),
    PlayKeys.playDefaultPort := 10181,
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*handlers.*;.*components.*;" +
      ".*Routes.*;.*viewmodels.govuk.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 78,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    scalacOptions ++= Seq(
      "-feature",
      "-Wconf:msg=deprecation:w,msg=feature:w,msg=optimizer:w,src=target/.*:s"
    ),
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    // concatenate js
    Concat.groups := Seq(
      "javascripts/application.js" ->
        group(
          baseDirectory.value / "app" / "assets" / "javascripts" * "*.js"
        )
    ),
    uglifyOps := UglifyOps.singleFile,
    // prevent removal of unused code which generates warning errors due to use of third-party libs
    uglifyCompressOptions := Seq("unused=false", "dead_code=false"),
    pipelineStages := Seq(digest),
    Assets / pipelineStages := Seq(concat, uglify),
    uglify / includeFilter := GlobFilter("application*.js")
  )

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork := true,
  unmanagedSourceDirectories += baseDirectory.value / "test-utils"
)

lazy val itSettings = Defaults.itSettings ++ Seq(
  unmanagedSourceDirectories := Seq(
    baseDirectory.value / "it",
    baseDirectory.value / "test-utils"
  ),
  unmanagedResourceDirectories := Seq(
    baseDirectory.value / "it" / "resources"
  ),
  parallelExecution := false,
  fork := true
)