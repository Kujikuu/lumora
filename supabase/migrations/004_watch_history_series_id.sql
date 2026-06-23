-- Add series_id to watch_history for episode resume / continue watching
alter table public.watch_history
  add column if not exists series_id text;

create index if not exists watch_history_profile_series_id_idx
  on public.watch_history (profile_id, series_id)
  where series_id is not null;
