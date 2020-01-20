--
-- PostgreSQL database dump
--

-- Dumped from database version 11.2
-- Dumped by pg_dump version 11.2

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: acled_source; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.acled_source (
    id integer NOT NULL,
    business_key text,
    data jsonb
);


ALTER TABLE public.acled_source OWNER TO postgres;

--
-- Name: acled_source_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.acled_source_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.acled_source_id_seq OWNER TO postgres;

--
-- Name: acled_source_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.acled_source_id_seq OWNED BY public.acled_source.id;


--
-- Name: acled_source id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.acled_source ALTER COLUMN id SET DEFAULT nextval('public.acled_source_id_seq'::regclass);


--
-- Data for Name: acled_source; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.acled_source (id, business_key, data) FROM stdin;
\.


--
-- Name: acled_source_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.acled_source_id_seq', 1, false);


--
-- Name: acled_source acled_source_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.acled_source
    ADD CONSTRAINT acled_source_pkey PRIMARY KEY (id);


--
-- Name: acled_source_data_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX acled_source_data_idx ON public.acled_source USING gin (data);


--
-- PostgreSQL database dump complete
--

