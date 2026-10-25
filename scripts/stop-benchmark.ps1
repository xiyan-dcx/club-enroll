$root=Split-Path $PSScriptRoot -Parent
$file=Join-Path $root '.local/benchmark.pid'
if(Test-Path $file){
 $processId=[int](Get-Content $file)
 $existing=Get-CimInstance Win32_Process -Filter "ProcessId=$processId" -ErrorAction SilentlyContinue
 if($existing -and $existing.CommandLine.Contains('--server.port=18082') -and $existing.CommandLine.Contains('qingju.jar')){Stop-Process -Id $processId;Write-Output 'Benchmark stopped'}
}
