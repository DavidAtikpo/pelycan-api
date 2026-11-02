# Telecharge Maven Wrapper (mvnw.cmd) une seule fois - plus besoin de Maven global.
# Usage: cd pelycan-api ; .\bootstrap-maven-wrapper.ps1

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

New-Item -ItemType Directory -Force -Path .mvn/wrapper | Out-Null

$jarUrl = "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
$mvnwUrl = "https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw.cmd"

Write-Host "Telechargement maven-wrapper.jar..."
Invoke-WebRequest -Uri $jarUrl -OutFile ".mvn/wrapper/maven-wrapper.jar" -UseBasicParsing

@"
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip
wrapperUrl=$jarUrl
"@ | Set-Content -Encoding ASCII ".mvn/wrapper/maven-wrapper.properties"

Write-Host "Telechargement mvnw.cmd..."
Invoke-WebRequest -Uri $mvnwUrl -OutFile "mvnw.cmd" -UseBasicParsing

Write-Host "OK. Lancez .\run-local.ps1"
