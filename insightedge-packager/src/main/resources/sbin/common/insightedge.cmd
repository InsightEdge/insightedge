@echo off

rem This is the entry point for running Insightedge. To avoid polluting the
rem environment, it just launches a new cmd to do the real work.

cmd /V /E /C %~dp0insightedge2.cmd %*