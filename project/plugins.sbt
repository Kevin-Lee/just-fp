logLevel := sbt.Level.Warn

addSbtPlugin("com.geirsson"    % "sbt-ci-release"  % "1.5.7")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.4.13")
addSbtPlugin("org.scoverage"   % "sbt-scoverage"   % "1.6.1")
addSbtPlugin("org.scoverage"   % "sbt-coveralls"   % "1.2.7")
addSbtPlugin("io.kevinlee"     % "sbt-devoops"     % "2.2.0")
addSbtPlugin("org.scalameta"   % "sbt-mdoc"        % "2.2.19")
addSbtPlugin("io.kevinlee"     % "sbt-docusaur"    % "0.4.0")
addSbtPlugin("ch.epfl.lamp"    % "sbt-dotty"       % "0.5.4")
addSbtPlugin("ch.epfl.scala"   % "sbt-scalafix"    % "0.9.27")
