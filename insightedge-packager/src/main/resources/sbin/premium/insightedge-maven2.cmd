@echo off

call %~dp0..\..\bin\setenv.bat



echo Installing InsightEdge %INSIGHTEDGE_VER% artifacts

call mvn install:install-file ^
    -Dpackaging=pom ^
    -Dfile=%XAP_HOME%\insightedge\tools\maven\poms\insightedge-package\pom.xml ^
    -DpomFile=%XAP_HOME%\insightedge\tools\maven\poms\insightedge-package\pom.xml

call mvn install:install-file ^
 -DgroupId=org.gigaspaces.insightedge ^
 -DcreateChecksum=true ^
 -DartifactId=insightedge-core ^
 -DpomFile=%XAP_HOME%\insightedge\tools\maven\poms\insightedge-core\pom.xml ^
 -Dpackaging=jar ^
 -Dfile=%XAP_HOME%\insightedge\lib\insightedge-core.jar

call mvn install:install-file ^
 -DgroupId=org.gigaspaces.insightedge ^
 -DcreateChecksum=true ^
 -DartifactId=insightedge-scala ^
 -DpomFile=%XAP_HOME%\insightedge\tools\maven\poms\insightedge-scala\pom.xml ^
 -Dpackaging=jar ^
 -Dfile=%XAP_HOME%\insightedge\lib\insightedge-scala.jar

rem Install spring.aopalliance to local maven repo (fixes SBT builds)
call mvn dependency:get ^
 -Dartifact=org.aopalliance:com.springsource.org.aopalliance:1.0.0 ^
 -DremoteRepositories=http://repository.springsource.com/maven/bundles/external/
