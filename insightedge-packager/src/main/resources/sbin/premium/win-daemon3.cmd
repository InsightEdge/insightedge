@echo off

rem Runs a Windows command and redirects output to a file

for /f "tokens=1,*" %%a in ("%*") do (
	set LOG_FILE=%%a
	set COMMAND=%%b
)

%COMMAND% > %LOG_FILE% 2>&1