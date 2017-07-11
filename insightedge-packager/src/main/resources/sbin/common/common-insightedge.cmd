@echo off

if "x%INSIGHTEDGE_HOME%"=="x" (
  set INSIGHTEDGE_HOME=%~dp0..
)

if not "%1"=="" goto %1

exit /b

rem populates INSIGHTEDGE_JARS variable with a list of InsightEdge jars
:GET_LIBS
  set SEPARATOR=%~2

  set INSIGHTEDGE_JARS=%INSIGHTEDGE_HOME%\lib\insightedge-core.jar%SEPARATOR%%INSIGHTEDGE_HOME%\lib\insightedge-scala.jar
  for %%d in (%INSIGHTEDGE_HOME%\datagrid\lib\required\*) do set INSIGHTEDGE_JARS=!INSIGHTEDGE_JARS!%SEPARATOR%%%d
  for %%d in (%INSIGHTEDGE_HOME%\datagrid\lib\optional\spatial\*) do set INSIGHTEDGE_JARS=!INSIGHTEDGE_JARS!%SEPARATOR%%%d

  set INSIGHTEDGE_JARS_WILDCARDS=%INSIGHTEDGE_HOME%\lib\*
  set INSIGHTEDGE_JARS_WILDCARDS=!INSIGHTEDGE_JARS_WILDCARDS!%SEPARATOR%%INSIGHTEDGE_HOME%\datagrid\lib\required\*%SEPARATOR%
  set INSIGHTEDGE_JARS_WILDCARDS=!INSIGHTEDGE_JARS_WILDCARDS!%SEPARATOR%%INSIGHTEDGE_HOME%\datagrid\lib\optional\spatial\*%SEPARATOR%
exit /b

rem sets HADOOP_HOME if not specified by user - fixes error due to missing winutils
:SET_HADOOP_HOME
  if "x%HADOOP_HOME%"=="x" (
    set HADOOP_HOME=%INSIGHTEDGE_HOME%\winutils
  )
exit /b
