@echo off

rem This is the entry point for running InsightEdge submit. To avoid polluting the
rem environment, it just launches a new cmd to do the real work.

cmd /V /E /C %~dp0insightedge-submit2.cmd %*
