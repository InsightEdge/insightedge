@echo off
call %~dp0..\conf\insightedge-env.cmd
call %SPARK_HOME%\bin\spark-submit --driver-class-path=%INSIGHTEDGE_CLASSPATH% --conf spark.executor.extraClassPath=%INSIGHTEDGE_CLASSPATH% %*