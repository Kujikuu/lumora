const translations = {
  en: {
    page_title: "Lumora Play — Activate TV",
    page_description: "Sign in to link your Lumora Play account to your Android TV.",
    heading_activate: "Activate Your TV",
    step_code: "Enter code",
    step_signin: "Sign in",
    step_done: "TV links",
    success_title: "Your TV is linked",
    success_signed_in: "Signed in as",
    success_hint: "Return to your Android TV — it should sign in automatically within a few seconds.",
    session_signed_in: "Signed in as",
    sign_out: "Sign out",
    label_code: "Activation code",
    label_code_hint: "from your TV screen",
    label_email: "Email",
    label_password: "Password",
    btn_sign_in: "Sign in & link TV",
    btn_sign_up: "Create account & link TV",
    divider_or: "or",
    btn_google: "Continue with Google",
    btn_link_tv: "Link TV now",
    form_hint: "Scan the QR code on your TV or enter the code shown on screen.",
    form_hint_signed_in: "Tap Link TV now to approve the code on your TV.",
    loading_working: "Working…",
    loading_linking: "Linking your TV…",
    loading_google: "Opening Google…",
    loading_signin: "Signing in…",
    loading_signup: "Creating account…",
    footer: "iptv.afifistudio.com · Device activation",
    err_missing_config: "Missing config.js — set SUPABASE_URL and SUPABASE_ANON_KEY.",
    err_invalid_code: "That TV code is invalid or expired. Get a fresh code from your TV.",
    err_code_already_used: "This TV code was already used. Get a fresh code from your TV.",
    err_rate_limited: "Too many attempts. Wait a minute and try again.",
    err_email_confirm: "Confirm your email first, then return here.",
    err_invalid_credentials: "Invalid email or password.",
    err_enter_code: "Enter the activation code from your TV.",
    err_sign_in_first: "Sign in first.",
    err_code_email_password: "Code, email, and password are required.",
    err_google_code_first: "Enter the TV code first, then continue with Google.",
    info_signed_in_enter_code: "Signed in. Enter your TV code and tap Link TV.",
    info_check_email: "Check your email to confirm, then return here.",
    info_signed_out: "Signed out.",
    account_fallback: "your account",
  },
  ar: {
    page_title: "Lumora Play — تفعيل التلفاز",
    page_description: "سجّل الدخول لربط حساب Lumora Play بتلفاز Android.",
    heading_activate: "فعّل تلفازك",
    step_code: "أدخل الرمز",
    step_signin: "تسجيل الدخول",
    step_done: "ربط التلفاز",
    success_title: "تم ربط تلفازك",
    success_signed_in: "مسجّل الدخول باسم",
    success_hint: "ارجع إلى تلفاز Android — يفترض أن يسجّل الدخول تلقائيًا خلال ثوانٍ.",
    session_signed_in: "مسجّل الدخول باسم",
    sign_out: "تسجيل الخروج",
    label_code: "رمز التفعيل",
    label_code_hint: "من شاشة التلفاز",
    label_email: "البريد الإلكتروني",
    label_password: "كلمة المرور",
    btn_sign_in: "تسجيل الدخول وربط التلفاز",
    btn_sign_up: "إنشاء حساب وربط التلفاز",
    divider_or: "أو",
    btn_google: "المتابعة مع Google",
    btn_link_tv: "ربط التلفاز الآن",
    form_hint: "امسح رمز QR على التلفاز أو أدخل الرمز الظاهر على الشاشة.",
    form_hint_signed_in: "اضغط «ربط التلفاز الآن» للموافقة على الرمز.",
    loading_working: "جارٍ العمل…",
    loading_linking: "جارٍ ربط التلفاز…",
    loading_google: "جارٍ فتح Google…",
    loading_signin: "جارٍ تسجيل الدخول…",
    loading_signup: "جارٍ إنشاء الحساب…",
    footer: "iptv.afifistudio.com · تفعيل الجهاز",
    err_missing_config: "config.js مفقود — عيّن SUPABASE_URL و SUPABASE_ANON_KEY.",
    err_invalid_code: "رمز التلفاز غير صالح أو منتهٍ. احصل على رمز جديد من التلفاز.",
    err_code_already_used: "تم استخدام رمز التلفاز بالفعل. احصل على رمز جديد من التلفاز.",
    err_rate_limited: "محاولات كثيرة. انتظر دقيقة ثم حاول مرة أخرى.",
    err_email_confirm: "أكّد بريدك أولًا ثم عد إلى هنا.",
    err_invalid_credentials: "البريد الإلكتروني أو كلمة المرور غير صحيحة.",
    err_enter_code: "أدخل رمز التفعيل من التلفاز.",
    err_sign_in_first: "سجّل الدخول أولًا.",
    err_code_email_password: "الرمز والبريد وكلمة المرور مطلوبة.",
    err_google_code_first: "أدخل رمز التلفاز أولًا ثم تابع مع Google.",
    info_signed_in_enter_code: "تم تسجيل الدخول. أدخل رمز التلفاز واضغط ربط التلفاز.",
    info_check_email: "تحقق من بريدك للتأكيد ثم عد إلى هنا.",
    info_signed_out: "تم تسجيل الخروج.",
    account_fallback: "حسابك",
  },
};

let locale = "en";

function detectLocale() {
  const lang = (navigator.language || "en").toLowerCase();
  return lang.startsWith("ar") ? "ar" : "en";
}

export function applyLocale() {
  locale = detectLocale();
  document.documentElement.lang = locale;
  document.documentElement.dir = locale === "ar" ? "rtl" : "ltr";
  document.title = t("page_title");

  const description = document.querySelector('meta[name="description"]');
  if (description) description.content = t("page_description");

  const ogTitle = document.querySelector('meta[property="og:title"]');
  if (ogTitle) ogTitle.content = t("page_title");

  const ogDescription = document.querySelector('meta[property="og:description"]');
  if (ogDescription) ogDescription.content = t("page_description");

  document.querySelectorAll("[data-i18n]").forEach((node) => {
    const key = node.getAttribute("data-i18n");
    if (key) node.textContent = t(key);
  });

  document.querySelectorAll("[data-i18n-placeholder]").forEach((node) => {
    const key = node.getAttribute("data-i18n-placeholder");
    if (key) node.placeholder = t(key);
  });

  const steps = document.getElementById("steps");
  if (steps) steps.setAttribute("aria-label", locale === "ar" ? "خطوات التفعيل" : "Activation steps");
}

export function t(key) {
  return translations[locale]?.[key] ?? translations.en[key] ?? key;
}

export function mapErr(e) {
  const text = e?.message ?? String(e);
  if (/invalid or expired activation/i.test(text)) return t("err_invalid_code");
  if (/already used|already linked|status = 'EXPIRED'/i.test(text)) return t("err_code_already_used");
  if (/too many attempts|rate limit/i.test(text)) return t("err_rate_limited");
  if (/email not confirmed/i.test(text)) return t("err_email_confirm");
  if (/invalid login credentials/i.test(text)) return t("err_invalid_credentials");
  return text;
}
