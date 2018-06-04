@echo off
call %~dp0..\conf\insightedge-env.cmd

rem loop over arguments to find if in cluster deploy-mode
:loop

if [%1] == [] goto END_LOOP
if [%1] == [--deploy-mode] (

    if [%2] == [cluster] (
        rem In cluster mode, local env variables override remote machine env variables (spark JIRA SPARK-24456)
        rem To work around this behavior:
        rem 1. stop spark submit to source spark-env
        set SPARK_ENV_LOADED=1

        rem 2. Unset i9e local classpaths env variable
        set SPARK_DIST_CLASSPATH=

		goto END_LOOP
    )
)
shift
goto loop

:END_LOOP
call %SPARK_HOME%\bin\spark-submit %*