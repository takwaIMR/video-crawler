name := "scala-indexer"

version := "1.0"

libraryDependencies += "org.json" % "json" % "20170516"
libraryDependencies += "com.hynnet" % "solr-solrj" % "5.3.1"
libraryDependencies += "commons-logging" % "commons-logging-api" % "1.1"
libraryDependencies += "org.mozilla" % "rhino" % "1.7.7.1"
libraryDependencies += "com.github.crawler-commons" % "crawler-commons" % "0.7"
libraryDependencies += "com.github.sebrichards" %% "postmark-scala" % "1.3"
libraryDependencies += "org.apache.opennlp" % "opennlp-tools" % "1.8.0"

libraryDependencies += "org.nd4j" % "nd4j-native-platform" % "0.8.0"
libraryDependencies += "org.nd4j" % "nd4s_2.11" % "0.8.0"
libraryDependencies += "org.deeplearning4j" % "deeplearning4j-core" % "0.8.0"
libraryDependencies += "org.deeplearning4j" % "deeplearning4j-nlp" % "0.8.0"
libraryDependencies += "gov.nih.imagej" % "imagej" % "1.47"
libraryDependencies += "com.twelvemonkeys.imageio" % "imageio-jpeg" % "3.3.2"
libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.6.7"

libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "1.11.235"
libraryDependencies += "com.amazonaws" % "aws-java-sdk-core" % "1.11.235"
libraryDependencies += "com.amazonaws" % "aws-java-sdk-emr" % "1.11.235"
libraryDependencies += "com.amazonaws" % "aws-java-sdk-s3" % "1.11.235"

libraryDependencies += "com.beust" % "jcommander" % "1.72"

libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "2.0.1"

resolvers += "jCenter" at "https://jcenter.bintray.com"

libraryDependencies += "net.dean.jraw" % "JRAW" % "0.9.0"
libraryDependencies += "org.rogach" %% "scallop" % "3.0.3"


scalaVersion := "2.11.11"

import com.github.retronym.SbtOneJar._

oneJarSettings
