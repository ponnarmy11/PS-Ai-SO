# PS AI STUDIO — Seedance 2.5 Worker

Small Cloudflare Worker proxy for the Seedance 2.5 US endpoints used by the Android app.

## Routes

- `POST /v1/seedance/text-to-video`
- `POST /v1/seedance/image-to-video`
- `GET /v1/seedance/{mode}/requests/{request_id}/status`
- `GET /v1/seedance/{mode}/requests/{request_id}/result`

The Android app sends the user's own fal API key in the Authorization bearer header. The Worker converts it to fal's required `Authorization: Key ...` header. The key is not stored by this Worker. Only fixed Seedance endpoint IDs are reachable; arbitrary proxy URLs are not accepted.

## Deploy

1. Install Node.js LTS on a PC.
2. From this directory run `npm install --save-dev wrangler`.
3. Run `npx wrangler login`.
4. Run `npx wrangler deploy`.
5. Copy the deployed `https://....workers.dev` URL into the Android app's Seedance endpoint configuration.

No fal API key or Cloudflare token belongs in Git. Each app user supplies their own fal API key, stored locally by the Android app.

## Request example

```http
POST /v1/seedance/text-to-video
Authorization: Bearer YOUR_FAL_API_KEY
Content-Type: application/json
```

```json
{
  "prompt": "A cinematic sunrise over the mountains",
  "resolution": "720p",
  "duration": "5",
  "aspect_ratio": "16:9",
  "generate_audio": true
}
```

For image-to-video, send `image_url` as a public HTTPS URL or JPEG/PNG/WebP base64 data URI. The Worker validates inputs and forwards the request to fal's queue API. Use the returned request ID with the status route until it reports `COMPLETED`, then call the result route.

## Security

- Never put a shared fal API key or Cloudflare credential in this repository or APK.
- The Worker forwards a per-user fal key over HTTPS and does not persist it.
- Add an authenticated app-user layer and abuse controls before exposing this Worker as a shared public service.
