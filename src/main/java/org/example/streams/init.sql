CREATE TABLE public.vehicle (
    id integer NOT NULL,
    "created" timestamp without time zone NOT NULL,
    "modified" timestamp without time zone,
    name character varying(100) NOT NULL,
    category character varying(100) NOT NULL,
    registration_number character varying(100) NOT NULL,
    identification_number character varying(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.location (
    vehicle_id INTEGER NOT NULL,
    latitude FLOAT NOT NULL,
    longitude FLOAT NOT NULL,
    "timestamp" INTEGER NOT NULL
    );



CREATE SEQUENCE public.vehicle_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER TABLE ONLY public.vehicle ALTER COLUMN id SET DEFAULT nextval('public.vehicle_id_seq'::regclass);
ALTER TABLE ONLY public.location ALTER COLUMN vehicle_id SET DEFAULT nextval('public.vehicle_id_seq'::regclass);
