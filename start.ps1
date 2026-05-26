$env:JAVA_HOME = "C:\Users\user\.jdks\ms-17.0.18"
$env:PATH = "$env:JAVA_HOME\bin;C:\Users\user\AppData\Local\Microsoft\WinGet\Packages\Ngrok.Ngrok_Microsoft.Winget.Source_8wekyb3d8bbwe;$env:PATH"
$env:FIREBASE_STORAGE_BUCKET = "campus-market-35345.appspot.com"

# 서버 백그라운드 실행
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot'; `$env:JAVA_HOME='C:\Users\user\.jdks\ms-17.0.18'; `$env:PATH='C:\Users\user\.jdks\ms-17.0.18\bin;' + `$env:PATH; `$env:FIREBASE_STORAGE_BUCKET='campus-market-35345.appspot.com'; .\gradlew.bat bootRun"

# 서버 뜰 때까지 대기
Write-Host "서버 시작 중..."
Start-Sleep -Seconds 15

# ngrok 백그라운드 실행
Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$env:PATH='C:\Users\user\AppData\Local\Microsoft\WinGet\Packages\Ngrok.Ngrok_Microsoft.Winget.Source_8wekyb3d8bbwe;`$env:PATH'; ngrok http 8081"

# ngrok 주소 확인
Start-Sleep -Seconds 5
try {
    $tunnel = Invoke-RestMethod -Uri "http://localhost:4040/api/tunnels"
    $url = $tunnel.tunnels[0].public_url
    Write-Host ""
    Write-Host "============================="
    Write-Host "서버 + ngrok 실행 완료!"
    Write-Host "Flutter baseUrl: $url"
    Write-Host "============================="
} catch {
    Write-Host "ngrok 주소 확인 실패. http://localhost:4040 에서 직접 확인하세요."
}

Write-Host ""
Write-Host "종료하려면:"
Write-Host "  서버 종료 : Get-Process -Name java  | Stop-Process -Force"
Write-Host "  ngrok 종료: Get-Process -Name ngrok | Stop-Process -Force"
