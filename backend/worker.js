const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
  "Content-Type": "application/json; charset=utf-8"
};

function json(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return new Response(null, { headers: corsHeaders });
    if (request.method !== "POST") return json({ error: "Usa POST para enviar un mensaje." }, 405);
    if (!env.GEMINI_API_KEY) return json({ error: "Falta configurar GEMINI_API_KEY en los secretos del servidor." }, 503);

    let body;
    try { body = await request.json(); }
    catch { return json({ error: "El cuerpo debe ser JSON válido." }, 400); }

    const message = typeof body.message === "string" ? body.message.trim() : "";
    if (!message) return json({ error: "Escribe un mensaje." }, 400);
    if (message.length > 8000) return json({ error: "El mensaje supera el límite de 8000 caracteres." }, 413);

    const history = Array.isArray(body.history) ? body.history.slice(-12) : [];
    const contents = [];
    for (const item of history) {
      if (!item || typeof item.text !== "string") continue;
      const role = item.role === "model" ? "model" : "user";
      contents.push({ role, parts: [{ text: item.text.slice(0, 4000) }] });
    }
    contents.push({ role: "user", parts: [{ text: message }] });

    try {
      const upstream = await fetch(
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "x-goog-api-key": env.GEMINI_API_KEY
          },
          body: JSON.stringify({
            systemInstruction: {
              parts: [{ text: "Eres Astral, un asistente útil, claro y amable integrado en Astral Browser. Responde en español si el usuario escribe en español. No afirmes haber visto páginas o realizado acciones que no recibiste o ejecutaste." }]
            },
            contents,
            generationConfig: { temperature: 0.7, maxOutputTokens: 700 }
          })
        }
      );

      const data = await upstream.json();
      if (!upstream.ok) {
        return json({ error: upstream.status === 429
          ? "Se alcanzó el límite temporal del proveedor. Intenta más tarde."
          : "El proveedor de IA no pudo completar la solicitud.", providerStatus: upstream.status }, 502);
      }
      const reply = data?.candidates?.[0]?.content?.parts
        ?.map(part => typeof part.text === "string" ? part.text : "")
        .join("").trim();
      if (!reply) return json({ error: "La IA devolvió una respuesta vacía. Intenta otra vez." }, 502);
      return json({ reply });
    } catch {
      return json({ error: "No se pudo conectar con el proveedor de IA." }, 502);
    }
  }
};
