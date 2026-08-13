CREATE OR REPLACE FUNCTION public.notify_vacancy()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  m public.matches%ROWTYPE;
  v_confirmed INTEGER;
  v_waiting INTEGER;
  v_left INTEGER;
BEGIN
  SELECT * INTO m FROM public.matches WHERE id = COALESCE(NEW.match_id, OLD.match_id);
  IF NOT FOUND OR m.status <> 'open' OR m.starts_at < now() THEN
    RETURN COALESCE(NEW, OLD);
  END IF;

  SELECT count(*) INTO v_confirmed FROM public.match_participants
    WHERE match_id = m.id AND status = 'confirmed';
  SELECT count(*) INTO v_waiting FROM public.match_participants
    WHERE match_id = m.id AND status = 'waitlist';

  v_left := m.total_slots - v_confirmed;
  IF v_left <= 0 OR v_waiting > 0 THEN
    RETURN COALESCE(NEW, OLD);
  END IF;

  INSERT INTO public.notifications (user_id, type, title, body, match_id)
  SELECT p.id, 'vacancy',
         'Vaga liberada: ' || m.venue,
         'Faltam ' || v_left || ' vaga(s) • ' || to_char(m.starts_at AT TIME ZONE 'America/Sao_Paulo', 'DD/MM HH24:MI'),
         m.id
  FROM public.profiles p
  WHERE p.id <> m.organizer_id
    AND p.is_banned = false
    AND p.is_available = true
    AND p.lat IS NOT NULL AND p.lng IS NOT NULL
    AND public.distance_km(p.lat, p.lng, m.lat, m.lng) <= GREATEST(p.radius_km, 20)
    AND NOT EXISTS (
      SELECT 1 FROM public.match_participants mp
      WHERE mp.match_id = m.id AND mp.user_id = p.id AND mp.status <> 'cancelled'
    );

  RETURN COALESCE(NEW, OLD);
END;
$$;

DROP TRIGGER IF EXISTS trg_notify_vacancy_del ON public.match_participants;
CREATE TRIGGER trg_notify_vacancy_del
AFTER DELETE ON public.match_participants
FOR EACH ROW EXECUTE FUNCTION public.notify_vacancy();

DROP TRIGGER IF EXISTS trg_notify_vacancy_upd ON public.match_participants;
CREATE TRIGGER trg_notify_vacancy_upd
AFTER UPDATE OF status ON public.match_participants
FOR EACH ROW WHEN (OLD.status <> NEW.status AND NEW.status = 'cancelled')
EXECUTE FUNCTION public.notify_vacancy();

CREATE OR REPLACE FUNCTION public.notify_nearby_players()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.notifications (user_id, type, title, body, match_id)
  SELECT p.id, 'new_match',
         'Faltam ' || NEW.total_slots || ' vagas: ' || NEW.venue,
         to_char(NEW.starts_at AT TIME ZONE 'America/Sao_Paulo', 'DD/MM HH24:MI') || ' • ' || NEW.venue,
         NEW.id
  FROM public.profiles p
  WHERE p.id <> NEW.organizer_id
    AND p.is_banned = false
    AND p.lat IS NOT NULL AND p.lng IS NOT NULL
    AND public.distance_km(p.lat, p.lng, NEW.lat, NEW.lng) <= GREATEST(p.radius_km, 20);
  RETURN NEW;
END;
$$;