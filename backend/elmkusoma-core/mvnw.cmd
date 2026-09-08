@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM ----------------------------------------------------------------------------

@SETLOCAL
@SET "MAVEN_USER_HOME=%USERPROFILE%\.m2"
@SET "WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar"
@SET "WRAPPER_MAIN=org.apache.maven.wrapper.MavenWrapperMain"
@SET "PROJECT_DIR=%~dp0"

@REM Find Java
IF DEFINED JAVA_HOME (
    SET "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
) ELSE (
    SET "JAVA_CMD=java.exe"
)

@REM Check Java exists
@"%JAVA_CMD%" -version >NUL 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
    exit /b 1
)

@REM Download wrapper jar if missing
IF NOT EXIST "%WRAPPER_JAR%" (
    echo Downloading Maven Wrapper JAR...
    mkdir "%~dp0.mvn\wrapper" 2>NUL
    curl.exe -L -o "%WRAPPER_JAR%" "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
)

@REM Run Maven using arguments file to handle spaces in paths
@SET "MVNW_ARGS_FILE=%TEMP%\mvnw_args_%RANDOM%.txt"
@SET "MAVEN_CMD_LINE_ARGS=%*"
@"%JAVA_CMD%" -classpath "%WRAPPER_JAR%" "%WRAPPER_MAIN%" -Dmaven.multiModuleProjectDirectory="%PROJECT_DIR:"=\"%" %MAVEN_CMD_LINE_ARGS%
@SET "EXIT_CODE=%ERRORLEVEL%"

@IF EXIST "%MVNW_ARGS_FILE%" DEL "%MVNW_ARGS_FILE%"
@ENDLOCAL & EXIT /B %EXIT_CODE%
