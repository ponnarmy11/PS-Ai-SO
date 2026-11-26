const SEEDANCE_MODELS = {
  "text-to-video": "bytedance/seedance-2.5/us/text-to-video",
  "image-to-video": "bytedance/seedance-2.5/us/image-to-video",
};

const FAL_MODELS = {
  "kling-v4": {
    "text-to-video": "kling/v4/text-to-video",
    "image-to-video": "kling/v4/image-to-video",
  },
  "wan-3": {
    "text-to-video": "alibaba/wan-3.0/text-to-video",
    "image-to-video": "alibaba/wan-3.0/image-to-video",
  },
  "hailuo-23": {
    "text-to-video": "fal-ai/minimax/hailuo-2.3/pro/text-to-video",
    "image-to-video": "fal-ai/minimax/hailuo-2.3/pro/image-to-video",
  },
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

function validImage(value) {
  return typeof value === "string" &&
    (/^https:\/\//i.test(value) || /^data:image\/(jpeg|png|webp);base64,/i.test(value)) &&
    value.length <= 42_000_000;
}

function falInput(model, mode, input) {
  const imageToVideo = mode === "image-to-video";
  if (model === "kling-v4") {
    const result = {
      prompt: input.prompt,
      duration: Math.min(30, Math.max(3, Number(input.duration) || 5)),
      resolution: ["720p", "1080p", "4k"].includes(input.resolution) ? input.resolution : "1080p",
    };
    if (imageToVideo) result.start_image_url = input.image_url;
    else result.aspect_ratio = ["16:9", "9:16", "1:1", "4:3", "3:4", "21:9"].includes(input.aspect_ratio) ? input.aspect_ratio : "16:9";
    return result;
  }
  if (model === "wan-3") {
    const result = {
      prompt: input.prompt,
      duration: Math.min(30, Math.max(2, Number(input.duration) || 5)),
      resolution: ["480p", "720p", "1080p"].includes(input.resolution) ? input.resolution : "1080p",
      aspect_ratio: ["adaptive", "16:9", "4:3", "1:1", "3:4", "9:16"].includes(input.aspect_ratio) ? input.aspect_ratio : "adaptive",
      audio: input.audio !== false,
      enable_prompt_expansion: true,
      enable_safety_checker: true,
    };
    if (imageToVideo) result.start_image_url = input.image_url;
    return result;
  }
  if (model === "hailuo-23") {
    const result = {
      prompt: input.prompt,
      prompt_optimizer: input.prompt_optimizer !== false,
      enable_safety_checker: true,
    };
    if (imageToVideo) result.image_url = input.image_url;
    return result;
  }
  throw new Error("Unsupported fal video model.");
}

async function submitToFal(endpoint, input, falKey) {
  return fetch("https://queue.fal.run/" + endpoint, {
    method: "POST",
    headers: {
      Authorization: "Key " + falKey,
      "Content-Type": "application/json",
      "X-Fal-Store-IO": "1",
    },
    body: JSON.stringify(input),
  });
}

async function proxyFalRequest(request, endpoint, requestId, action, falKey) {
  const base = "https://queue.fal.run/" + endpoint + "/requests/" + encodeURIComponent(requestId);
  const target = action === "status" ? base + "/status" : base;
  const upstream = await fetch(target, {
    headers: { Authorization: "Key " + falKey },
  });
  return new Response(await upstream.text(), { status: upstream.status, headers: corsHeaders });
}

export default {
  async fetch(request) {
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: corsHeaders });

    const url = new URL(request.url);
    const parts = url.pathname.split("/").filter(Boolean);
    if (parts[0] !== "v1") return json({ error: "Not found" }, 404);

    const falKey = getFalKey(request);
    if (!falKey) return json({ error: "Missing user fal API key. Send Authorization: Bearer <FAL_KEY>." }, 401);

    // Keep the original Seedance routes compatible.
    if (parts[1] === "seedance") {
      const mode = parts[2];
      const endpoint = SEEDANCE_MODELS[mode];
      if (!endpoint) return json({ error: "Unsupported Seedance mode." }, 404);

      if (request.method === "POST" && parts.length === 3) {
        let input;
        try { input = await request.json(); } catch { return json({ error: "Invalid JSON body." }, 400); }
        if (!validPrompt(input?.prompt)) return json({ error: "Prompt is required." }, 400);
        if (mode === "image-to-video" && !validImage(input.image_url)) return json({ error: "A valid image_url is required." }, 400);
        const upstream = await submitToFal(endpoint, input, falKey);
        return new Response(await upstream.text(), { status: upstream.status, headers: corsHeaders });
      }

      if (request.method === "GET" && parts.length === 6 && parts[3] === "requests" &&
          /^[0-9a-f-]{20,}$/i.test(parts[4]) && ["status", "result"].includes(parts[5])) {
        return proxyFalRequest(request, endpoint, parts[4], parts[5], falKey);
      }
      return json({ error: "Unsupported Seedance route." }, 404);
    }

    // Generic fal video routes: /v1/fal/{model}/{mode}
    if (parts[1] === "fal") {
      const model = parts[2];
      const mode = parts[3];
      const endpoint = FAL_MODELS[model]?.[mode];
      if (!endpoint) return json({ error: "Unsupported fal video model or mode." }, 404);

      if (request.method === "POST" && parts.length === 4) {
        let input;
        try { input = await request.json(); } catch { return json({ error: "Invalid JSON body." }, 400); }
        if (!validPrompt(input?.prompt)) return json({ error: "Prompt is required." }, 400);
        if (mode === "image-to-video" && !validImage(input.image_url)) return json({ error: "A valid starting image is required." }, 400);
        let mapped;
        try { mapped = falInput(model, mode, input); }
        catch (error) { return json({ error: error.message }, 400); }
        const upstream = await submitToFal(endpoint, mapped, falKey);
        return new Response(await upstream.text(), { status: upstream.status, headers: corsHeaders });
      }

      if (request.method === "GET" && parts.length === 7 && parts[4] === "requests" &&
          /^[0-9a-f-]{20,}$/i.test(parts[5]) && ["status", "result"].includes(parts[6])) {
        return proxyFalRequest(request, endpoint, parts[5], parts[6], falKey);
      }
      return json({ error: "Unsupported fal route." }, 404);
    }

    return json({ error: "Not found" }, 404);
  },
};
