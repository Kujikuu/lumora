-- TV devices may have incorrect clocks; never trust client-supplied expires_at.
alter table public.device_activation_sessions
  alter column expires_at set default (now() + interval '15 minutes');
