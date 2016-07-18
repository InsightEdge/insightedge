@echo off

rem This is the entry point for running Windows daemon. To avoid polluting the
rem environment, it just launches a new cmd to do the real work.

cmd /V /E /C %~dp0win-daemon2.cmd %*
