@echo off
call %~dp0..\conf\insightedge-env.cmd

rem Because of SPARK-21642, the driver url is composed from hostname instead of ip. This is a workaround - setting SPARK_LOCAL_HOSTNAME to the submitting machine IP
if not defined SPARK_LOCAL_HOSTNAME set SPARK_LOCAL_HOSTNAME=%XAP_NIC_ADDRESS%

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