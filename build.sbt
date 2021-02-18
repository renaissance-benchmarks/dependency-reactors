import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val reactorsScalaVersion = "2.12.13"
val scalaTestVersion = "3.1.4"
val scalaCheckVersion = "1.13.4"
val akkaVersion = "2.6.12"
val scalaMeterVersion = "0.19"

def projectSettings(suffix: String) = {
  Seq(
    name := s"reactors$suffix",
    organization := "io.reactors",
    scalaVersion := reactorsScalaVersion,
    logBuffered := false,
    scalacOptions ++= Seq(
      "-deprecation", "-feature"
    ),
    Compile / doc / scalacOptions ++= Seq(
      "-implicits"
    ),

    Test / fork := true,
    Test / parallelExecution := false,

    Test / testOptions += Tests.Argument(
      TestFrameworks.ScalaCheck,
      "-minSuccessfulTests", "200",
      "-workers", "1",
      "-verbosity", "2"
    ),

    Test / publishArtifact := false,

    Global / concurrentRestrictions += Tags.limit(Tags.Test, 1),
    Global / cancelable := true,

    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases",
      "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"
    ),

    ThisBuild / parallelExecution := false
  )
}


def jvmProjectSettings(suffix: String) =
  Seq(
    Test / javaOptions ++= Seq(
      "-Xmx3G",
      "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
    ),
  )


def gitPropsContents(dir: File, baseDir: File): Seq[File] = {
  def run(cmd: String*): String = scala.sys.process.Process(cmd, Some(baseDir)).!!
  val branch = run("git", "rev-parse", "--abbrev-ref", "HEAD").trim
  val commitTs = run("git", "--no-pager", "show", "-s", "--format=%ct", "HEAD")
  val sha = run("git", "rev-parse", "HEAD").trim
  val contents = s"""
  {
    "branch": "$branch",
    "commit-timestamp": $commitTs,
    "sha": "$sha"
  }
  """
  val file = dir / "reactors-io" / ".gitprops"
  IO.write(file, contents)
  Seq(file)
}


// Produces reactorsCommonJVM

lazy val reactorsCommon = crossProject(JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("reactors-common"))
  .settings(
    projectSettings("-common") ++ Seq(
      libraryDependencies ++= Seq(
        "org.scalatest" %%% "scalatest" % scalaTestVersion % "test",
        "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % "test"
      ),
      unmanagedSourceDirectories in Compile +=
        baseDirectory.value.getParentFile / "shared" / "src" / "main" / "scala",
      unmanagedSourceDirectories in Test +=
        baseDirectory.value.getParentFile / "shared" / "src" / "test" / "scala"
    ): _*
  )
  .jvmSettings(
    jvmProjectSettings("-common") ++ Seq(
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % akkaVersion % "test",
        "com.storm-enroute" %% "scalameter" % scalaMeterVersion % "test"
      )
    ): _*
  )


// Produces reactorsCoreJVM 

lazy val reactorsCore = crossProject(JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("reactors-core"))
  .settings(
    projectSettings("-core") ++ Seq(
      Compile / resourceGenerators += Def.task {
        gitPropsContents((Compile / resourceManaged).value, baseDirectory.value)
      },
      libraryDependencies ++= Seq(
        "org.scalatest" %%% "scalatest" % scalaTestVersion % "test",
        "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % "test"
      ),
      Compile / unmanagedSourceDirectories +=
        baseDirectory.value.getParentFile / "shared" / "src" / "main" / "scala",
      Test / unmanagedSourceDirectories +=
        baseDirectory.value.getParentFile / "shared" / "src" / "test" / "scala"
    ): _*
  )
  .jvmSettings(
    jvmProjectSettings("-core") ++ Seq(
      libraryDependencies ++= Seq(
        "com.typesafe" % "config" % "1.2.1",
        "com.typesafe.akka" %% "akka-actor" % akkaVersion % "test",
        "com.storm-enroute" %% "scalameter" % scalaMeterVersion % "test"
      )
    ): _*
  )
  .dependsOn(reactorsCommon % "compile->compile;test->test")


// Produces reactorsContainerJVM

lazy val reactorsContainer = crossProject(JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("reactors-container"))
  .settings(
    projectSettings("-container") ++ Seq(
      libraryDependencies ++= Seq(
        "org.scalatest" %%% "scalatest" % scalaTestVersion % "test",
        "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % "test"
      ),
      unmanagedSourceDirectories in Compile +=
        baseDirectory.value.getParentFile / "shared" / "src" / "main" / "scala",
      unmanagedSourceDirectories in Test +=
        baseDirectory.value.getParentFile / "shared" / "src" / "test" / "scala"
    ): _*
  )
  .jvmSettings(
    jvmProjectSettings("-container") ++ Seq(
      libraryDependencies ++= Seq(
        "com.storm-enroute" %% "scalameter" % scalaMeterVersion % "test"
      )
    ): _*
  )
  .dependsOn(
    reactorsCore % "compile->compile;test->test"
  )


// Produces reactorsProtocolJVM

lazy val reactorsProtocol = crossProject(JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("reactors-protocol"))
  .settings(
    projectSettings("-protocol") ++ Seq(
      libraryDependencies ++= Seq(
        "org.scalatest" %%% "scalatest" % scalaTestVersion % "test",
        "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % "test"
      ),
      unmanagedSourceDirectories in Compile +=
        baseDirectory.value.getParentFile / "shared" / "src" / "main" / "scala",
      unmanagedSourceDirectories in Test +=
        baseDirectory.value.getParentFile / "shared" / "src" / "test" / "scala"
    ): _*
  )
  .jvmSettings(
    jvmProjectSettings("-protocol"): _*
  )
  .dependsOn(
    reactorsCommon % "compile->compile;test->test",
    reactorsCore % "compile->compile;test->test",
    reactorsContainer % "compile->compile;test->test"
  )


lazy val root = Project("root", file("."))
  .aggregate(
    reactorsCommon.jvm,
    reactorsCore.jvm,
    reactorsContainer.jvm,
    reactorsProtocol.jvm
  )
