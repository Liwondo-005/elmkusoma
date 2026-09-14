Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
WshShell.CurrentDirectory = "C:\Users\manyusi\Desktop\Elmukusoma\backend\elmkusoma-core"

javaHome = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"

' Load .env from backend folder if it exists
envFile = "C:\Users\manyusi\Desktop\Elmukusoma\backend\.env"
dbUser = "elmkusoma"
dbPass = "changeme"

If fso.FileExists(envFile) Then
    Set f = fso.OpenTextFile(envFile, 1)
    Do While Not f.AtEndOfStream
        line = f.ReadLine
        line = Trim(line)
        If Left(line, 1) <> "#" And InStr(line, "=") > 0 Then
            parts = Split(line, "=", 2)
            key = Trim(parts(0))
            val = Trim(parts(1))
            If key = "DB_USERNAME" Then dbUser = val
            If key = "DB_PASSWORD" Then dbPass = val
        End If
    Loop
    f.Close
End If

cmd = "cmd /c ""set ""JAVA_HOME=" & javaHome & """ && set ""PATH=" & javaHome & "\bin;%PATH%"" && set ""DB_USERNAME=" & dbUser & """ && set ""DB_PASSWORD=" & dbPass & """ && java -jar target\elmkusoma-core-0.1.0-SNAPSHOT.jar > ..\spring-output.log 2> ..\spring-error.log"""
WshShell.Run cmd, 0, False
