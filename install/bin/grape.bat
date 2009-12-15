@if "%DEBUG%" == "" @echo off

@rem 
@rem $Revision: 13700 $ $Date: 2008-10-07 03:25:10 +0200 (Tue, 07 Oct 2008) $
@rem 

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

:begin
@rem Determine what directory it is in.
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.\

"%DIRNAME%\startGroovy.bat" "%DIRNAME%" org.codehaus.groovy.tools.GrapeMain %*

@rem End local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" endlocal