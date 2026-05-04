# PlateMate

PlateMate is a Software Engineering II MVP for a small food ordering and delivery platform.

## Stack

- Java 21
- Spring Boot 4
- Vaadin 25
- PostgreSQL
- Maven

## Local Development

Start PostgreSQL with Docker:

```powershell
docker compose up -d
```

Run the app in dev mode:

```powershell
.\scripts\run-dev.ps1
```

Or from Git Bash/WSL:

```bash
./scripts/run-dev.sh
```

This starts Docker Postgres if needed and runs Spring Boot/Vaadin in development mode.

For the closest workflow to `ng serve`, enable auto-build in your IDE. If you are not using IDE auto-build, keep this watcher running in a second terminal:

```powershell
.\scripts\watch-compile.ps1
```

Or from Git Bash/WSL:

```bash
./scripts/watch-compile.sh
```

Or run the packaged JAR with the local `.env` file:

```powershell
mvn -Pproduction -DskipTests package
.\scripts\run-local.ps1
```

If Vaadin dev mode asks about usage statistics in a restricted shell, disable telemetry for that process:

```powershell
$env:VAADIN_USAGE_STATS_ENABLED='false'
```

Open:

```text
http://localhost:8080
```

The dev profile expects:

```text
jdbc:postgresql://localhost:5432/platemate
username: platemate
password: platemate
```

## Build

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -DskipTests package
```

## Smoke Test Without PostgreSQL

If Docker Desktop or PostgreSQL is not running yet, boot the app with the in-memory smoke profile:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:VAADIN_USAGE_STATS_ENABLED='false'
mvn spring-boot:run -Dspring-boot.run.profiles=smoke
```

## Deployment Direction

For the Windows VM, use native PostgreSQL as a Windows service, run the Spring Boot JAR through `pm2` or a Windows service wrapper, and let Caddy reverse proxy to the local app port.
