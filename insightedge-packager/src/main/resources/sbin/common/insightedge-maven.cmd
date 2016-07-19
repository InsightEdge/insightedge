@echo off

set INSIGHTEDGE_VER=0.4.0-SNAPSHOT

if "x%INSIGHTEDGE_HOME%"=="x" (
    set INSIGHTEDGE_HOME=%~dp0..
)

set XAP_HOME=%INSIGHTEDGE_HOME%\datagrid
echo "Data Grid home %XAP_HOME%"

set POMS_DIR=%XAP_HOME%\tools\maven\poms
set XAP_DATAGRID_POMS_DIR=%POMS_DIR%\xap-datagrid
set XAP_DATAGRID_CORE_POMS=%XAP_DATAGRID_POMS_DIR%\xap-core
set XAP_DATAGRID_EXT_POMS=%XAP_DATAGRID_POMS_DIR%\xap-extensions

echo "Installing Data Grid %DATA_GRID_VERSION% artifacts"

rem GigaSpaces Jars
call mvn install:install-file -DcreateChecksum=true -DpomFile=%XAP_DATAGRID_POMS_DIR%\pom.xml -Dfile=%XAP_DATAGRID_POMS_DIR%\pom.xml

rem open core modules
call mvn install:install-file -DcreateChecksum=true -DpomFile=%XAP_DATAGRID_CORE_POMS%\xap-common\pom.xml -Dfile=%XAP_HOME%\lib\required\xap-common.jar
call mvn install:install-file -DcreateChecksum=true -DpomFile=%XAP_DATAGRID_CORE_POMS%\xap-trove\pom.xml -Dfile=%XAP_HOME%\lib\required\xap-trove.jar
call mvn install:install-file -DcreateChecksum=true -DpomFile=%XAP_DATAGRID_CORE_POMS%\xap-asm\pom.xml -Dfile=%XAP_HOME%\lib\required\xap-asm.jar
call mvn install:install-file -DcreateChecksum=true -DpomFile=%XAP_DATAGRID_CORE_POMS%\xap-datagrid\pom.xml -Dfile=%XAP_HOME%\lib\required\xap-datagrid.jar
call mvn install:install-file -DcreateChecksum=true -DpomFile=%XAP_DATAGRID_CORE_POMS%\xap-openspaces\pom.xml -Dfile=%XAP_HOME%\lib\required\xap-openspaces.jar
call mvn install:install-file -DcreateChecksum=true -DpomFile=%XAP_DATAGRID_EXT_POMS%\xap-jms\pom.xml -Dfile=%XAP_HOME%\lib\optional\jms\xap-jms.jar
call mvn install:install-file -DcreateChecksum=true -DpomFile=%XAP_DATAGRID_EXT_POMS%\xap-spatial\pom.xml -Dfile=%XAP_HOME%\lib\optional\spatial\xap-spatial.jar


echo "Installing InsightEdge %INSIGHTEDGE_VER% artifacts"

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