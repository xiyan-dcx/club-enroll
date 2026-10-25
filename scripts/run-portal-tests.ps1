param([string]$TomcatHome = '', [int]$DatabasePort = 13316, [int]$HttpPort = 18081)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (!$TomcatHome) { $TomcatHome = Join-Path (Split-Path (Split-Path $projectRoot -Parent) -Parent) '.runtime\apache-tomcat-10.1.60' }
$mysqlServer = (Get-Command mysqld.exe).Source
$mysqlClient = (Get-Command mysql.exe).Source
$javaExe = (Get-Command java.exe).Source
$runtimeRoot = Join-Path ([IO.Path]::GetTempPath()) ('qingju-tests-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $runtimeRoot | Out-Null
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dbProcess = $null; $webProcess = $null
function Invoke-TestSql([string]$sqlText) {
    $info = New-Object Diagnostics.ProcessStartInfo
    $info.FileName = $mysqlClient
    $info.Arguments = "--no-defaults --host=127.0.0.1 --port=$DatabasePort --user=root --default-character-set=utf8mb4"
    $info.UseShellExecute = $false; $info.CreateNoWindow = $true
    $info.RedirectStandardInput = $true
    $proc = [Diagnostics.Process]::Start($info)
    $bytes = $utf8.GetBytes($sqlText)
    $proc.StandardInput.BaseStream.Write($bytes,0,$bytes.Length); $proc.StandardInput.Close(); $proc.WaitForExit()
    if ($proc.ExitCode -ne 0) { throw 'Test SQL failed' }
}
try {
    & $mysqlServer --no-defaults --initialize-insecure --console "--datadir=$runtimeRoot\mysql"
    if ($LASTEXITCODE -ne 0) { throw 'Test MySQL initialization failed' }
    $dbProcess = Start-Process $mysqlServer -ArgumentList "--no-defaults --console --bind-address=127.0.0.1 --port=$DatabasePort --mysqlx=OFF --default-time-zone=+08:00 --datadir=`"$runtimeRoot\mysql`"" -WindowStyle Hidden -PassThru -RedirectStandardOutput "$runtimeRoot\mysql.out" -RedirectStandardError "$runtimeRoot\mysql.err"
    $ready = $false
    for ($i=0; $i -lt 40; $i++) {
        if ($dbProcess.HasExited) { throw ([IO.File]::ReadAllText("$runtimeRoot\mysql.err")) }
        & $mysqlClient --no-defaults --host=127.0.0.1 "--port=$DatabasePort" --user=root --execute='SELECT 1' 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $ready=$true; break }
        Start-Sleep -Milliseconds 500
    }
    if (!$ready) { throw 'Test MySQL not ready' }
    Invoke-TestSql ([IO.File]::ReadAllText((Join-Path $projectRoot 'sql\init.sql'), $utf8))
    Invoke-TestSql ('USE club_enroll;' + [Environment]::NewLine + [IO.File]::ReadAllText((Join-Path $projectRoot 'sql\upgrade-20261024.sql'), $utf8))
    # Verify the additive migration can safely be rerun.
    Invoke-TestSql ('USE club_enroll;' + [Environment]::NewLine + [IO.File]::ReadAllText((Join-Path $projectRoot 'sql\upgrade-20261024.sql'), $utf8))
    $base = Join-Path $runtimeRoot 'tomcat'
    foreach ($dir in @('conf','logs','temp','work','webapps')) { New-Item -ItemType Directory -Path (Join-Path $base $dir) -Force | Out-Null }
    Copy-Item "$TomcatHome\conf\*" "$base\conf" -Recurse
    $serverXml = '<Server port="-1"><Service name="Catalina"><Connector address="127.0.0.1" port="' + $HttpPort + '" protocol="HTTP/1.1"/><Engine name="Catalina" defaultHost="localhost"><Host name="localhost" appBase="webapps" autoDeploy="false" unpackWARs="true"/></Engine></Service></Server>'
    [IO.File]::WriteAllText("$base\conf\server.xml", $serverXml, $utf8)
    Copy-Item (Join-Path $projectRoot 'backend\target\club-enroll') "$base\webapps\club-enroll" -Recurse
    $dbConfig = "jdbc.url=jdbc:mysql://127.0.0.1:$DatabasePort/club_enroll?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai`njdbc.username=root`njdbc.password=`n"
    [IO.File]::WriteAllText("$base\webapps\club-enroll\WEB-INF\classes\db.properties", $dbConfig, $utf8)
    $javaArgs = "-Duser.timezone=Asia/Shanghai -Dcatalina.home=`"$TomcatHome`" -Dcatalina.base=`"$base`" -Djava.io.tmpdir=`"$base\temp`" -cp `"$TomcatHome\bin\bootstrap.jar;$TomcatHome\bin\tomcat-juli.jar`" org.apache.catalina.startup.Bootstrap start"
    $webProcess = Start-Process $javaExe -ArgumentList $javaArgs -WindowStyle Hidden -PassThru -RedirectStandardOutput "$runtimeRoot\tomcat.out" -RedirectStandardError "$runtimeRoot\tomcat.err"
    $ready=$false
    for ($i=0; $i -lt 40; $i++) {
        try { Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:$HttpPort/club-enroll/api/auth/me" | Out-Null } catch {
            if ($_.Exception.Response.StatusCode.value__ -eq 401) { $ready=$true; break }
        }
        if ($webProcess.HasExited) { throw ([IO.File]::ReadAllText("$runtimeRoot\tomcat.err")) }
        Start-Sleep -Milliseconds 500
    }
    if (!$ready) { throw 'Test Tomcat not ready' }
    & node (Join-Path $projectRoot 'scripts\portal-test.mjs') "http://127.0.0.1:$HttpPort/club-enroll/api" --allow-write
    if ($LASTEXITCODE -ne 0) { throw 'Portal integration checks failed' }
    & node (Join-Path $projectRoot 'scripts\smoke-test.mjs') "http://127.0.0.1:$HttpPort/club-enroll/api" --allow-write
    if ($LASTEXITCODE -ne 0) { throw 'Existing feature regression checks failed' }
} finally {
    if ($webProcess -and !$webProcess.HasExited) { Stop-Process -Id $webProcess.Id }
    if ($dbProcess -and !$dbProcess.HasExited) {
        try { Invoke-TestSql 'SHUTDOWN;' } catch { Stop-Process -Id $dbProcess.Id -ErrorAction SilentlyContinue }
    }
    Write-Output "TEST_LOGS=$runtimeRoot"
}
