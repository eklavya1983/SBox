name := "sbox"

version := "1.0"

lazy val `sbox` = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq( javaJdbc , javaJpa, cache , javaWs )

libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.7.2"

libraryDependencies += "org.hibernate" % "hibernate-entitymanager" % "4.3.6.Final"

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )