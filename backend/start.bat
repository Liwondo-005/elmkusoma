@echo off
SET JAVA_HOME=C:\Program Files\Java\jdk-17.0.13+11
SET PATH=C:\Program Files\Java\jdk-17.0.13+11\bin;%PATH%
cd /d "%~dp0elmkusoma-core"
"C:\Program Files\Java\jdk-17.0.13+11\bin\java.exe" -Dmaven.multiModuleProjectDirectory="%~dp0elmkusoma-core" -classpath .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain spring-boot:run
