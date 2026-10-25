param([switch]$IncludeMiddleware)
$root=Split-Path $PSScriptRoot -Parent
$names=@('app');if($IncludeMiddleware){$names+=@('kafka','redis')}
foreach($name in $names){
 $file=Join-Path $root ".local\$name.pid"
 if(Test-Path $file){
  $processId=[int](Get-Content $file)
  $process=Get-CimInstance Win32_Process -Filter "ProcessId=$processId" -ErrorAction SilentlyContinue
  $expected=if($name -eq 'app'){'qingju.jar'}elseif($name -eq 'kafka'){'kafka.Kafka'}else{'redis.conf'}
  if($process -and $process.CommandLine.Contains($expected)){Stop-Process -Id $processId;Write-Output "Stopped $name ($processId)"}
 }
}
