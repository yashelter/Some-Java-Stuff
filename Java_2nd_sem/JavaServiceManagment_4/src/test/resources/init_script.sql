DROP TABLE IF EXISTS public.filter_rules;

CREATE TABLE IF NOT EXISTS public.filter_rules (
  id serial NOT NULL PRIMARY KEY,
  filter_id bigint NOT NULL,
  rule_id bigint NOT NULL,
  field_name text NOT NULL,
  filter_function_name text NOT NULL,
  filter_value text NOT NULL
);

DROP TABLE IF EXISTS public.deduplication_rules;

CREATE TABLE IF NOT EXISTS public.deduplication_rules (
  id serial NOT NULL PRIMARY KEY,
  deduplication_id bigint NOT NULL,
  rule_id bigint NOT NULL,
  field_name text NOT NULL,
  time_to_live_sec bigint NOT NULL,
  is_active bool NOT NULL
);

DROP TABLE IF EXISTS public.enrichment_rules;

CREATE TABLE IF NOT EXISTS public.enrichment_rules (
  id serial NOT NULL PRIMARY KEY,
  enrichment_id bigint NOT NULL,
  rule_id bigint NOT NULL,
  field_name text NOT NULL,
  field_name_enrichment text NOT NULL,
  field_value text NOT NULL,
  field_value_default text NOT NULL
);