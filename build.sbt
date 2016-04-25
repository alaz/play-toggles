organization := "com.osinka.play"

name := "play-toggles"

homepage := Some(url("https://github.com/osinka/play-toggles"))

startYear := Some(2016)

licenses += "Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")

organizationName := "Osinka"

description := """Feature Toggles for Play Framework"""

scalaVersion in ThisBuild := "2.11.8"

libraryDependencies in ThisBuild ++= Seq(
  "com.typesafe.play" %% "play" % "2.3.10" % "provided"
)

scalacOptions ++= List("-deprecation", "-unchecked", "-feature")

resolvers += Resolver.typesafeRepo("releases")

credentials <+= (version) map { version: String =>
  val file =
    if (version.trim endsWith "SNAPSHOT") "credentials_osinka"
    else "credentials_sonatype"
  Credentials(Path.userHome / ".ivy2" / file)
}

pomIncludeRepository := { x => false }

publishTo <<= (version) { version: String =>
  Some(
    if (version.trim endsWith "SNAPSHOT")
      "Osinka Internal Repo" at "https://r.osinka.co/content/repositories/snapshots/"
    else
      "Sonatype OSS Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
  )
}

useGpg := true

pomExtra := <xml:group>
  <developers>
    <developer>
      <id>alaz</id>
      <email>azarov@osinka.com</email>
      <name>Alexander Azarov</name>
      <timezone>Europe/Riga</timezone>
    </developer>
  </developers>
  <scm>
    <connection>scm:git:git://github.com/osinka/play-toggles.git</connection>
    <developerConnection>scm:git:git@github.com:osinka/play-toggles.git</developerConnection>
    <url>http://github.com/osinka/play-toggles</url>
  </scm>
  <issueManagement>
    <system>github</system>
    <url>http://github.com/osinka/play-toggles/issues</url>
  </issueManagement>
</xml:group>

