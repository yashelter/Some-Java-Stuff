DROP TABLE IF EXISTS public.filter_rules;

CREATE TABLE IF NOT EXISTS public.filter_rules (
  id serial NOT NULL PRIMARY KEY,
  filter_id bigint NOT NULL,
  rule_id bigint NOT NULL,
  field_name text NOT NULL,
  filter_function_name text NOT NULL,
  filter_value text NOT NULL
);