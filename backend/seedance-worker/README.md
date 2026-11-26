# PS AI STUDIO — fal.ai Video Worker

Cloudflare Worker proxy for Seedance 2.5, Kling 4.0, Wan 3.0 and Hailuo 2.3 Pro video generation.

## Android API routes

Seedance routes remain compatible:
- `POST /v1/seedance/{text-to-video|image-to-video}`
- `GET /v1/seedance/{mode}/requests/{request_id}/status`
- `GET /v1/seedance/{mode}/requests/{request_id}/result`

Additional fal.ai model routes:
- `POST /v1/fal/kling-v4/{text-to-video|image-to-video}`
- `POST /v1/fal/wan-3/{text-to-video|image-to-video}`
- `POST /v1/fal/hailuo-23/{text-to-video|image-to-video}`
- `GET /v1/fal/{model}/{mode}/requests/{request_id}/status`
- `GET /v1/fal/{model}/{mode}/requests/{request_id}/result`

The Worker uses a fixed allowlist of fal.ai endpoint IDs. It does not accept arbitrary upstream URLs.

## Deploy

From this directory, with Wrangler already authenticated to the Cloudflare account:
```sh
npx wrangler deploy
```

## Authentication and privacy

The Android app sends the user's own fal.ai API key in an HTTPS Authorization bearer header. The Worker forwards it to fal.ai as `Authorization: Key ...`; it does not store the key. The key remains in the app's encrypted local settings. Do not commit API keys or Cloudflare credentials.

## Model-specific behavior

- Kling 4.0: text-to-video and image-to-video; image mode uses the uploaded image as the first frame.
- Wan 3.0: text-to-video and image-to-video; supports audio, resolution, duration and aspect ratio.
- Hailuo 2.3 Pro: text-to-video and image-to-video; uses the provider's fixed output profile and optional prompt optimizer.

fal.ai usage is billed by fal.ai according to the user's account and selected model.
