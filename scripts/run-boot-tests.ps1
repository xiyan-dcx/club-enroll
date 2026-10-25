$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
Set-Location $root
python -X utf8 scripts/prepare-boot-test.py
if($LASTEXITCODE -ne 0){throw 'Test database preparation failed'}
$ns=Get-Content .local/test-namespace.txt
$args="-Xmx512m -Duser.timezone=Asia/Shanghai -jar backend/target/qingju.jar --server.port=18081 --spring.config.additional-location=file:.local/test-db.properties --qingju.namespace=$ns"
$proc=Start-Process (Get-Command java.exe).Source -ArgumentList $args -WindowStyle Hidden -PassThru -RedirectStandardOutput "$root\.local\logs\test.out" -RedirectStandardError "$root\.local\logs\test.err" -WorkingDirectory $root
try{
 $ready=$false
 for($i=0;$i -lt 80;$i++){
  try{$r=Invoke-RestMethod 'http://127.0.0.1:18081/actuator/health';if($r.status -eq 'UP'){$ready=$true;break}}catch{}
  if($proc.HasExited){throw 'Test application exited; see .local/logs/test.out'}
  Start-Sleep -Milliseconds 500
 }
 if(!$ready){throw 'Test application not healthy'}
 node scripts/submission-test.mjs http://127.0.0.1:18081/api --allow-write
 if($LASTEXITCODE -ne 0){throw 'Portal tests failed'}
 node scripts/smoke-test.mjs http://127.0.0.1:18081/api --allow-write
 if($LASTEXITCODE -ne 0){throw 'Regression tests failed'}
 node scripts/infra-test.mjs http://127.0.0.1:18081/api --allow-write
 if($LASTEXITCODE -ne 0){throw 'Middleware tests failed'}
}finally{if(!$proc.HasExited){Stop-Process -Id $proc.Id}}
