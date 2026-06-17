#!/usr/bin/env pwsh
# ============================================================
#  VenueSync - Keycloak Sync Script
#  Pushes your local realm export to the deployed Keycloak.
#
#  Usage (from repo root or keycloak/ folder):
#    .\keycloak\sync-to-render.ps1
#
#  Override defaults:
#    .\keycloak\sync-to-render.ps1 -AdminPass "mypassword" -FrontendUrl "https://myapp.pages.dev"
# ============================================================

param(
    [string]$KcUrl       = "https://keycloak-server-3dsx.onrender.com",
    [string]$AdminUser   = "admin",
    [string]$AdminPass   = "admin123",
    [string]$Realm       = "event-ticket-platform",
    [string]$FrontendUrl = "https://venuesync.pages.dev"
)

$ErrorActionPreference = "Stop"

# Resolve export file paths (works from repo root or keycloak/ folder)
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$realmFile = Join-Path $scriptDir "event-ticket-platform-realm.json"
$usersFile = Join-Path $scriptDir "event-ticket-platform-users-0.json"

if (-not (Test-Path $realmFile)) {
    Write-Error "Cannot find $realmFile"
    exit 1
}
if (-not (Test-Path $usersFile)) {
    Write-Error "Cannot find $usersFile"
    exit 1
}

function Get-Token {
    $body = "client_id=admin-cli" `
          + "&username=$AdminUser" `
          + "&password=$AdminPass" `
          + "&grant_type=password"
    $res = Invoke-RestMethod `
        -Uri "$KcUrl/realms/master/protocol/openid-connect/token" `
        -Method Post `
        -ContentType "application/x-www-form-urlencoded" `
        -Body $body
    return $res.access_token
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  VenueSync Keycloak Sync" -ForegroundColor Cyan
Write-Host "  Target  : $KcUrl" -ForegroundColor Cyan
Write-Host "  Realm   : $Realm" -ForegroundColor Cyan
Write-Host "  Frontend: $FrontendUrl" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ── Step 1: Authenticate ───────────────────────────────────────────────────────
Write-Host "[1/6] Authenticating..." -ForegroundColor Yellow
$token = Get-Token
$h = @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" }
Write-Host "      OK" -ForegroundColor Green

# ── Step 2: Check realm ────────────────────────────────────────────────────────
Write-Host "[2/6] Checking if realm exists..." -ForegroundColor Yellow
$existingRealms = Invoke-RestMethod -Uri "$KcUrl/admin/realms" -Headers $h
$realmExists = ($existingRealms | Where-Object { $_.realm -eq $Realm }).Count -gt 0

if ($realmExists) {
    Write-Host "      Realm '$Realm' already exists - skipping import." -ForegroundColor DarkYellow
} else {
    # ── Step 3: Import realm ───────────────────────────────────────────────────
    Write-Host "[3/6] Importing realm..." -ForegroundColor Yellow
    $realmJson = Get-Content $realmFile -Raw
    Invoke-RestMethod -Uri "$KcUrl/admin/realms" -Method Post -Headers $h -Body $realmJson
    Write-Host "      Realm imported OK" -ForegroundColor Green
}

# Refresh token (may have expired during realm import)
$token = Get-Token
$h = @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" }

# ── Step 4: Import users ───────────────────────────────────────────────────────
Write-Host "[4/6] Importing users..." -ForegroundColor Yellow
$usersData = Get-Content $usersFile -Raw | ConvertFrom-Json

foreach ($user in $usersData.users) {
    $existingUser = Invoke-RestMethod `
        -Uri "$KcUrl/admin/realms/$Realm/users?username=$($user.username)&exact=true" `
        -Headers $h

    if ($existingUser.Count -gt 0) {
        Write-Host "      '$($user.username)' already exists - skipping." -ForegroundColor DarkYellow
    } else {
        $userJson = $user | ConvertTo-Json -Depth 10
        try {
            Invoke-RestMethod -Uri "$KcUrl/admin/realms/$Realm/users" -Method Post -Headers $h -Body $userJson
            Write-Host "      Imported: $($user.username)" -ForegroundColor Green
        } catch {
            Write-Host "      ERROR importing $($user.username): $_" -ForegroundColor Red
        }
    }
}

# ── Step 5: Fix client redirect URIs ──────────────────────────────────────────
Write-Host "[5/6] Updating client redirect + logout URIs..." -ForegroundColor Yellow
$clients = Invoke-RestMethod `
    -Uri "$KcUrl/admin/realms/$Realm/clients?clientId=event-ticket-platform-app" `
    -Headers $h
$client = $clients[0]
$cid    = $client.id

$client.redirectUris = @(
    "http://localhost:5173/callback",
    "https://venuesync-web.onrender.com/callback",
    "$FrontendUrl/callback"
)
$client.webOrigins = @(
    "http://localhost:5173",
    "https://venuesync-web.onrender.com",
    $FrontendUrl
)
$client.rootUrl  = $FrontendUrl
$client.baseUrl  = "$FrontendUrl/"
$client.adminUrl = "$FrontendUrl/"

# Post-logout redirect URIs (## is Keycloak's separator)
$client.attributes.'post.logout.redirect.uris' = `
    "http://localhost:5173/*" `
    + "##https://venuesync-web.onrender.com/*" `
    + "##$FrontendUrl/*" `
    + "##$FrontendUrl"

$body = $client | ConvertTo-Json -Depth 20
Invoke-RestMethod `
    -Uri "$KcUrl/admin/realms/$Realm/clients/$cid" `
    -Method Put -Headers $h -Body $body
Write-Host "      Client URIs updated OK" -ForegroundColor Green

# ── Step 6: Verify ────────────────────────────────────────────────────────────
Write-Host "[6/6] Verifying..." -ForegroundColor Yellow
$vRealms = (Invoke-RestMethod -Uri "$KcUrl/admin/realms" -Headers $h).realm
$vUsers  = Invoke-RestMethod -Uri "$KcUrl/admin/realms/$Realm/users" -Headers $h
$vRoles  = Invoke-RestMethod -Uri "$KcUrl/admin/realms/$Realm/roles" -Headers $h

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  SYNC COMPLETE" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host "  Realms : $($vRealms -join ', ')"
Write-Host "  Users  : $($vUsers.username -join ', ')"
Write-Host "  Roles  : $($vRoles.name -join ', ')"
Write-Host ""
Write-Host "  Admin console : $KcUrl/admin" -ForegroundColor Cyan
$issuer = "$KcUrl/realms/$Realm"
Write-Host "  Issuer URI    : $issuer" -ForegroundColor Cyan
Write-Host ""
