import ProjectInfo._
import kevinlee.sbt.SbtCommon.crossVersionProps
import just.semver.SemVer
import SemVer.{Major, Minor}
import microsites.ConfigYml

val DottyVersions = Seq("3.0.0-M1", "3.0.0-M2")
val ProjectScalaVersion = "2.13.3"

val removeDottyIncompatible: ModuleID => Boolean =
  m => 
    m.name == "wartremover" ||
    m.name == "ammonite" ||
    m.name == "kind-projector" ||
    m.name == "mdoc"

val CrossScalaVersions: Seq[String] = (Seq(
  "2.10.7", "2.11.12", "2.12.12", "2.13.3"
) ++ DottyVersions).distinct

val GitHubUsername = "Kevin-Lee"
val RepoName = "just-fp"
val ProjectName = RepoName

def prefixedProjectName(name: String) = s"$ProjectName${if (name.isEmpty) "" else s"-$name"}"

lazy val noPublish: SettingsDefinition = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  skip in sbt.Keys.`package` := true,
  skip in packagedArtifacts := true,
  skip in publish := true
)

val hedgehogVersionFor2_10 = "7bd29241fababd9a3e954fd38083ed280fc9e4e8"
val hedgehogVersion = "0.5.1"
val hedgehogRepo: MavenRepository =
  "bintray-scala-hedgehog" at "https://dl.bintray.com/hedgehogqa/scala-hedgehog"

def hedgehogLibs(hedgehogVersion: String): Seq[ModuleID] = Seq(
    "qa.hedgehog" %% "hedgehog-core" % hedgehogVersion % Test
  , "qa.hedgehog" %% "hedgehog-runner" % hedgehogVersion % Test
  , "qa.hedgehog" %% "hedgehog-sbt" % hedgehogVersion % Test
  )

Global / semanticdbEnabled := false
ThisBuild / semanticdbEnabled := false

ThisBuild / scalaVersion := ProjectScalaVersion
ThisBuild / version := ProjectVersion
ThisBuild / organization := "io.kevinlee"
ThisBuild / developers := List(
    Developer(GitHubUsername, "Kevin Lee", "kevin.code@kevinlee.io", url(s"https://github.com/$GitHubUsername"))
  )
ThisBuild / homepage := Some(url(s"https://github.com/$GitHubUsername/$RepoName"))
ThisBuild / scmInfo :=
  Some(ScmInfo(
      url(s"https://github.com/$GitHubUsername/$RepoName")
    , s"git@github.com:$GitHubUsername/$RepoName.git"
    ))

libraryDependencies := (
  if (isDotty.value)
    libraryDependencies.value
      .filterNot(removeDottyIncompatible)
  else
    libraryDependencies.value
)
libraryDependencies := libraryDependencies.value.map(_.withDottyCompat(scalaVersion.value))

lazy val core = (project in file("core"))
  .enablePlugins(DevOopsGitReleasePlugin)
//  .disablePlugins((if (isDotty.value) Seq(WartRemover) else Seq.empty[AutoPlugin]):_*)
  .settings(
    name := prefixedProjectName("core")
  , semanticdbEnabled := false
  , description  := "Just FP Lib - Core"
  , crossScalaVersions := CrossScalaVersions
  , unmanagedSourceDirectories in Compile ++= {
      val sharedSourceDir = baseDirectory.value / "src/main"
      if (scalaVersion.value.startsWith("2.10") || scalaVersion.value.startsWith("2.11"))
        Seq(sharedSourceDir / "scala-2.10_2.11")
      else if (scalaVersion.value.startsWith("2.12"))
        Seq(
          sharedSourceDir / "scala-2.12_2.13",
          sharedSourceDir / "scala-2.12_3.0",
        )
      else if (scalaVersion.value.startsWith("2.13"))
        Seq(
          sharedSourceDir / "scala-2.12_2.13",
          sharedSourceDir / "scala-2.12_3.0",
          sharedSourceDir / "scala-2.13_3.0",
        )
      else if (scalaVersion.value.startsWith("3.0"))
        Seq(
          sharedSourceDir / "scala-2.12_3.0",
          sharedSourceDir / "scala-2.13_3.0"
        )
      else
        Seq.empty
    }
  , scalacOptions :=
      ( if (isDotty.value)
          Seq(
            "-source:3.0-migration",
            "-Ykind-projector",
            "-language:" + List(
              "dynamics",
              "existentials",
              "higherKinds",
              "reflectiveCalls",
              "experimental.macros",
              "implicitConversions"
            ).mkString(","),
          )
        else
          Nil
      )
  , resolvers ++= Seq(
        hedgehogRepo
      )
  , addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.0" cross CrossVersion.full)
  /* Ammonite-REPL { */
  , libraryDependencies ++=
      (scalaBinaryVersion.value match {
        case "2.10" =>
          Seq.empty[ModuleID]
        case "2.11" =>
          Seq("com.lihaoyi" % "ammonite" % "1.6.7" % Test cross CrossVersion.full)
        case "2.12" =>
          Seq("com.lihaoyi" % "ammonite" % "2.2.0" % Test cross CrossVersion.full)
        case "2.13" =>
          Seq("com.lihaoyi" % "ammonite" % "2.2.0" % Test cross CrossVersion.full)
        case _ =>
          Seq.empty[ModuleID]
      })
  , libraryDependencies :=
      crossVersionProps(List.empty, SemVer.parseUnsafe(scalaVersion.value)) {
        case (Major(2), Minor(10)) =>
          hedgehogLibs(hedgehogVersionFor2_10) ++
            libraryDependencies.value.filterNot(
              m => m.organization == "org.wartremover" && m.name == "wartremover"
            )
        case x =>
          hedgehogLibs(hedgehogVersion) ++
            libraryDependencies.value
      }
  , libraryDependencies := (
      if (isDotty.value) {
        libraryDependencies.value
          .filterNot(removeDottyIncompatible)
      }
      else
        (libraryDependencies).value
    )
  , libraryDependencies := libraryDependencies.value.map(_.withDottyCompat(scalaVersion.value))
  , sourceGenerators in Test +=
      (scalaBinaryVersion.value match {
        case "2.10" =>
          task(Seq.empty[File])
        case "2.11" | "2.12" | "2.13" =>
          task {
            val file = (sourceManaged in Test).value / "amm.scala"
            IO.write(file, """object amm extends App { ammonite.Main.main(args) }""")
            Seq(file)
          }
        case _ =>
          task(Seq.empty[File])
      })
  /* } Ammonite-REPL */
//  , wartremoverErrors in (Compile, compile) ++= commonWarts((scalaBinaryVersion in update).value)
//  , wartremoverErrors in (Test, compile) ++= commonWarts((scalaBinaryVersion in update).value)
//  , wartremoverErrors ++= commonWarts((scalaBinaryVersion in update).value)
  //      , wartremoverErrors ++= Warts.all
//  , Compile / console / wartremoverErrors := List.empty
  , Compile / console / scalacOptions := (console / scalacOptions).value.filterNot(_.contains("wartremover"))
//  , Test / console / wartremoverErrors := List.empty
  , Test / console / scalacOptions := (console / scalacOptions).value.filterNot(_.contains("wartremover"))
  , testFrameworks ++= Seq(TestFramework("hedgehog.sbt.Framework"))
  /* Bintray { */
  , bintrayPackageLabels := Seq("Scala", "Functional Programming", "FP")
  , bintrayVcsUrl := Some(s"""git@github.com:$GitHubUsername/$RepoName.git""")
  , licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
  /* } Bintray */

  , initialCommands in console :=
      """import just.fp._; import just.fp.syntax._"""

  /* Coveralls { */
  , coverageHighlighting := (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 10)) =>
      false
    case _ =>
      true
  })
  /* } Coveralls */
  )

//lazy val docDir = file("docs")
//lazy val docs = (project in docDir)
//  .enablePlugins(MicrositesPlugin)
//  .settings(noPublish)
//  .settings(
//    name := prefixedProjectName("docs")
//  /* microsites { */
//  , micrositeName := prefixedProjectName("")
//  , micrositeAuthor := "Kevin Lee"
//  , micrositeHomepage := "https://blog.kevinlee.io"
//  , micrositeDescription := "Just FP"
//  , micrositeGithubOwner := "Kevin-Lee"
//  , micrositeGithubRepo := "just-fp"
//  , micrositeBaseUrl := "/just-fp"
//  , micrositeDocumentationUrl := s"${micrositeBaseUrl.value}/docs"
//  , micrositePushSiteWith := GitHub4s
//  , micrositeGithubToken := sys.env.get("GITHUB_TOKEN")
////  , micrositeTheme := "pattern"
//  , micrositeHighlightTheme := "atom-one-light"
//  , micrositeGitterChannel := false
//  , micrositeGithubLinks := false
//  , micrositeShareOnSocial := false
//  , micrositeHighlightLanguages ++= Seq("shell")
//
//  , micrositeConfigYaml := ConfigYml(
//      yamlPath = Some(docDir / "microsite" / "_config.yml")
//    )
//  , micrositeImgDirectory := docDir / "microsite" / "img"
//  , micrositeCssDirectory := docDir / "microsite" / "css"
//  , micrositeSassDirectory := docDir / "microsite" / "sass"
//  , micrositeJsDirectory := docDir / "microsite" / "js"
//  , micrositeExternalLayoutsDirectory := docDir / "microsite" / "layouts"
//  , micrositeExternalIncludesDirectory := docDir / "microsite" / "includes"
//  , micrositeDataDirectory := docDir / "microsite" / "data"
//  , micrositeStaticDirectory := docDir / "microsite" / "static"
//  , micrositeExtraMdFilesOutput := docDir / "microsite" / "extra_md"
//  , micrositePluginsDirectory := docDir / "microsite" / "plugins"
//
//  /* } microsites */
//
//  )
//  .dependsOn(core)

lazy val docs = (project in file("generated-docs"))
  .enablePlugins(MdocPlugin, DocusaurPlugin)
  .settings(
      name := prefixedProjectName("docs")

    , docusaurDir := (ThisBuild / baseDirectory).value / "website"
    , docusaurBuildDir := docusaurDir.value / "build"

    , gitHubPagesOrgName := GitHubUsername
    , gitHubPagesRepoName := RepoName

    , libraryDependencies := (
        if (isDotty.value)
          libraryDependencies.value
            .filterNot(removeDottyIncompatible)
        else
          libraryDependencies.value
      )
    , libraryDependencies := libraryDependencies.value.map(_.withDottyCompat(scalaVersion.value))
  )
  .settings(noPublish)
  .dependsOn(core)

lazy val justFp = (project in file("."))
  .enablePlugins(DevOopsGitReleasePlugin)
  .settings(
    name := prefixedProjectName("")
  , description  := "Just FP Lib"
  , semanticdbEnabled := false
  )
  .settings(noPublish)
  .aggregate(core)
