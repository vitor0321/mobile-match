import { useEffect, useState } from "react";
import type { Session, User } from "@supabase/supabase-js";
import { supabase } from "@/integrations/supabase/client";

export type Profile = {
  id: string;
  full_name: string;
  nickname: string | null;
  avatar_url: string | null;
  phone: string | null;
  position: string | null;
  level: string;
  city: string | null;
  lat: number | null;
  lng: number | null;
  is_available: boolean;
  available_until: string | null;
  radius_km: number;
  pix_key: string | null;
  rating: number;
  matches_played: number;
};

export function useAuth() {
  const [session, setSession] = useState<Session | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const { data: sub } = supabase.auth.onAuthStateChange((_event, s) => {
      setSession(s);
      setUser(s?.user ?? null);
      if (!s?.user) setProfile(null);
    });
    supabase.auth.getSession().then(({ data }) => {
      setSession(data.session);
      setUser(data.session?.user ?? null);
      setLoading(false);
    });
    return () => sub.subscription.unsubscribe();
  }, []);

  useEffect(() => {
    if (!user) return;
    let active = true;
    supabase
      .from("profiles")
      .select("*")
      .eq("id", user.id)
      .maybeSingle()
      .then(({ data }) => {
        if (active) setProfile((data as Profile) ?? null);
      });
    return () => {
      active = false;
    };
  }, [user?.id]);

  async function signOut() {
    await supabase.auth.signOut();
    setProfile(null);
  }

  return { session, user, profile, setProfile, loading, signOut };
}
