@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-17.0.13+11
set PATH=C:\Program Files\Java\jdk-17.0.13+11\bin;C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.16\bin;%PATH%
cd /d C:\Users\dell\Desktop\elmkusoma\backend\elmkusoma-core
mvn spring-boot:run
