@echo off

call %~dp0..\conf\insightedge-env.cmd

rem call %SPARK_HOME%\bin\spark-submit --driver-class-path=%INSIGHTEDGE_CORE_CP% %*
call %SPARK_HOME%\bin\spark-submit --driver-class-path=%INSIGHTEDGE_CORE_CP% --conf spark.executor.extraClassPath=%INSIGHTEDGE_CORE_CP% %*

