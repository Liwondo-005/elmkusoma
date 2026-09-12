@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "DB_USERNAME=elmkusoma"
set "DB_PASSWORD=elmkusoma"
set "JWT_SECRET=Y2hvb3NlYS1hLXNlY3VyZS1zZWNyZXQta2V5LWZvci1lbG1rdXNvbWEtand0LXRva2VuLWdlbmVyYXRpb24tMjAyNA=="
cd /d C:\Users\manyusi\Desktop\Elmukusoma\backend\elmkusoma-core
echo Starting ELMKUSOMA Backend...
mvnw.cmd spring-boot:run
pause
