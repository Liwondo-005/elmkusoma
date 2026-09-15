$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "elmkusoma"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "gtwina @Tz#"
$env:JWT_SECRET = "Y2hvb3NlYS1hLXNlY3VyZS1zZWNyZXQta2V5LWZvci1lbG1rdXNvbWEtand0LXRva2VuLWdlbmVyYXRpb24tMjAyNA=="
Set-Location "C:\Users\dell\Desktop\elmkusoma\backend"
Start-Process -FilePath "C:\Program Files\Java\jdk-17.0.13+11\bin\java.exe" -ArgumentList "-Xmx256m", "-jar", "elmkusoma-core\target\elmkusoma-core-0.1.0-SNAPSHOT.jar" -WorkingDirectory "C:\Users\dell\Desktop\elmkusoma\backend" -WindowStyle Minimized
