@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Maven Wrapper - Windows Batch Script
@REM Lädt Maven automatisch herunter falls nicht vorhanden.

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0") ELSE (SET "BASE_DIR=%__MVNW_ARG0_NAME__%")
@SET MAVEN_WRAPPER_JAR="%BASE_DIR%.mvn\wrapper\maven-wrapper.jar"
@SET MAVEN_WRAPPER_PROPERTIES="%BASE_DIR%.mvn\wrapper\maven-wrapper.properties"
@SET DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

@FOR /F "usebackq tokens=1,2 delims==" %%A IN (%MAVEN_WRAPPER_PROPERTIES%) DO (
    @IF "%%A"=="distributionUrl" (SET DISTRIBUTION_URL=%%B)
)

@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@SET DOWNLOAD_FROM_URL=%DOWNLOAD_URL%
@IF EXIST %MAVEN_WRAPPER_JAR% (
    @SET DOWNLOAD_FROM_URL=
)

@IF NOT "%DOWNLOAD_FROM_URL%"=="" (
    @SET JAVA_HOME_CANDIDATE=%JAVA_HOME%
    @IF "%JAVA_HOME_CANDIDATE%"=="" (
        @FOR /F "tokens=*" %%j IN ('where java 2^>NUL') DO (
            @SET JAVA_HOME_CANDIDATE=%%~dpj..
            @GOTO :found_java
        )
    )
    :found_java
    @"%JAVA_HOME_CANDIDATE%\bin\java.exe" -classpath "" ^
        "-Dmaven.wrapper.jarPath=%MAVEN_WRAPPER_JAR%" ^
        "-Dmaven.wrapper.distributionUrl=%DISTRIBUTION_URL%" ^
        %WRAPPER_LAUNCHER% %* 2>NUL

    @IF NOT EXIST %MAVEN_WRAPPER_JAR% (
        @powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DOWNLOAD_FROM_URL%' -OutFile '%MAVEN_WRAPPER_JAR%'"
    )
)

@SET JAVA_CMD=java
@IF NOT "%JAVA_HOME%"=="" @SET "JAVA_CMD=%JAVA_HOME%\bin\java"

@"%JAVA_CMD%" ^
  -classpath "%MAVEN_WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%BASE_DIR%" ^
  %WRAPPER_LAUNCHER% %*
