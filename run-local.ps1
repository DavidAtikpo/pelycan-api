# Demarre Postgres + Redis (Docker) puis l API Spring Boot (port 4000).
# Usage: cd pelycan-api ; .\run-local.ps1
#        .\run-local.ps1 -SkipDocker   # si Postgres/Redis deja lances a la main

param(
    [switch]$SkipDocker
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Get-JavaVersionText {
    $java = Get-Command java -ErrorAction SilentlyContinue
    if (-not $java) { return "" }
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $java.Source
    $psi.Arguments = "-version"
    $psi.RedirectStandardError = $true
    $psi.RedirectStandardOutput = $true
    $psi.UseShellExecute = $false
    $p = [System.Diagnostics.Process]::Start($psi)
    $text = $p.StandardError.ReadToEnd() + $p.StandardOutput.ReadToEnd()
    $p.WaitForExit() | Out-Null
    return $text
}

function Add-EnvPathIfExists {
    param([string[]]$Dirs)
    foreach ($d in $Dirs) {
        if ($d -and (Test-Path $d)) {
            $env:Path = "$d;" + $env:Path
        }
    }
}

function Import-DotEnv {
    param([string]$Path = ".env")
    if (-not (Test-Path $Path)) {
        if (Test-Path ".env.example") {
            Copy-Item ".env.example" $Path
            Write-Host '.env cree depuis .env.example - verifiez JWT_SECRET en prod.'
        } else {
            Write-Warning 'Pas de .env - variables par defaut (application.yml)'
            return
        }
    }
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $eq = $line.IndexOf("=")
        if ($eq -lt 1) { return }
        $name = $line.Substring(0, $eq).Trim()
        $value = $line.Substring($eq + 1).Trim()
        Set-Item -Path "Env:$name" -Value $value
    }
    if ($env:JAVA_HOME -and (Test-Path $env:JAVA_HOME)) {
        Add-EnvPathIfExists @((Join-Path $env:JAVA_HOME "bin"))
    }
    if ($env:MAVEN_HOME -and (Test-Path $env:MAVEN_HOME)) {
        Add-EnvPathIfExists @((Join-Path $env:MAVEN_HOME "bin"))
    }
}

function Set-PelycanJdbcUrlFromDatabaseUrl {
    $raw = $env:DATABASE_URL
    if (-not $raw) { return }
    $raw = $raw.Trim()
    if ($raw.StartsWith("jdbc:")) {
        $env:PELYCAN_JDBC_URL = $raw
        return
    }
    if ($raw -match '^(postgres(?:ql)?://)(.+)$') {
        $rest = $Matches[2]
        if ($rest -match '^([^@]+)@(.+)$') {
            $userInfo = $Matches[1]
            $hostPart = $Matches[2]
            if ($userInfo -match '^([^:]+):(.+)$') {
                $env:DATABASE_USERNAME = $Matches[1]
                $env:DATABASE_PASSWORD = $Matches[2]
            } elseif ($userInfo) {
                $env:DATABASE_USERNAME = $userInfo
            }
            $env:PELYCAN_JDBC_URL = "jdbc:postgresql://$hostPart"
        } else {
            $env:PELYCAN_JDBC_URL = "jdbc:postgresql://$rest"
        }
    }
}

function Enable-SimpleCacheIfRedisUnavailable {
    if ($env:PELYCAN_CACHE_TYPE) { return }
    $redisOk = (Test-NetConnection -ComputerName localhost -Port 6379 -WarningAction SilentlyContinue).TcpTestSucceeded
    if (-not $redisOk) {
        $env:PELYCAN_CACHE_TYPE = 'simple'
        Write-Host 'Redis (6379) absent — cache memoire local (PELYCAN_CACHE_TYPE=simple).' -ForegroundColor Yellow
        Write-Host '  /actuator/health OK sans Redis. Pour Redis: Docker puis docker compose up -d redis' -ForegroundColor DarkYellow
    }
}

function Set-Java21Environment {
    $current = Get-JavaVersionText
    if ($current -match 'version "21') {
        Write-Host 'Java 21 actif.'
        return $true
    }
    $JavaHomes = @(
        $env:JAVA_HOME,
        (Get-ChildItem "C:\Program Files\Eclipse Adoptium\jdk-21*" -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName),
        (Get-ChildItem "C:\Program Files\Java\jdk-21*" -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName),
        (Get-ChildItem "$env:LOCALAPPDATA\Programs\Eclipse Adoptium\jdk-21*" -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName)
    ) | Where-Object { $_ -and (Test-Path $_) } | Select-Object -Unique

    foreach ($JavaHome in $JavaHomes) {
        Add-EnvPathIfExists @((Join-Path $JavaHome "bin"))
        $check = Get-JavaVersionText
        if ($check -match 'version "21') {
            $env:JAVA_HOME = $JavaHome
            Write-Host "JAVA_HOME = $env:JAVA_HOME"
            return $true
        }
    }
    return $false
}

function Resolve-MavenCommand {
    if (Get-Command mvn -ErrorAction SilentlyContinue) { return "mvn" }
    $candidates = @(
        (Get-ChildItem "C:\Program Files\Apache\Maven\*\bin\mvn.cmd" -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName),
        (Get-ChildItem "${env:ProgramFiles(x86)}\Apache\Maven\*\bin\mvn.cmd" -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName),
        (Get-ChildItem "C:\apache-maven-*\bin\mvn.cmd" -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName),
        (Get-ChildItem "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd" -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName),
        "$env:ProgramData\chocolatey\lib\maven\apache-maven-*\bin\mvn.cmd"
    )
    foreach ($pattern in $candidates) {
        if (-not $pattern) { continue }
        if ($pattern -match '\*') {
            $found = Get-ChildItem $pattern -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
        } else {
            $found = $pattern
        }
        if ($found -and (Test-Path $found)) {
            Add-EnvPathIfExists @((Split-Path $found -Parent))
            if (Get-Command mvn -ErrorAction SilentlyContinue) { return "mvn" }
        }
    }
    $mvnw = Join-Path $PSScriptRoot "mvnw.cmd"
    if (Test-Path $mvnw) { return $mvnw }
    return $null
}

function Initialize-DockerPath {
    Add-EnvPathIfExists @(
        "C:\Program Files\Docker\Docker\resources\bin",
        "$env:ProgramFiles\Docker\Docker\resources\bin",
        "$env:LOCALAPPDATA\Docker\cli-plugins"
    )
}

Initialize-DockerPath

Write-Host '=== Pelycan API - demarrage local ===' -ForegroundColor Cyan
Import-DotEnv
Set-PelycanJdbcUrlFromDatabaseUrl

if (-not (Set-Java21Environment)) {
    $javaVersion = Get-JavaVersionText
    if ($javaVersion) { Write-Host $javaVersion }
    Write-Host ''
    Write-Host 'ERREUR: Java 21 introuvable (Java 17 detecte ou java absent).' -ForegroundColor Red
    Write-Host 'Installez JDK 21 puis redemarrez le terminal:' -ForegroundColor Yellow
    Write-Host '  winget install EclipseAdoptium.Temurin.21.JDK' -ForegroundColor White
    Write-Host 'Ou definissez JAVA_HOME dans .env (ex: JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot)' -ForegroundColor White
    exit 1
}

Write-Host (Get-JavaVersionText)

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host 'Docker introuvable dans le PATH.' -ForegroundColor Yellow
    Write-Host '  winget install Docker.DockerDesktop  (puis demarrer Docker Desktop)' -ForegroundColor White
    Write-Host 'Sans Docker: RDS/Postgres dans .env ; Redis optionnel (cache memoire auto si 6379 ferme)' -ForegroundColor Yellow
    if (-not $SkipDocker) {
        Write-Host 'Pour lancer quand meme Spring Boot: .\run-local.ps1 -SkipDocker' -ForegroundColor Yellow
    }
} elseif (-not $SkipDocker) {
    $remoteDb = $env:DATABASE_URL -match 'amazonaws\.com|rds\.'
    if ($remoteDb) {
        Write-Host 'Docker: demarrage redis uniquement (BDD distante dans .env)...' -ForegroundColor Cyan
        docker compose up -d redis
    } else {
        Write-Host 'Docker: demarrage postgres + redis...' -ForegroundColor Cyan
        docker compose up -d
    }
    if ($LASTEXITCODE -ne 0) { throw 'docker compose a echoue' }
}

Enable-SimpleCacheIfRedisUnavailable

$mvn = Resolve-MavenCommand
if (-not $mvn) {
    Write-Host 'Maven introuvable et mvnw.cmd absent.' -ForegroundColor Red
    Write-Host 'Option A - Maven Wrapper (recommande):' -ForegroundColor Yellow
    Write-Host '  .\bootstrap-maven-wrapper.ps1' -ForegroundColor White
    Write-Host 'Option B - Maven global:' -ForegroundColor Yellow
    Write-Host '  winget install Apache.Maven' -ForegroundColor White
    Write-Host '  puis ajoutez MAVEN_HOME dans .env si besoin' -ForegroundColor White
    exit 1
}

Write-Host 'Maven: spring-boot:run sur http://localhost:4000 ...' -ForegroundColor Cyan
& $mvn spring-boot:run
