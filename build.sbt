name := "splitthebill"

version := "1.0"

scalaVersion := "2.11.6"

scalacOptions += "-target:jvm-1.6"

javaHome := Some(file("c:\\dev\\jdk1.6.0_45"))

javacOptions ++= Seq("-source", "1.6", "-target", "1.6")




// Include sqlite4java support libs
classpathTypes ++= Set("so")

lazy val growlStart = taskKey[Unit]("Send Growl notification on start")

lazy val growlDone = taskKey[Unit]("Send Growl notification on done")

lazy val growlTestStart = taskKey[Unit]("Send Growl notification on start")

lazy val growlTestDone = taskKey[Unit]("Send Growl notification on done")

growlDone := { "c:\\users\\graham\\bin\\growlnotify /t:\"Android run\" \"Yup, we're done\"" !   }

growlStart := { "c:\\users\\graham\\bin\\growlnotify /t:\"Android run\" \"Detected source change\"" !   }

growlTestDone := { "c:\\users\\graham\\bin\\growlnotify /t:\"Test compile\" \"Yup, we're done\"" !   }

growlTestStart := { "c:\\users\\graham\\bin\\growlnotify /t:\"Test compile\" \"Detected source change\"" !   }

pollInterval := 300

scalaSource in Test := baseDirectory.value / "test"



proguardOptions in Android ++= Seq(
  "-keepattributes Signature",
    "-dontwarn com.google.android.gms.**",
    "-dontnote com.google.android.gms.**"
)

resolvers += Resolver.mavenLocal

resolvers += "RoboTest releases" at "https://raw.github.com/zbsz/mvn-repo/master/releases/"

resolvers += "Robolectric releases" at "https://oss.sonatype.org/content/repositories/snapshots"

//https://groups.google.com/forum/#!topic/scala-on-android/Aw1doNZl_cI
// Required by Robolectric
fork in Test := true

// Required by Robolectric. I think, docs unclear.
//parallelExecution in Test := false

javaOptions in Test ++= Seq("-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled")   // usually needed due to using forked tests

// Duplicated in some test libs
apkbuildExcludes in Android += "META-INF/LICENSE"

apkbuildExcludes in Android += "LICENSE.txt"

apkbuildExcludes in Android += "org/apache/maven/project/pom-4.0.0.xml"

apkbuildExcludes in Android += "org/cyberneko/html/res/ErrorMessages.properties"

apkbuildExcludes in Android += "org/cyberneko/html/res/ErrorMessages_ja.properties"

apkbuildExcludes in Android += "org/cyberneko/html/res/ErrorMessages_ja.txt"

apkbuildExcludes in Android += "org/cyberneko/html/res/HTMLlat1.properties"

apkbuildExcludes in Android += "org/cyberneko/html/res/HTMLspecial.properties"

apkbuildExcludes in Android += "org/cyberneko/html/res/HTMLsymbol.properties"

apkbuildExcludes in Android += "org/cyberneko/html/res/XMLbuiltin.properties"

apkbuildExcludes in Android += "licenses/extreme.indiana.edu.license.TXT"

apkbuildExcludes in Android += "licenses/javolution.license.TXT"

apkbuildExcludes in Android += "licenses/thoughtworks.TXT"

apkbuildExcludes in Android += "META-INF/DEPENDENCIES"

libraryDependencies ++= Seq(
  aar("com.google.android.gms" % "play-services-lib" % "7.5.0"),
  aar("com.google.android.gms" % "play-services-ads" % "7.5.0"),
  "org.robolectric" % "android-all" % "5.0.0_r2-robolectric-0" % Test,  // android version used by Robolectric 2.4
  "org.robolectric" % "robolectric" % "2.4" % Test,
  "com.android.support" % "support-v4" % "19.0.1" % Test,
  "junit" % "junit" % "4.8.2" % Test,                                     // required by Robolectric 2.3
  "com.geteit" %% "robotest" % "0.7" % Test,                              // 0.7 for Robo 2.4, 0.8 for 3.0
  "org.scalatest" %% "scalatest" % "2.1.6" % Test,
  "cglib" % "cglib-nodep" % "3.1" % Test,
  "org.assertj" % "assertj-core" % "1.7.1" % Test,
  "org.codehaus.groovy" % "groovy-all" % "2.4.1" % Test       // purely for PowerAssert in tests
)


val classVersion = sys.props("java.class.version")
val specVersion = sys.props("java.specification.version")
