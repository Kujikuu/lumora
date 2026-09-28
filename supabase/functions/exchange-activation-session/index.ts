import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";
import { corsHeaders, jsonResponse } from "../_shared/cors.ts";

// The TV proves it owns the activation session with (session_id, qr_token). The code shown
// on screen is not enough: anyone in the room can read it.
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const QR_TOKEN_RE = /^[a-z0-9]{32}$/;
const MAX_ATTEMPTS_PER_WINDOW = 20;
const RATE_LIMIT_WINDOW_MS = 60_000;
const rateLimitBuckets = new Map<string, { count: number; resetAt: number }>();

function clientIp(req: Request): string {
  return req.headers.get("cf-connecting-ip")
    ?? req.headers.get("x-forwarded-for")?.split(",")[0]?.trim()
    ?? "unknown";
}

// Best effort only: buckets live in this worker's memory. The qr_token is what makes
// guessing infeasible; this just slows down noisy clients.
function checkRateLimit(key: string): boolean {
  const now = Date.now();
  const bucket = rateLimitBuckets.get(key);
  if (!bucket || now >= bucket.resetAt) {
    rateLimitBuckets.set(key, { count: 1, resetAt: now + RATE_LIMIT_WINDOW_MS });
    return true;
  }
  if (bucket.count >= MAX_ATTEMPTS_PER_WINDOW) {
    return false;
  }
  bucket.count += 1;
  return true;
}

function logFailure(reason: string, sessionId: string, detail?: string) {
  console.warn(JSON.stringify({
    event: "activation_exchange_failed",
    reason,
    session: sessionId.slice(0, 8),
    detail,
  }));
}

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
    throw new Error(`generateLink: ${linkError.message}`);
  }

  const tokenHash = linkData.properties?.hashed_token;
  if (!tokenHash) {
    throw new Error("generateLink: missing token hash");
  }

  let lastError = "no session returned";
  for (const otpType of ["magiclink", "email"] as const) {
    const { data: authData, error: authError } = await anon.auth.verifyOtp({
      type: otpType,
      token_hash: tokenHash,
    });
    if (!authError && authData.session) {
      return authData.session;
    }
    lastError = authError?.message ?? lastError;
  }
  throw new Error(`verifyOtp: ${lastError}`);
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  if (req.method !== "POST") {
    return jsonResponse({ error: "method_not_allowed" }, 405);
  }

  let sessionId = "";
  try {
    const body = await req.json().catch(() => null);
    sessionId = typeof body?.session_id === "string" ? body.session_id : "";
    const qrToken = typeof body?.qr_token === "string" ? body.qr_token : "";

    if (!UUID_RE.test(sessionId) || !QR_TOKEN_RE.test(qrToken)) {
      return jsonResponse({ error: "invalid_request" }, 400);
    }

    if (!checkRateLimit(clientIp(req))) {
      logFailure("rate_limited", sessionId);
      return jsonResponse({ error: "rate_limited" }, 429);
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

    // One-time claim: flips APPROVED -> EXPIRED and returns the approver. A second call
    // (double tap, retry, or anyone else) gets nothing.
    const { data: userId, error: claimError } = await admin.rpc("claim_approved_activation", {
      session_id: sessionId,
      session_qr_token: qrToken,
    });
    if (claimError) {
      logFailure("claim_error", sessionId, claimError.message);
      return jsonResponse({ error: "server_error" }, 500);
    }
    if (!userId) {
      logFailure("not_approved", sessionId);
      return jsonResponse({ error: "not_approved" }, 409);
    }

    const { data: userData, error: userError } = await admin.auth.admin.getUserById(userId);
    if (userError || !userData.user) {
      logFailure("user_not_found", sessionId, userError?.message);
      return jsonResponse({ error: "user_not_found" }, 404);
    }

    const email = userData.user.email;
    if (!email) {
      logFailure("no_email", sessionId);
      return jsonResponse({ error: "account_has_no_email" }, 422);
    }

    const authSession = await mintSessionForUser(admin, anon, email);

    return jsonResponse({
      access_token: authSession.access_token,
      refresh_token: authSession.refresh_token,
      expires_in: authSession.expires_in,
      token_type: authSession.token_type,
      user_id: authSession.user.id,
    });
  } catch (error) {
    logFailure("exception", sessionId, error instanceof Error ? error.message : String(error));
    return jsonResponse({ error: "server_error" }, 500);
  }
});
