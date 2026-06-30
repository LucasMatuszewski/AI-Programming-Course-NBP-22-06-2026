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

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup script, version 3.3.2
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0")

@SET MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%
@IF "%MAVEN_PROJECTBASEDIR%"=="" (SET "MAVEN_PROJECTBASEDIR=%BASE_DIR%")
@IF NOT "%MAVEN_PROJECTBASEDIR:~-1%"=="\" (SET "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR%\")

@SET WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@SET WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

@FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties") DO (
    @IF "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
)

@IF NOT EXIST %WRAPPER_JAR% (
    @IF NOT "%MVNW_REPOURL%"=="" SET WRAPPER_URL="%MVNW_REPOURL%/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
    @echo Downloading %WRAPPER_URL% to %WRAPPER_JAR%
    @java -classpath %WRAPPER_JAR% org.apache.maven.wrapper.MavenWrapperDownloader %WRAPPER_URL% %WRAPPER_JAR%
)

@SET MAVEN_JAVA_EXE="%JAVA_HOME%\bin\java.exe"
@IF NOT EXIST %MAVEN_JAVA_EXE% (
    @SET MAVEN_JAVA_EXE=java.exe
)

@IF "%MAVEN_OPTS%"=="" SET "MAVEN_OPTS=-Xmx512m"

@%MAVEN_JAVA_EXE% %MAVEN_OPTS% ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  -classpath %WRAPPER_JAR% %WRAPPER_LAUNCHER% %*
