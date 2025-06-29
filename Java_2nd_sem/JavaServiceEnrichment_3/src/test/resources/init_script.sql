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