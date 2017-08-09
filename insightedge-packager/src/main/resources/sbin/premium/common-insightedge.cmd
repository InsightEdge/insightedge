

call %~dp0..\..\bin\setenv.bat



if not "%1"=="" goto %1

exit /b

rem populates INSIGHTEDGE_JARS variable with a list of InsightEdge jars
:GET_LIBS
  set SEPARATOR=%~2

  set INSIGHTEDGE_JARS=%XAP_HOME%\insightedge\lib\insightedge-core.jar
  REM get jars with * instead of full path
  REM set INSIGHTEDGE_JARS=!INSIGHTEDGE_JARS!%SEPARATOR%%XAP_HOME%\lib\required\*%SEPARATOR%
  REM set INSIGHTEDGE_JARS=!INSIGHTEDGE_JARS!%SEPARATOR%%XAP_HOME%\lib\optional\spatial\*%SEPARATOR%


  set INSIGHTEDGE_JARS_WILDCARDS=%XAP_HOME%\insightedge\lib\*
  set INSIGHTEDGE_JARS_WILDCARDS=!INSIGHTEDGE_JARS_WILDCARDS!%SEPARATOR%%XAP_HOME%\lib\required\*%SEPARATOR%
  set INSIGHTEDGE_JARS_WILDCARDS=!INSIGHTEDGE_JARS_WILDCARDS!%SEPARATOR%%XAP_HOME%\lib\optional\spatial\*%SEPARATOR%
exit /b

rem sets HADOOP_HOME if not specified by user - fixes error due to missing winutils
:SET_HADOOP_HOME
  if "x%HADOOP_HOME%"=="x" (
    set HADOOP_HOME=%XAP_HOME%\insightedge\winutils
  )
exit /b
