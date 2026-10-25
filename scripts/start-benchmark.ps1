param([ValidateSet('baseline','optimized')][string]$Mode='optimized')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
Set-Location $root
Write-Output '[1/4] Ensuring Redis and Kafka are running...'
& "$PSScriptRoot/start-local.ps1" -MiddlewareOnly
Write-Output '[2/4] Starting isolated benchmark application...'
if(Test-Path '.local/benchmark.pid'){
 $processId=[int](Get-Content '.local/benchmark.pid')
 $existing=Get-CimInstance Win32_Process -Filter "ProcessId=$processId" -ErrorAction SilentlyContinue
 if($existing -and $existing.CommandLine.Contains('--server.port=18082') -and $existing.CommandLine.Contains('qingju.jar')){Stop-Process -Id $processId;Start-Sleep -Seconds 1}
}
if(!(Test-Path '.local/benchmark-db.properties')){
 python -X utf8 scripts/prepare-boot-test.py
 if($LASTEXITCODE -ne 0){throw 'Cannot prepare isolated database'}
 Copy-Item '.local/test-db.properties' '.local/benchmark-db.properties'
 Copy-Item '.local/test-namespace.txt' '.local/benchmark-namespace.txt'
}
$ns=(Get-Content '.local/benchmark-namespace.txt').Trim()
$cache=if($Mode -eq 'baseline'){'false'}else{'true'}
$arguments="-Xmx512m -Duser.timezone=Asia/Shanghai -jar backend/target/qingju.jar --server.port=18082 --spring.config.additional-location=file:.local/benchmark-db.properties --qingju.namespace=$ns --qingju.cache-enabled=$cache"
$proc=Start-Process (Get-Command java.exe).Source -ArgumentList $arguments -WorkingDirectory $root -WindowStyle Hidden -PassThru -RedirectStandardOutput "$root\.local\logs\benchmark.out" -RedirectStandardError "$root\.local\logs\benchmark.err"
$proc.Id | Set-Content '.local/benchmark.pid'
$ready=$false
for($i=0;$i -lt 80;$i++){
 try{if((Invoke-RestMethod 'http://127.0.0.1:18082/actuator/health' -TimeoutSec 3).status -eq 'UP'){$ready=$true;break}}catch{}
 if($i % 10 -eq 0){Write-Output "Waiting for application health (attempt $i/80)..."}
 if($proc.HasExited){throw 'Benchmark app exited; see .local/logs/benchmark.out'}
 Start-Sleep -Milliseconds 500
}
if(!$ready){throw 'Benchmark health check failed; inspect .local/logs/benchmark.out and benchmark.err. Check MySQL, Redis and Kafka.'}
Write-Output '[3/4] Refreshing 40 Postman sessions (may take several seconds)...'
node scripts/prepare-postman.mjs
if($LASTEXITCODE -ne 0){throw 'Postman data preparation failed'}
Write-Output "[4/4] BENCHMARK_MODE=$Mode URL=http://127.0.0.1:18082"
