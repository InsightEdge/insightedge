@echo off
call %~dp0..\conf\insightedge-env.cmd

:loop
set key=%1

if [%key%] == [] goto EOF
if [%key%] == [--deploy-mode] (
	set value=%2

    if [%value%] == [cluster] (

        rem 1. stop spark submit to source spark-env
        set SPARK_ENV_LOADED=1

        rem 2. empty i9e local classpaths env variable
        set SPARK_DIST_CLASSPATH=

		goto EOF
    )
)
shift
goto loop

:EOF
call %SPARK_HOME%\bin\spark-submit %*