object Dependencies {

  lazy val V = new {
    val scala = "2.12.3"
  }

  lazy val commonDependencies = Seq()

  lazy val testDependencies = Seq()

  lazy val libDependencies = commonDependencies ++ testDependencies

  lazy val exampleDependencies = commonDependencies

}
