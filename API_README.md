# Android Key Attestation API

A modern Kotlin-based REST API wrapper for Android Key Attestation verification.

## Quick Start

### 1. Build and Run Locally

```bash
./gradlew run
```

The server will start on port 8080 (or the port specified in the `PORT` environment variable).

### 2. API Usage

#### Health Check: `GET /health`

**Response:**
```json
{
  "status": "healthy"
}
```

#### Attestation Verification: `POST /verify`

**Request Body:**
```json
{
  "attestationChainPem": [
    "-----BEGIN CERTIFICATE-----\nMIIB...==\n-----END CERTIFICATE-----",
    "-----BEGIN CERTIFICATE-----\nMIIC...==\n-----END CERTIFICATE-----"
  ],
  "challenge": "BASE64_SERVER_NONCE"
}
```

**Response (Success):**
```json
{
  "ok": true,
  "packageName": "com.wootz.browser",
  "signingCertDigest": "a1b2c3d4...",
  "verifiedBootState": "VERIFIED",
  "securityLevel": "TRUSTED_ENVIRONMENT"
}
```

**Response (Error):**
```json
{
  "ok": false,
  "error": "Challenge mismatch"
}
```

### 3. Deploy to Railway

1. Go to [Railway](https://railway.app)
2. Create a new project → "Deploy from GitHub repo"
3. Select this repository
4. Railway will auto-detect the Gradle project and deploy it
5. Your API will be available at: `https://<your-app>.up.railway.app/verify`

### 4. Example cURL Request

```bash
curl -X POST https://your-app.up.railway.app/verify \
  -H "Content-Type: application/json" \
  -d '{
    "attestationChainPem": [
      "-----BEGIN CERTIFICATE-----\nYOUR_CERT_HERE\n-----END CERTIFICATE-----"
    ],
    "challenge": "YOUR_BASE64_CHALLENGE"
  }'
```

## Features

- ✅ Modern Kotlin-based implementation
- ✅ Google Trust Anchors integration
- ✅ Certificate revocation checking
- ✅ Challenge validation
- ✅ Comprehensive error handling
- ✅ Ready for Railway deployment

## Environment Variables

- `PORT` - Server port (default: 8080)

## Library

This API uses the modern Android Key Attestation library written in Kotlin, providing robust verification of Android hardware-backed key attestation.
