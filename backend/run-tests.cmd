@echo off
setlocal

if not defined JAVA_HOME (
  for %%D in (
    "C:\Users\%USERNAME%\.jdks"
    "C:\Program Files\Java"
    "C:\Program Files\Microsoft"
    "C:\Program Files\JetBrains"
  ) do (
    if exist "%%~D" (
      for /r "%%~D" %%F in java.exe do (
        set "JAVA_HOME=%%~dpF.."
        goto :found_java
      )
    )
  )
)

:found_java
if not defined JAVA_HOME (
  echo No JDK found. Please install a JDK and set JAVA_HOME.
  exit /b 1
)

set "Path=%JAVA_HOME%\bin;%Path%"
echo Using JAVA_HOME=%JAVA_HOME%
call "%~dp0mvnw.cmd" test
exit /b %ERRORLEVEL%

