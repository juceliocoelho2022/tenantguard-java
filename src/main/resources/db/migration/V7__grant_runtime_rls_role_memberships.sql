-- Sprint 11: make the runtime login explicitly capable of SET ROLE without
-- inheriting tenant/audit privileges outside the guarded transaction scope.
-- PostgreSQL 16+ supports per-membership INHERIT and SET options.

GRANT tenantguard_app
TO SESSION_USER
WITH INHERIT FALSE, SET TRUE;

GRANT tenantguard_audit
TO SESSION_USER
WITH INHERIT FALSE, SET TRUE;
