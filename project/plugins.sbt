logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.14")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.8.0")
