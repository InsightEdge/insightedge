@echo off

setlocal enableextensions enabledelayedexpansion

if "x%INSIGHTEDGE_HOME%"=="x" (
  set INSIGHTEDGE_HOME=%~dp0..
)

rem add spark, datagrid and InsightEdge JARs to Zeppelin classpath
set SEPARATOR=";"
call "%INSIGHTEDGE_HOME%\sbin\common-insightedge.cmd" GET_LIBS ";"

for %%d in (%INSIGHTEDGE_HOME%\lib\spark-assembly-*.jar) do set ZEPPELIN_CLASSPATH_OVERRIDES=%INSIGHTEDGE_JARS%%SEPARATOR%%%d

call "%INSIGHTEDGE_HOME%/zeppelin/bin/zeppelin.cmd"