const MODELS = {
  "text-to-video": "bytedance/seedance-2.5/us/text-to-video",
  "image-to-video": "bytedance/seedance-2.5/us/image-to-video",
};

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Authorization, Content-Type",
  "Content-Type": "application/json; charset=utf-8",
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: corsHeaders });
}

function getFalKey(request) {
  const value = request.headers.get("Authorization") || "";
  const match = value.match(/^Bearer\s+(.+)$/i);
  return match?.[1]?.trim() || null;
}

function validPrompt(value) {
  return typeof value === "string" && value.trim().length > 0 && value.length <= 20000;
}

function validateInput(mode, input) {
  if (!input || typeof input !== "object" || Array.isArray(input)) {
    return "Request body must be a JSON object.";
  }
  if (!validPrompt(input.prompt)) return "Prompt is required (maximum 20,000 characters).";
  const resolutions = ["480p", "720p", "1080p", "4k"];
  const durations = ["auto", ...Array.from({ length: 27 }, (_, i) => String(i + 4))];
  const ratios = ["auto", "21:9", "16:9", "4:3", "1:1", "3:4", "9:16"];
  if (input.resolution && !resolutions.includes(input.resolution)) return "Unsupported resolution.";
  if (input.duration && !durations.includes(String(input.duration))) return "Unsupported duration.";
  if (input.aspect_ratio && !ratios.includes(input.aspect_ratio)) return "Unsupported aspect ratio.";
  if (input.image_url !== undefined) {
    if (mode !== "image-to-video") return "image_url is only valid for image-to-video.";
    const value = input.image_url;
    const isHttps = typeof value === "string" && /^https:\/\//i.test(value);
    const isDataUri = typeof value === "string" && /^data:image\/(jpeg|png|webp);base64,/i.test(value);
    if (!isHttps && !isDataUri) return "image_url must be an HTTPS URL or JPEG/PNG/WebP base64 data URI.";
    if (value.length > 42_000_000) return "Image input is too large (maximum about 30 MB).";
  }
  if (mode === "image-to-video" && !input.image_url) return "image_url is required for image-to-video.";
  return null;
}

function pickInput(mode, input) {
  const allowed = [
    "prompt", "resolution", "duration", "aspect_ratio", "generate_audio",
    "bitrate_mode", "codec", "output_format", "draft", "return_last_frame",
    "end_user_id",
  ];
  const result = {};
  for (const key of allowed) {
    if (input[key] !== undefined) result[key] = input[key];
  }
  if (mode === "image-to-video") {
    result.image_url = input.image_url;
    result.aspect_ratio = "auto";
  }
  return result;
}

export default {
  async fetch(request) {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders });
    }

    const url = new URL(request.url);
    const parts = url.pathname.split("/").filter(Boolean);
    if (parts[0] !== "v1" || parts[1] !== "seedance") return json({ error: "Not found" }, 404);

    const falKey = getFalKey(request);
    if (!falKey) return json({ error: "Missing user fal API key. Send Authorization: Bearer <FAL_KEY>." }, 401);

    // POST /v1/seedance/{text-to-video|image-to-video}
    if (request.method === "POST" && parts.length === 3 && MODELS[parts[2]]) {
      const mode = parts[2];
      let input;
      try {
        input = await request.json();
      } catch {
        return json({ error: "Invalid JSON body." }, 400);
      }
      const validationError = validateInput(mode, input);
      if (validationError) return json({ error: validationError }, 400);

      const endpoint = MODELS[mode].split("/");
      const upstreamUrl = `https://queue.fal.run/${endpoint.slice(0, 2).join("/")}/${endpoint.slice(2).join("/")}`;
      const upstream = await fetch(upstreamUrl, {
        method: "POST",
        headers: {
          Authorization: `Key ${falKey}`,
          "Content-Type": "application/json",
          "X-Fal-Store-IO": "1",
        },
        body: JSON.stringify(pickInput(mode, input)),
      });
      const body = await upstream.text();
      return new Response(body, {
        status: upstream.status,
        headers: corsHeaders,
      });
    }

    // GET /v1/seedance/{mode}/requests/{request_id}/status|result
    if (request.method === "GET" && parts.length === 6 &&
        MODELS[parts[2]] && parts[3] === "requests" &&
        /^[0-9a-f-]{20,}$/i.test(parts[4]) &&
        ["status", "result"].includes(parts[5])) {
      const mode = parts[2];
      const endpoint = MODELS[mode].split("/");
      const base = `https://queue.fal.run/${endpoint.join("/")}/requests/${encodeURIComponent(parts[4])}`;
      const target = parts[5] === "status" ? `${base}/status` : base;
      const upstream = await fetch(target, {
        headers: { Authorization: `Key ${falKey}` },
      });
      const body = await upstream.text();
      return new Response(body, { status: upstream.status, headers: corsHeaders });
    }

    return json({ error: "Unsupported route." }, 404);
  },
};
