Set WshShell = CreateObject("WScript.Shell")
WshShell.CurrentDirectory = "C:\Users\manyusi\Desktop\Elmukusoma\backend\elmkusoma-core"
WshShell.Run "cmd /c ""set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot&& set PATH=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin;%PATH%&& set DB_USERNAME=elmkusoma&& set DB_PASSWORD=elmkusoma&& java -jar target\elmkusoma-core-0.1.0-SNAPSHOT.jar > ..\spring-output.log 2> ..\spring-error.log""", 0, False
