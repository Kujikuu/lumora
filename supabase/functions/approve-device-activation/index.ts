import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";
import { corsHeaders, jsonResponse } from "../_shared/cors.ts";

const ACTIVATION_CODE_RE = /^[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{2}$/;

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader?.startsWith("Bearer ")) {
      return jsonResponse({ error: "Sign in required. Missing authorization token." }, 401);
    }

    const { activation_code } = await req.json();
    if (!activation_code || typeof activation_code !== "string") {
      return jsonResponse({ error: "Activation code is required." }, 400);
    }

    const normalizedCode = activation_code.trim().toUpperCase();
    if (!ACTIVATION_CODE_RE.test(normalizedCode)) {
      return jsonResponse({ error: "Invalid activation code format." }, 400);
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;

    const userClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: authHeader } },
      auth: { autoRefreshToken: false, persistSession: false },
    });

    const { data: userData, error: userError } = await userClient.auth.getUser();
    if (userError || !userData.user) {
      return jsonResponse({ error: "Invalid or expired sign-in. Please sign in again." }, 401);
    }

    const { data, error } = await userClient.rpc("approve_device_activation", {
      activation_code: normalizedCode,
    });

    if (error) {
      return jsonResponse({ error: error.message }, 400);
    }

    return jsonResponse({
      ok: true,
      session: data,
      user_id: userData.user.id,
      email: userData.user.email,
    });
  } catch (error) {
    return jsonResponse({ error: String(error) }, 500);
  }
});
