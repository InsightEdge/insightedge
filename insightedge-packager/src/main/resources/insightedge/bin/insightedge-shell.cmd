@echo off
call %~dp0..\conf\insightedge-env.cmd

rem SPARK_SUBMIT_OPTS adds -Dspark.driver.extraClassPath + -Dspark.executor.extraClassPath which the spark-class2 do not like
set SPARK_SUBMIT_OPTS=
"%SPARK_HOME%\bin\spark-shell2.cmd" --driver-class-path=%INSIGHTEDGE_CLASSPATH% -i %~dp0\shell-init.scala  %*