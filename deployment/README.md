# PlateMate Windows Deployment

This folder contains templates for hosting PlateMate on a Windows Server with Caddy and pm2.

## Files

- `Caddyfile.example`: reverse proxy from `platemate.at` to the Java app on `127.0.0.1:8081`.
- `ecosystem.config.cjs`: pm2 process config for the Spring Boot JAR and GitHub webhook.
- `deploy.ps1`: pulls, builds, copies the JAR, and restarts pm2.
- `webhook-server.cjs`: tiny GitHub webhook receiver using only Node built-ins.

## First Server Setup

Create the app folders:

```powershell
New-Item -ItemType Directory -Force -Path C:\apps\platemate\repo
New-Item -ItemType Directory -Force -Path C:\apps\platemate\current
New-Item -ItemType Directory -Force -Path C:\apps\platemate\uploads
```

Clone the repo into `C:\apps\platemate\repo`.

Before starting pm2, set production environment variables in the server shell, Windows user environment, or pm2 startup environment:

- `PLATEMATE_DB_PASSWORD`
- `MAPBOX_ACCESS_TOKEN` if route previews should work
- `PLATEMATE_DB_URL` if the production database name differs
- `PLATEMATE_WEBHOOK_SECRET` before enabling the webhook

Deploy once:

```powershell
powershell -ExecutionPolicy Bypass -File C:\apps\platemate\repo\deployment\deploy.ps1
```

If you want to start both PM2 apps manually:

```powershell
pm2 start C:\apps\platemate\repo\deployment\ecosystem.config.cjs
pm2 save
```

## Caddy

Replace the static placeholder site block with the reverse-proxy block from `Caddyfile.example`.

Reload Caddy after editing:

```powershell
caddy reload --config C:\path\to\Caddyfile
```

## GitHub Webhook

Set a strong `PLATEMATE_WEBHOOK_SECRET` in the server environment.

Start the webhook:

```powershell
pm2 start C:\apps\platemate\repo\deployment\ecosystem.config.cjs --only platemate-webhook
pm2 save
```

In GitHub, add a webhook:

- Payload URL: `https://platemate.at/github`
- Content type: `application/json`
- Secret: same value as `PLATEMATE_WEBHOOK_SECRET`
- Events: just `push`

To expose the webhook through Caddy, add this inside the `platemate.at` block before the main `reverse_proxy`:

```caddyfile
handle_path /github {
    reverse_proxy 127.0.0.1:9091
}
```

## Useful Commands

```powershell
pm2 status
pm2 logs platemate
pm2 restart platemate --update-env
pm2 logs platemate-webhook
```
