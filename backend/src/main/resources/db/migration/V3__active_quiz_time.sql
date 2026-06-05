alter table quiz_attempts
    add column active_duration_seconds bigint not null default 0,
    add column last_heartbeat_at timestamptz;

update quiz_attempts
set active_duration_seconds = greatest(0, floor(extract(epoch from (completed_at - started_at)))::bigint)
where status = 'COMPLETED'
  and completed_at is not null;
