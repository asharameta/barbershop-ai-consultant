CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;

CREATE FUNCTION public.slugify(input text)
    RETURNS text
    LANGUAGE sql
    IMMUTABLE STRICT PARALLEL SAFE
AS $$
    SELECT trim(BOTH '-' FROM
               regexp_replace(lower(public.unaccent('public.unaccent', input)), '[^a-z0-9]+', '-', 'g'));
$$;