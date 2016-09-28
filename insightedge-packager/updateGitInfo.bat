@echo off
mkdir target
set SHA=
for /f "delims=" %%a in ('git rev-parse HEAD') do @set SHA=%%a
echo InsightEdge:https://github.com/InsightEdge/insightedge/commit/%SHA% > target\metadata.txt