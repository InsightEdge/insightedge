stage 'Build'
env.PATH = "${tool 'maven-3.3.9'}/bin:$env.PATH"
env.PATH = "${tool 'sbt-0.13.11'}/bin:$env.PATH"
load 'tools/build.groovy'