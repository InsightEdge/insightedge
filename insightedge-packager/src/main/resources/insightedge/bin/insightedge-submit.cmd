@echo off
call %~dp0..\conf\insightedge-env.cmd
call %SPARK_HOME%\bin\spark-submit %*