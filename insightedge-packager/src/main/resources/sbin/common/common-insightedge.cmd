@echo off

if "x%INSIGHTEDGE_HOME%"=="x" (
  set INSIGHTEDGE_HOME=%~dp0..
)

if not "%1"=="" goto %1

exit /b

:GET_LIBS
set SEPARATOR=%~2
set INSIGHTEDGE_JARS=
for %%d in (%INSIGHTEDGE_HOME%\lib\insightedge-core-*.jar) do set INSIGHTEDGE_JARS=!INSIGHTEDGE_JARS!%SEPARATOR%%%d
for %%d in (%INSIGHTEDGE_HOME%\lib\gigaspaces-scala-*.jar) do set INSIGHTEDGE_JARS=!INSIGHTEDGE_JARS!%SEPARATOR%%%d
set INSIGHTEDGE_JARS=%INSIGHTEDGE_JARS%%SEPARATOR%%INSIGHTEDGE_HOME%\datagrid\lib\required\*%SEPARATOR%
set INSIGHTEDGE_JARS=%INSIGHTEDGE_JARS%%SEPARATOR%%INSIGHTEDGE_HOME%\datagrid\lib\optional\spatial\*%SEPARATOR%
exit /b
