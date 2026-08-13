-- ENUMS
CREATE TYPE public.app_role AS ENUM ('admin','moderator','user');
CREATE TYPE public.match_status AS ENUM ('open','full','cancelled','finished');
CREATE TYPE public.participant_status AS ENUM ('confirmed','waitlist','cancelled');
CREATE TYPE public.payment_status AS ENUM ('pending','paid','expired','refunded');
CREATE TYPE public.plan_tier AS ENUM ('free','pro','business');

-- PROFILES
CREATE TABLE public.profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  full_name TEXT NOT NULL DEFAULT '',
  nickname TEXT,
  avatar_url TEXT,
  phone TEXT,
  position TEXT,
  level TEXT NOT NULL DEFAULT 'Livre',
  city TEXT,
  lat DOUBLE PRECISION,
  lng DOUBLE PRECISION,
  is_available BOOLEAN NOT NULL DEFAULT false,
  available_until TIMESTAMPTZ,
  radius_km INTEGER NOT NULL DEFAULT 15,
  pix_key TEXT,
  rating NUMERIC(3,2) NOT NULL DEFAULT 5.00,
  matches_played INTEGER NOT NULL DEFAULT 0,
  is_banned BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
GRANT SELECT, INSERT, UPDATE ON public.profiles TO authenticated;
GRANT SELECT ON public.profiles TO anon;
GRANT ALL ON public.profiles TO service_role;
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- USER ROLES
CREATE TABLE public.user_roles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  role public.app_role NOT NULL DEFAULT 'user',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, role)
);
GRANT SELECT ON public.user_roles TO authenticated;
GRANT ALL ON public.user_roles TO service_role;
ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;

CREATE OR REPLACE FUNCTION public.has_role(_user_id UUID, _role public.app_role)
RETURNS BOOLEAN LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT EXISTS (SELECT 1 FROM public.user_roles WHERE user_id = _user_id AND role = _role)
$$;

-- MATCHES
CREATE TABLE public.matches (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  organizer_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  sport TEXT NOT NULL DEFAULT 'futsal',
  title TEXT NOT NULL,
  venue TEXT NOT NULL,
  address TEXT,
  neighborhood TEXT,
  lat DOUBLE PRECISION NOT NULL,
  lng DOUBLE PRECISION NOT NULL,
  starts_at TIMESTAMPTZ NOT NULL,
  duration_min INTEGER NOT NULL DEFAULT 60,
  total_slots INTEGER NOT NULL DEFAULT 12,
  price_cents INTEGER NOT NULL DEFAULT 0,
  platform_fee_cents INTEGER NOT NULL DEFAULT 0,
  level TEXT NOT NULL DEFAULT 'Livre',
  rules TEXT[] NOT NULL DEFAULT '{}',
  pix_key TEXT,
  status public.match_status NOT NULL DEFAULT 'open',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX matches_starts_at_idx ON public.matches (starts_at);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.matches TO authenticated;
GRANT SELECT ON public.matches TO anon;
GRANT ALL ON public.matches TO service_role;
ALTER TABLE public.matches ENABLE ROW LEVEL SECURITY;

-- PARTICIPANTS
CREATE TABLE public.match_participants (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  match_id UUID NOT NULL REFERENCES public.matches(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  status public.participant_status NOT NULL DEFAULT 'confirmed',
  payment_status public.payment_status NOT NULL DEFAULT 'pending',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (match_id, user_id)
);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.match_participants TO authenticated;
GRANT SELECT ON public.match_participants TO anon;
GRANT ALL ON public.match_participants TO service_role;
ALTER TABLE public.match_participants ENABLE ROW LEVEL SECURITY;

-- PAYMENTS
CREATE TABLE public.payments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  match_id UUID REFERENCES public.matches(id) ON DELETE SET NULL,
  kind TEXT NOT NULL DEFAULT 'match_fee',
  amount_cents INTEGER NOT NULL,
  method TEXT NOT NULL DEFAULT 'pix',
  pix_key TEXT,
  pix_payload TEXT,
  txid TEXT,
  status public.payment_status NOT NULL DEFAULT 'pending',
  paid_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
GRANT SELECT, INSERT, UPDATE ON public.payments TO authenticated;
GRANT ALL ON public.payments TO service_role;
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;

-- SUBSCRIPTIONS
CREATE TABLE public.subscriptions (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  plan public.plan_tier NOT NULL DEFAULT 'free',
  status public.payment_status NOT NULL DEFAULT 'pending',
  current_period_end TIMESTAMPTZ,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
GRANT SELECT, INSERT, UPDATE ON public.subscriptions TO authenticated;
GRANT ALL ON public.subscriptions TO service_role;
ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;

-- NOTIFICATIONS
CREATE TABLE public.notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  type TEXT NOT NULL DEFAULT 'new_match',
  title TEXT NOT NULL,
  body TEXT,
  match_id UUID REFERENCES public.matches(id) ON DELETE CASCADE,
  read_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX notifications_user_idx ON public.notifications (user_id, created_at DESC);
GRANT SELECT, UPDATE, DELETE ON public.notifications TO authenticated;
GRANT ALL ON public.notifications TO service_role;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- RATINGS
CREATE TABLE public.ratings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  match_id UUID NOT NULL REFERENCES public.matches(id) ON DELETE CASCADE,
  rater_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  rated_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  stars INTEGER NOT NULL CHECK (stars BETWEEN 1 AND 5),
  comment TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (match_id, rater_id, rated_id)
);
GRANT SELECT, INSERT ON public.ratings TO authenticated;
GRANT SELECT ON public.ratings TO anon;
GRANT ALL ON public.ratings TO service_role;
ALTER TABLE public.ratings ENABLE ROW LEVEL SECURITY;

-- REPORTS
CREATE TABLE public.reports (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reporter_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  reported_user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  match_id UUID REFERENCES public.matches(id) ON DELETE SET NULL,
  reason TEXT NOT NULL,
  details TEXT,
  status TEXT NOT NULL DEFAULT 'open',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
GRANT SELECT, INSERT ON public.reports TO authenticated;
GRANT ALL ON public.reports TO service_role;
ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;

-- POLICIES
CREATE POLICY "profiles_public_read" ON public.profiles FOR SELECT USING (true);
CREATE POLICY "profiles_insert_own" ON public.profiles FOR INSERT TO authenticated WITH CHECK (auth.uid() = id);
CREATE POLICY "profiles_update_own" ON public.profiles FOR UPDATE TO authenticated USING (auth.uid() = id) WITH CHECK (auth.uid() = id);
CREATE POLICY "profiles_admin_update" ON public.profiles FOR UPDATE TO authenticated USING (public.has_role(auth.uid(),'admin')) WITH CHECK (public.has_role(auth.uid(),'admin'));

CREATE POLICY "roles_read_own" ON public.user_roles FOR SELECT TO authenticated USING (auth.uid() = user_id OR public.has_role(auth.uid(),'admin'));

CREATE POLICY "matches_public_read" ON public.matches FOR SELECT USING (true);
CREATE POLICY "matches_insert_own" ON public.matches FOR INSERT TO authenticated WITH CHECK (auth.uid() = organizer_id);
CREATE POLICY "matches_update_own" ON public.matches FOR UPDATE TO authenticated USING (auth.uid() = organizer_id OR public.has_role(auth.uid(),'admin')) WITH CHECK (auth.uid() = organizer_id OR public.has_role(auth.uid(),'admin'));
CREATE POLICY "matches_delete_own" ON public.matches FOR DELETE TO authenticated USING (auth.uid() = organizer_id OR public.has_role(auth.uid(),'admin'));

CREATE POLICY "participants_read" ON public.match_participants FOR SELECT USING (true);
CREATE POLICY "participants_insert_own" ON public.match_participants FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
CREATE POLICY "participants_update" ON public.match_participants FOR UPDATE TO authenticated USING (auth.uid() = user_id OR EXISTS (SELECT 1 FROM public.matches m WHERE m.id = match_id AND m.organizer_id = auth.uid())) WITH CHECK (auth.uid() = user_id OR EXISTS (SELECT 1 FROM public.matches m WHERE m.id = match_id AND m.organizer_id = auth.uid()));
CREATE POLICY "participants_delete_own" ON public.match_participants FOR DELETE TO authenticated USING (auth.uid() = user_id OR EXISTS (SELECT 1 FROM public.matches m WHERE m.id = match_id AND m.organizer_id = auth.uid()));

CREATE POLICY "payments_own" ON public.payments FOR SELECT TO authenticated USING (auth.uid() = user_id OR public.has_role(auth.uid(),'admin'));
CREATE POLICY "payments_insert_own" ON public.payments FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
CREATE POLICY "payments_update_own" ON public.payments FOR UPDATE TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "subs_own" ON public.subscriptions FOR SELECT TO authenticated USING (auth.uid() = user_id OR public.has_role(auth.uid(),'admin'));
CREATE POLICY "subs_insert_own" ON public.subscriptions FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
CREATE POLICY "subs_update_own" ON public.subscriptions FOR UPDATE TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "notifications_own" ON public.notifications FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "notifications_update_own" ON public.notifications FOR UPDATE TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "notifications_delete_own" ON public.notifications FOR DELETE TO authenticated USING (auth.uid() = user_id);

CREATE POLICY "ratings_read" ON public.ratings FOR SELECT USING (true);
CREATE POLICY "ratings_insert_own" ON public.ratings FOR INSERT TO authenticated WITH CHECK (auth.uid() = rater_id);

CREATE POLICY "reports_read_own" ON public.reports FOR SELECT TO authenticated USING (auth.uid() = reporter_id OR public.has_role(auth.uid(),'admin'));
CREATE POLICY "reports_insert_own" ON public.reports FOR INSERT TO authenticated WITH CHECK (auth.uid() = reporter_id);

-- HELPERS
CREATE OR REPLACE FUNCTION public.distance_km(lat1 DOUBLE PRECISION, lng1 DOUBLE PRECISION, lat2 DOUBLE PRECISION, lng2 DOUBLE PRECISION)
RETURNS DOUBLE PRECISION LANGUAGE sql IMMUTABLE AS $$
  SELECT 6371 * acos(
    least(1, greatest(-1,
      cos(radians(lat1)) * cos(radians(lat2)) * cos(radians(lng2) - radians(lng1))
      + sin(radians(lat1)) * sin(radians(lat2))
    ))
  )
$$;

CREATE OR REPLACE FUNCTION public.touch_updated_at() RETURNS TRIGGER LANGUAGE plpgsql SET search_path = public AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END; $$;
CREATE TRIGGER profiles_touch BEFORE UPDATE ON public.profiles FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();
CREATE TRIGGER matches_touch BEFORE UPDATE ON public.matches FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();

-- Auto-create profile + free subscription on signup
CREATE OR REPLACE FUNCTION public.handle_new_user() RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  INSERT INTO public.profiles (id, full_name)
  VALUES (NEW.id, COALESCE(NEW.raw_user_meta_data->>'full_name', NEW.raw_user_meta_data->>'name', ''))
  ON CONFLICT (id) DO NOTHING;
  INSERT INTO public.user_roles (user_id, role) VALUES (NEW.id, 'user') ON CONFLICT DO NOTHING;
  INSERT INTO public.subscriptions (user_id, plan, status) VALUES (NEW.id, 'free', 'paid') ON CONFLICT DO NOTHING;
  RETURN NEW;
END; $$;
CREATE TRIGGER on_auth_user_created AFTER INSERT ON auth.users FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Notify nearby available players when a match is created
CREATE OR REPLACE FUNCTION public.notify_nearby_players() RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  INSERT INTO public.notifications (user_id, type, title, body, match_id)
  SELECT p.id, 'new_match',
         'Nova vaga perto de você',
         NEW.venue || ' • ' || to_char(NEW.starts_at AT TIME ZONE 'America/Sao_Paulo', 'DD/MM HH24:MI') || ' • ' || NEW.total_slots || ' vagas',
         NEW.id
  FROM public.profiles p
  WHERE p.id <> NEW.organizer_id
    AND p.is_banned = false
    AND p.lat IS NOT NULL AND p.lng IS NOT NULL
    AND public.distance_km(p.lat, p.lng, NEW.lat, NEW.lng) <= GREATEST(p.radius_km, 20);
  RETURN NEW;
END; $$;
CREATE TRIGGER matches_notify AFTER INSERT ON public.matches FOR EACH ROW EXECUTE FUNCTION public.notify_nearby_players();

-- Promote first waitlisted player when a spot frees up
CREATE OR REPLACE FUNCTION public.promote_waitlist() RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  v_total INTEGER;
  v_confirmed INTEGER;
  v_next public.match_participants%ROWTYPE;
BEGIN
  SELECT total_slots INTO v_total FROM public.matches WHERE id = NEW.match_id;
  SELECT count(*) INTO v_confirmed FROM public.match_participants WHERE match_id = NEW.match_id AND status = 'confirmed';
  IF v_confirmed < v_total THEN
    SELECT * INTO v_next FROM public.match_participants
      WHERE match_id = NEW.match_id AND status = 'waitlist' ORDER BY created_at LIMIT 1;
    IF FOUND THEN
      UPDATE public.match_participants SET status = 'confirmed' WHERE id = v_next.id;
      INSERT INTO public.notifications (user_id, type, title, body, match_id)
      VALUES (v_next.user_id, 'promoted', 'Sua vaga foi confirmada!', 'Abriu uma vaga e você entrou no jogo.', NEW.match_id);
    END IF;
  END IF;
  RETURN NEW;
END; $$;
CREATE TRIGGER participants_promote AFTER UPDATE OF status ON public.match_participants FOR EACH ROW WHEN (NEW.status = 'cancelled') EXECUTE FUNCTION public.promote_waitlist();

ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
ALTER PUBLICATION supabase_realtime ADD TABLE public.match_participants;
ALTER PUBLICATION supabase_realtime ADD TABLE public.matches;