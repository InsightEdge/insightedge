@echo off
SETLOCAL EnableDelayedExpansion
call %~dp0..\insightedge\conf\insightedge-env.cmd

set INSIGHTEDGE_CLI_CP="%XAP_HOME%\tools\cli\*";%INSIGHTEDGE_CLASSPATH%;%SIGAR_JARS%
%JAVACMD% %JAVA_OPTIONS% %XAP_OPTIONS% %XAP_CLI_OPTIONS% -cp %INSIGHTEDGE_CLI_CP% org.insightedge.cli.commands.I9EMainCommand %*