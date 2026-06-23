import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";
import { corsHeaders, jsonResponse } from "../_shared/cors.ts";

const ACTIVATION_CODE_RE = /^[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{2}$/;

async function mintSessionForUser(
  admin: ReturnType<typeof createClient>,
  anon: ReturnType<typeof createClient>,
  email: string,
) {
  const { data: linkData, error: linkError } = await admin.auth.admin.generateLink({
    type: "magiclink",
    email,
  });

  if (linkError) {
    throw new Error(`Unable to create auth session: ${linkError.message}`);
  }

  const tokenHash = linkData.properties?.hashed_token;
  if (!tokenHash) {
    throw new Error("Unable to create auth session: missing token hash");
  }

  for (const otpType of ["magiclink", "email"] as const) {
    const { data: authData, error: authError } = await anon.auth.verifyOtp({
      type: otpType,
      token_hash: tokenHash,
    });

    if (!authError && authData.session) {
      return authData.session;
    }
  }

  throw new Error("Unable to verify auth session for this account.");
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { code } = await req.json();
    if (!code || typeof code !== "string") {
      return jsonResponse({ error: "Activation code is required" }, 400);
    }

    const normalizedCode = code.trim().toUpperCase();
    if (!ACTIVATION_CODE_RE.test(normalizedCode)) {
      return jsonResponse({ error: "Invalid activation code format." }, 400);
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;

    const admin = createClient(supabaseUrl, serviceRoleKey, {
      auth: { autoRefreshToken: false, persistSession: false },
    });
    const anon = createClient(supabaseUrl, anonKey, {
      auth: { autoRefreshToken: false, persistSession: false },
    });

    const { data: session, error: sessionError } = await admin
      .from("device_activation_sessions")
      .select("*")
      .eq("code", normalizedCode)
      .eq("status", "APPROVED")
      .gt("expires_at", new Date().toISOString())
      .maybeSingle();

    if (sessionError) {
      return jsonResponse({ error: sessionError.message }, 400);
    }

    if (!session?.user_id) {
      return jsonResponse(
        { error: "Activation session not approved yet. Approve the code on your phone first." },
        400,
      );
    }

    const { data: userData, error: userError } = await admin.auth.admin.getUserById(session.user_id);
    if (userError || !userData.user) {
      return jsonResponse({ error: "Approved user not found" }, 400);
    }

    const email = userData.user.email;
    if (!email) {
      return jsonResponse(
        {
          error:
            "This account has no email address. Sign in with email on iptv.afifistudio.com, then approve the TV again.",
        },
        400,
      );
    }

    const authSession = await mintSessionForUser(admin, anon, email);

    await admin
      .from("device_activation_sessions")
      .update({ status: "EXPIRED" })
      .eq("id", session.id);

    return jsonResponse({
      access_token: authSession.access_token,
      refresh_token: authSession.refresh_token,
      expires_in: authSession.expires_in,
      token_type: authSession.token_type,
      user_id: authSession.user.id,
    });
  } catch (error) {
    return jsonResponse({ error: String(error) }, 500);
  }
});
