@echo off

set INSIGHTEDGE_VER=1.0.0

if "x%INSIGHTEDGE_HOME%"=="x" (
    set INSIGHTEDGE_HOME=%~dp0..
)

echo Installing InsightEdge %INSIGHTEDGE_VER% artifacts

call mvn install:install-file ^
    -Dpackaging=pom ^
    -Dfile=%INSIGHTEDGE_HOME%\tools\maven\poms\insightedge-package\pom.xml ^
    -DpomFile=%INSIGHTEDGE_HOME%\tools\maven\poms\insightedge-package\pom.xml

call mvn install:install-file ^
 -DgroupId=com.gigaspaces.insightedge ^
 -DcreateChecksum=true ^
 -DartifactId=insightedge-core ^
 -Dversion=%INSIGHTEDGE_VER% ^
 -DpomFile=%INSIGHTEDGE_HOME%\tools\maven\poms\insightedge-core\pom.xml ^
 -Dpackaging=jar ^
 -Dfile=%INSIGHTEDGE_HOME%\lib\insightedge-core-%INSIGHTEDGE_VER%.jar

call mvn install:install-file ^
 -DgroupId=com.gigaspaces.insightedge ^
 -DcreateChecksum=true ^
 -DartifactId=gigaspaces-scala ^
 -Dversion=%INSIGHTEDGE_VER% ^
 -DpomFile=%INSIGHTEDGE_HOME%\tools\maven\poms\gigaspaces-scala\pom.xml ^
 -Dpackaging=jar ^
 -Dfile=%INSIGHTEDGE_HOME%\lib\gigaspaces-scala-%INSIGHTEDGE_VER%.jar