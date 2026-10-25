param([int]$AppPort=18080,[string]$RedisExe='D:\Redis-x64-5.0.10\redis-server.exe',[switch]$MiddlewareOnly)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
Set-Location $root
$runtime=Join-Path $root '.local'
$utf8=New-Object Text.UTF8Encoding($false)
New-Item -ItemType Directory -Force "$runtime\logs","$runtime\redis-data","$runtime\kafka-data" | Out-Null
function Listening([int]$port) {
 $c=New-Object Net.Sockets.TcpClient
 try{$c.Connect('127.0.0.1',$port);return $true}catch{return $false}finally{$c.Dispose()}
}
function Wait-Port([int]$port) {for($i=0;$i -lt 60;$i++){if(Listening $port){return};Start-Sleep -Milliseconds 500};throw "Port $port did not become ready; see .local/logs"}
$java=(Get-Command java.exe).Source
$kafka=Join-Path $runtime 'kafka_2.13-4.1.1'
if(!(Test-Path "$kafka\libs")){throw 'Download and verify Kafka 4.1.1 before starting'}
if(!(Listening 16379)) {
 Write-Output 'Starting Redis on 16379...'
 $redisConfig="bind 127.0.0.1`nport 16379`nprotected-mode yes`nappendonly yes`nappendfsync everysec`ndir ./redis-data`n"
 [IO.File]::WriteAllText("$runtime\redis.conf",$redisConfig,$utf8)
 $proc=Start-Process $RedisExe -ArgumentList 'redis.conf' -WorkingDirectory $runtime -WindowStyle Hidden -PassThru -RedirectStandardOutput "$runtime\logs\redis.out" -RedirectStandardError "$runtime\logs\redis.err"
 $proc.Id | Set-Content "$runtime\redis.pid"
 Wait-Port 16379
}
if(!(Listening 19092)) {
 Write-Output 'Starting Kafka on 19092...'
 $configuration=@'
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@127.0.0.1:19093
listeners=PLAINTEXT://127.0.0.1:19092,CONTROLLER://127.0.0.1:19093
advertised.listeners=PLAINTEXT://127.0.0.1:19092
listener.security.protocol.map=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
controller.listener.names=CONTROLLER
inter.broker.listener.name=PLAINTEXT
log.dirs=.local/kafka-data
offsets.topic.replication.factor=1
transaction.state.log.replication.factor=1
transaction.state.log.min.isr=1
group.initial.rebalance.delay.ms=0
num.partitions=3
log.retention.hours=168
'@
 [IO.File]::WriteAllText("$runtime\kafka.properties",$configuration,$utf8)
 if(!(Test-Path "$runtime\kafka-data\meta.properties")){
  & $java -cp "$kafka\libs\*" kafka.tools.StorageTool format -t 'MkU3OEVBNTcwNTJENDM2Qk' -c "$runtime\kafka.properties"
  if($LASTEXITCODE -ne 0){throw 'Kafka storage format failed'}
 }
 $args="-Xms256m -Xmx512m -cp `"$kafka\libs\*`" kafka.Kafka `"$runtime\kafka.properties`""
 $proc=Start-Process $java -ArgumentList $args -WorkingDirectory $root -WindowStyle Hidden -PassThru -RedirectStandardOutput "$runtime\logs\kafka.out" -RedirectStandardError "$runtime\logs\kafka.err"
 $proc.Id | Set-Content "$runtime\kafka.pid"
 Wait-Port 19092
}
if($MiddlewareOnly){Write-Output 'Middleware ports ready';return}
if(Listening $AppPort){throw "App port $AppPort is already occupied; stop the existing app before restarting"}
$args="-Xms128m -Xmx512m -Duser.timezone=Asia/Shanghai -jar backend/target/qingju.jar --server.port=$AppPort"
$proc=Start-Process $java -ArgumentList $args -WorkingDirectory $root -WindowStyle Hidden -PassThru -RedirectStandardOutput "$runtime\logs\app.out" -RedirectStandardError "$runtime\logs\app.err"
$proc.Id | Set-Content "$runtime\app.pid"
Wait-Port $AppPort
$healthy=$false
for($i=0;$i -lt 60;$i++){
 try{if((Invoke-RestMethod "http://127.0.0.1:$AppPort/actuator/health").status -eq 'UP'){$healthy=$true;break}}catch{}
 Start-Sleep -Milliseconds 500
}
if(!$healthy){throw 'Application health check failed; see .local/logs/app.out'}
Write-Output "QINGJU_URL=http://127.0.0.1:$AppPort"
