@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Begin all REM://
@echo off

@REM Set title of command window
title %0

@setlocal

set ERROR_CODE=0

@REM ==== START VALIDATION ====
if not "%JAVA_HOME%"=="" goto OkJHome
for %%i in (java.exe) do set "JAVACMD=%%~$PATH:i"
goto chkJCmd

:OkJHome
set "JAVACMD=%JAVA_HOME%\bin\java.exe"

:chkJCmd
if exist "%JAVACMD%" goto chkMHome

echo The JAVA_HOME environment variable is not defined correctly, >&2
echo this environment variable is needed to run this program. >&2
goto error

:chkMHome
set "MAVEN_PROJECTBASEDIR=%~dp0"

@REM Find the .mvn directory
:findBaseDir
set "WDIR=%MAVEN_PROJECTBASEDIR%"

:findBaseDirLoop
if exist "%WDIR%\.mvn" goto baseDirFound
cd ..
IF "%WDIR%"=="%CD%" goto baseDirNotFound
set "WDIR=%CD%"
goto findBaseDirLoop

:baseDirFound
set "MAVEN_PROJECTBASEDIR=%WDIR%"
cd "%MAVEN_PROJECTBASEDIR%"
goto endDetectBaseDir

:baseDirNotFound
set "MAVEN_PROJECTBASEDIR=%~dp0"
cd "%MAVEN_PROJECTBASEDIR%"
goto endDetectBaseDir

:endDetectBaseDir

set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"

@REM Extension to allow automatically downloading the maven-wrapper.jar from Maven-central
@REM This allows using the maven wrapper in projects that prohibit checking in binary data.
if exist "%WRAPPER_JAR%" (
    if "%MVNW_VERBOSE%"=="true" (
        echo Found %WRAPPER_JAR%
    )
) else (
    if not "%MVNW_REPOURL%"=="" (
        SET WRAPPER_URL="%MVNW_REPOURL%/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
    )
    if "%WRAPPER_URL%"=="" (
        SET WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
    )

    powershell -Command "&{"^
		"$webclient = new-object System.Net.WebClient;"^
		"if (-not ([string]::IsNullOrEmpty('%MVNW_USERNAME%') -and [string]::IsNullOrEmpty('%MVNW_PASSWORD%'))) {"^
		"$webclient.Credentials = new-object System.Net.NetworkCredential('%MVNW_USERNAME%', '%MVNW_PASSWORD%');"^
		"}"^
		"[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $webclient.DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%')"^
		"}"
    if "%MVNW_VERBOSE%"=="true" (
        echo Finished downloading %WRAPPER_JAR%
    )
)
@REM End of extension

@REM If specified, validate the SHA-256 sum of the Maven wrapper jar file
SET WRAPPER_SHA_256_SUM=""
FOR /F "usebackq tokens=1,2 delims=  " %%a IN ("%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties") DO (
    IF "%%a"=="wrapperSha256Sum" SET WRAPPER_SHA_256_SUM=%%b
)
IF NOT %WRAPPER_SHA_256_SUM%=="" (
    powershell -Command "&{"^
       "$hash = (Get-FileHash \"%WRAPPER_JAR%\" -Algorithm SHA256).Hash.ToLower();"^
       "If('%WRAPPER_SHA_256_SUM%' -ne $hash){"^
       "  Write-Output 'Error: Failed to validate Maven wrapper SHA-256, your Maven wrapper might be compromised.';"^
       "  Write-Output \"Investigating...\";"^
       "  exit 1;"^
       "}"^
       "}"
    if ERRORLEVEL 1 goto error
)

@REM Provide a "standardized" way to retrieve the CLI args that will
@REM work with both Windows and non-Windows executions.
set MAVEN_CMD_LINE_ARGS=%*

%JAVACMD% ^
  %MAVEN_OPTS% ^
  %MAVEN_DEBUG_OPTS% ^
  -classpath "%WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  org.apache.maven.wrapper.MavenWrapperMain %MAVEN_CMD_LINE_ARGS%
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%

cmd /C exit /B %ERROR_CODE%
