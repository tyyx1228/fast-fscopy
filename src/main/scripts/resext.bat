@echo off

set SCRIPT_DIR=%~dp0

set CONSOLE_PARAMS=%*

SET CLASSPATH=""
setlocal enabledelayedexpansion
for %%i in (%SCRIPT_DIR%\..\lib\*.jar) do set CLASSPATH=!CLASSPATH!;%%i

java -cp %CLASSPATH%  com.bigdata.FileSuffixRename %CONSOLE_PARAMS%