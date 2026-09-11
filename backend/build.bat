@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d C:\Users\manyusi\Desktop\Elmukusoma\backend\elmkusoma-core
echo BUILD STARTED %DATE% %TIME% > C:\Users\manyusi\Desktop\Elmukusoma\backend\build.log
mvn package -DskipTests -Dmaven.test.skip=true -o >> C:\Users\manyusi\Desktop\Elmukusoma\backend\build.log 2>&1
echo BUILD EXIT CODE: %ERRORLEVEL% >> C:\Users\manyusi\Desktop\Elmukusoma\backend\build.log
echo BUILD FINISHED %DATE% %TIME% >> C:\Users\manyusi\Desktop\Elmukusoma\backend\build.log
