@echo off



rem disable randomized hash for string in Python 3.3+
set PYTHONHASHSEED=0

rem build INSIGHTEDGE_JARS string
set INSIGHTEDGE_JARS=
call "%XAP_HOME%\insightedge\sbin\common-insightedge.cmd" GET_LIBS ","

call "%XAP_HOME%\insightedge\sbin\common-insightedge.cmd" SET_HADOOP_HOME

rem do not remove empty lines after "set NEWLINE=^"!
set NEWLINE=^


set ARGUMENTS=
set APPENDED_JARS=false
set USER_ARGUMENTS=%*
rem this replaces space with newlines and cycles through the arguments
for /f %%i in ("%USER_ARGUMENTS: =!NEWLINE!%") do (
  if "x%%i" == "x--jars" (
    set ARGUMENTS=!ARGUMENTS!--jars %INSIGHTEDGE_JARS%,
	set APPENDED_JARS=true
  ) else (
	set ARGUMENTS=!ARGUMENTS!%%i 
  )
)
if "%APPENDED_JARS%" == "false" (
  rem pyspark-shell arguments order is different
  set INSIGHTEDGE_CLASSPATH=%INSIGHTEDGE_JARS_WILDCARDS:,=;%
  if "%1" == "pyspark-shell-main" (
    set ARGUMENTS=!ARGUMENTS! --jars %INSIGHTEDGE_JARS% --conf spark.driver.extraClassPath=!INSIGHTEDGE_CLASSPATH! --conf spark.executor.extraClassPath=!INSIGHTEDGE_CLASSPATH!
  ) else (
    set ARGUMENTS=--jars %INSIGHTEDGE_JARS% --conf spark.driver.extraClassPath=!INSIGHTEDGE_CLASSPATH! --conf spark.executor.extraClassPath=!INSIGHTEDGE_CLASSPATH! !ARGUMENTS!
  )
)

set CLASS=org.apache.spark.deploy.SparkSubmit
 spark-class2.cmd %CLASS% %ARGUMENTS%
