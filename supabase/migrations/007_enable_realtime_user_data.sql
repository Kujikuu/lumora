-- Enable Realtime for cloud user-data tables (cross-device sync on TV)

alter publication supabase_realtime add table public.favorites;
alter publication supabase_realtime add table public.watch_history;
alter publication supabase_realtime add table public.user_settings;
