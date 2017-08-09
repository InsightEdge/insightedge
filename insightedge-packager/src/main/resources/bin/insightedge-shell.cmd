@echo off
call %~dp0..\conf\insightedge-env.cmd
"%SPARK_HOME%\bin\spark-shell2.cmd" -i %~dp0shell-init.scala --driver-class-path=%INSIGHTEDGE_CORE_CP% %*