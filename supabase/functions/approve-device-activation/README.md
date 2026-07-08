# Deprecated — not used in production

The activation companion calls the `approve_device_activation` Postgres RPC directly with the signed-in user's JWT.

This edge function is kept for reference only. Do not deploy it unless you intentionally want a redundant HTTP approval path.

Production flow:

1. TV creates a `PENDING` session
2. Companion signs in → `approve_device_activation(code)` RPC
3. TV polls → `exchange-activation-session` edge function
