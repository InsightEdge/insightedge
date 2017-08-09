@echo off
rem ***********************************************************************************************************
rem * This script is used to initialize common environment to GigaSpaces InsightEdge platform.                *
rem * It is highly recommended NOT TO MODIFY THIS SCRIPT, to simplify future upgrades.                        *
rem * If you need to override the defaults, please modify $XAP_HOME\bin\setenv-overrides.bat or set           *
rem * the XAP_SETTINGS_FILE environment variable to your custom script.                                       *
rem * For more information see http://docs.gigaspaces.com/xap/12.2/dev-java/common-environment-variables.html *
rem ***********************************************************************************************************
rem Source XAP environment:
call %~dp0..\..\bin\setenv.bat
rem Set InsightEdge defaults:
if not defined HADOOP_HOME set HADOOP_HOME=%XAP_HOME%\insightedge\winutils

set INSIGHTEDGE_CORE_CP=%XAP_HOME%\insightedge\lib\*