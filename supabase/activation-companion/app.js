import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";
import { applyLocale, mapErr, t } from "./i18n.js";

const APP_VERSION = "3.3.0";
const CODE_KEY = "iptv_cinema_activation_code";
const ACTIVATION_PARAM = "activation";
const CODE_RE = /^[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{2}$/;

applyLocale();

const config = window.IPTV_CINEMA_CONFIG ?? {};
const supabaseUrl = config.SUPABASE_URL?.trim();
const supabaseAnonKey = config.SUPABASE_ANON_KEY?.trim();

const $ = (id) => document.getElementById(id);
const els = {
  form: $("auth-form"),
  authFields: $("auth-fields"),
  formHint: $("form-hint"),
  steps: $("steps"),
  successPanel: $("success-panel"),
  sessionBanner: $("session-banner"),
  sessionEmail: $("session-email"),
  codeInput: $("code"),
  emailInput: $("email"),
  passwordInput: $("password"),
  signInBtn: $("sign-in"),
  signUpBtn: $("sign-up"),
  googleBtn: $("google-sign-in"),
  linkTvBtn: $("link-tv"),
  signOutBtn: $("sign-out"),
  message: $("message"),
  loading: $("loading"),
};

let supabase = null;
let busy = false;

function norm(raw) {
  return (raw ?? "").trim().toUpperCase();
}

function validCode(raw) {
  return CODE_RE.test(norm(raw));
}

function formatCodeInput(raw) {
  const chars = norm(raw).replace(/[^A-HJ-NP-Z2-9]/g, "").slice(0, 6);
  if (chars.length <= 4) return chars;
  return `${chars.slice(0, 4)}-${chars.slice(4)}`;
}

function persist(code) {
  const c = norm(code);
  if (!validCode(c)) return "";
  sessionStorage.setItem(CODE_KEY, c);
  localStorage.setItem(CODE_KEY, c);
  document.cookie = `${CODE_KEY}=${encodeURIComponent(c)}; path=/; max-age=900; SameSite=Lax; Secure`;
  return c;
}

function loadCode() {
  for (const s of [sessionStorage, localStorage]) {
    const c = norm(s.getItem(CODE_KEY));
    if (validCode(c)) return c;
  }
  const m = document.cookie.match(new RegExp(`(?:^|; )${CODE_KEY}=([^;]*)`));
  if (m && validCode(m[1])) return norm(decodeURIComponent(m[1]));
  return "";
}

function readCodeFromUrl() {
  const search = new URLSearchParams(location.search);
  const hash = new URLSearchParams(location.hash.replace(/^#/, ""));

  for (const params of [hash, search]) {
    const a = norm(params.get(ACTIVATION_PARAM));
    if (validCode(a)) return a;
  }

  const legacy = norm(search.get("code"));
  if (validCode(legacy)) return legacy;

  return "";
}

function oauthCodeInUrl() {
  const c = location.search.match(/[?&]code=([^&]+)/);
  return c && !validCode(decodeURIComponent(c[1]));
}

function getCode() {
  const input = norm(els.codeInput?.value);
  if (validCode(input)) return input;
  const url = readCodeFromUrl();
  if (url) return url;
  return loadCode();
}

function fillCodeInput(code) {
  if (code && els.codeInput) {
    els.codeInput.value = formatCodeInput(code);
    persist(code);
    setStep("code", validCode(code) ? "done" : "active");
  }
}

function setStep(name, state) {
  const step = els.steps?.querySelector(`[data-step="${name}"]`);
  if (!step) return;
  step.classList.remove("active", "done");
  if (state) step.classList.add(state);
}

function updateSteps({ hasCode, signedIn, complete }) {
  if (complete) {
    setStep("code", "done");
    setStep("signin", "done");
    setStep("done", "done");
    return;
  }
  setStep("code", hasCode ? "done" : "active");
  setStep("signin", signedIn ? "done" : hasCode ? "active" : "");
  setStep("done", "");
}

function redirectUrl(code) {
  persist(code);
  const u = new URL(location.origin);
  u.searchParams.set(ACTIVATION_PARAM, norm(code));
  return u.toString();
}

function stripOAuthCodeFromUrl() {
  const u = new URL(location.href);
  const q = u.searchParams.get("code");
  if (q && !validCode(q)) u.searchParams.delete("code");
  history.replaceState({}, "", u.pathname + u.search + u.hash);
}

function msg(text, type = "") {
  els.message.textContent = text;
  els.message.className = "msg" + (type ? ` ${type}` : "");
  els.message.classList.toggle("hidden", !text);
}

function loading(on, label = null) {
  els.loading.classList.toggle("visible", on);
  if (on) els.loading.querySelector("span").textContent = label ?? t("loading_working");
}

function setBusy(on) {
  busy = on;
  for (const el of [els.signInBtn, els.signUpBtn, els.googleBtn, els.linkTvBtn, els.codeInput, els.emailInput, els.passwordInput]) {
    if (el) el.disabled = on;
  }
}

function showSuccess(email) {
  els.successPanel.classList.add("visible");
  $("success-email").textContent = email ?? t("account_fallback");
  updateSteps({ complete: true });
}

function showBanner(session) {
  const email = session?.user?.email;
  const signedIn = Boolean(email);

  if (signedIn) {
    els.sessionBanner.classList.add("visible");
    els.sessionEmail.textContent = email;
    els.authFields?.classList.add("signed-in-hidden");
    if (els.linkTvBtn) els.linkTvBtn.hidden = false;
    if (els.formHint) els.formHint.textContent = t("form_hint_signed_in");
  } else {
    els.sessionBanner.classList.remove("visible");
    els.authFields?.classList.remove("signed-in-hidden");
    if (els.linkTvBtn) els.linkTvBtn.hidden = true;
    if (els.formHint) els.formHint.textContent = t("form_hint");
  }

  updateSteps({ hasCode: Boolean(getCode()), signedIn });
}

async function approveTv(session) {
  const code = getCode();
  if (!code) {
    msg(t("err_enter_code"), "error");
    els.codeInput?.focus();
    return false;
  }
  if (!session) {
    msg(t("err_sign_in_first"), "error");
    return false;
  }

  setBusy(true);
  loading(true, t("loading_linking"));
  msg("");

  try {
    const { error } = await supabase.rpc("approve_device_activation", {
      activation_code: code,
    });
    if (error) throw error;

    sessionStorage.removeItem(CODE_KEY);
    localStorage.removeItem(CODE_KEY);
    document.cookie = `${CODE_KEY}=; path=/; max-age=0`;
    stripOAuthCodeFromUrl();
    showSuccess(session.user?.email);
    return true;
  } catch (e) {
    msg(mapErr(e), "error");
    return false;
  } finally {
    setBusy(false);
    loading(false);
  }
}

async function finishOAuthReturn() {
  if (!oauthCodeInUrl()) return null;

  const params = new URLSearchParams(location.search);
  const oauthCode = params.get("code");
  const { data, error } = await supabase.auth.exchangeCodeForSession(oauthCode);
  if (error) throw error;
  return data.session;
}

async function onSessionReady(session) {
  if (!session) return;
  showBanner(session);
  fillCodeInput(getCode());
  if (getCode()) {
    await approveTv(session);
  } else {
    msg(t("info_signed_in_enter_code"), "info");
  }
}

async function googleSignIn() {
  const code = getCode();
  if (!code) {
    msg(t("err_google_code_first"), "error");
    els.codeInput?.focus();
    return;
  }
  persist(code);
  setBusy(true);
  loading(true, t("loading_google"));
  const { error } = await supabase.auth.signInWithOAuth({
    provider: "google",
    options: { redirectTo: redirectUrl(code) },
  });
  if (error) {
    msg(mapErr(error), "error");
    setBusy(false);
    loading(false);
  }
}

async function emailSignIn() {
  const code = getCode();
  const email = els.emailInput.value.trim();
  const password = els.passwordInput.value;
  if (!code || !email || !password) {
    msg(t("err_code_email_password"), "error");
    return;
  }
  setBusy(true);
  loading(true, t("loading_signin"));
  try {
    const { data, error } = await supabase.auth.signInWithPassword({ email, password });
    if (error) throw error;
    await onSessionReady(data.session);
  } catch (e) {
    msg(mapErr(e), "error");
  } finally {
    setBusy(false);
    loading(false);
  }
}

async function emailSignUp() {
  const code = getCode();
  const email = els.emailInput.value.trim();
  const password = els.passwordInput.value;
  if (!code || !email || !password) {
    msg(t("err_code_email_password"), "error");
    return;
  }
  setBusy(true);
  loading(true, t("loading_signup"));
  try {
    const { data, error } = await supabase.auth.signUp({
      email,
      password,
      options: { emailRedirectTo: redirectUrl(code) },
    });
    if (error) throw error;
    if (data.session) {
      await onSessionReady(data.session);
    } else {
      msg(t("info_check_email"), "info");
    }
  } catch (e) {
    msg(mapErr(e), "error");
  } finally {
    setBusy(false);
    loading(false);
  }
}

async function init() {
  if (!supabaseUrl || !supabaseAnonKey) {
    msg(t("err_missing_config"), "error");
    return;
  }

  const urlCode = readCodeFromUrl() || loadCode();
  fillCodeInput(urlCode);
  if (!urlCode) setStep("code", "active");

  supabase = createClient(supabaseUrl, supabaseAnonKey, {
    auth: { flowType: "pkce", persistSession: true, detectSessionInUrl: false },
  });

  try {
    const oauthSession = await finishOAuthReturn();
    if (oauthSession) {
      stripOAuthCodeFromUrl();
      await onSessionReady(oauthSession);
      return;
    }
  } catch (e) {
    msg(mapErr(e), "error");
    return;
  }

  const { data: { session } } = await supabase.auth.getSession();
  if (session) {
    showBanner(session);
    fillCodeInput(getCode());
  }
}

els.signInBtn?.addEventListener("click", () => !busy && emailSignIn());
els.signUpBtn?.addEventListener("click", () => !busy && emailSignUp());
els.googleBtn?.addEventListener("click", () => !busy && googleSignIn());
els.linkTvBtn?.addEventListener("click", async () => {
  if (busy) return;
  const { data: { session } } = await supabase.auth.getSession();
  await approveTv(session);
});
els.signOutBtn?.addEventListener("click", async () => {
  await supabase.auth.signOut();
  showBanner(null);
  msg(t("info_signed_out"), "info");
});
els.codeInput?.addEventListener("input", () => {
  const formatted = formatCodeInput(els.codeInput.value);
  els.codeInput.value = formatted;
  persist(formatted);
  updateSteps({ hasCode: validCode(formatted), signedIn: Boolean(els.sessionBanner?.classList.contains("visible")) });
});

init();
