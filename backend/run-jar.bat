@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "DB_USERNAME=elmkusoma"
set "DB_PASSWORD=elmkusoma"
set "JWT_SECRET=Y2hvb3NlYS1hLXNlY3VyZS1zZWNyZXQta2V5LWZvci1lbG1rdXNvbWEtand0LXRva2VuLWdlbmVyYXRpb24tMjAyNA=="
echo Starting ELMKUSOMA Backend on port 8080...
java -jar C:\Users\manyusi\Desktop\Elmukusoma\backend\elmkusoma-core\target\elmkusoma-core-0.1.0-SNAPSHOT.jar
pause
