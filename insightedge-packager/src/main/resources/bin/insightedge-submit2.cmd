@echo off

call %~dp0..\conf\insightedge-env.cmd

call %SPARK_HOME%\bin\spark-submit --driver-class-path=%INSIGHTEDGE_CORE_CP%  %*