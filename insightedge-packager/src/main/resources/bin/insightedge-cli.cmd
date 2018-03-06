@echo off
SETLOCAL EnableDelayedExpansion
call %~dp0..\conf\insightedge-env.cmd

set INSIGHTEDGE_CLI_CP="%XAP_HOME%\tools\cli\*";"%XAP_HOME%\insightedge\lib\insightedge-cli.jar";%INSIGHTEDGE_CLASSPATH%
java %XAP_OPTIONS% -cp %INSIGHTEDGE_CLI_CP% org.insightedge.cli.commands.I9EMainCommand %*