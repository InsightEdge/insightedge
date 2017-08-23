@echo off

call %~dp0..\conf\insightedge-env.cmd


@echo on
echo "~~~~~ START I9E submit2 "
echo "--- dp [" %~dp0 "]"
echo "%SPARK_HOME%= [" %SPARK_HOME% "]"
echo "INSIGHTEDGE_CORE_CP= [" %INSIGHTEDGE_CORE_CP% "]"
echo "all args = [" %* "]"
@echo off


rem call %SPARK_HOME%\bin\spark-submit --driver-class-path=%INSIGHTEDGE_CORE_CP% %*
call %SPARK_HOME%\bin\spark-submit --driver-class-path=%INSIGHTEDGE_CORE_CP% --conf spark.executor.extraClassPath=%INSIGHTEDGE_CORE_CP% %*

