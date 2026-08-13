export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[]

export type Database = {
  // Allows to automatically instantiate createClient with right options
  // instead of createClient<Database, { PostgrestVersion: 'XX' }>(URL, KEY)
  __InternalSupabase: {
    PostgrestVersion: "14.15"
  }
  public: {
    Tables: {
      match_participants: {
        Row: {
          created_at: string
          id: string
          match_id: string
          payment_status: Database["public"]["Enums"]["payment_status"]
          status: Database["public"]["Enums"]["participant_status"]
          user_id: string
        }
        Insert: {
          created_at?: string
          id?: string
          match_id: string
          payment_status?: Database["public"]["Enums"]["payment_status"]
          status?: Database["public"]["Enums"]["participant_status"]
          user_id: string
        }
        Update: {
          created_at?: string
          id?: string
          match_id?: string
          payment_status?: Database["public"]["Enums"]["payment_status"]
          status?: Database["public"]["Enums"]["participant_status"]
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "match_participants_match_id_fkey"
            columns: ["match_id"]
            isOneToOne: false
            referencedRelation: "matches"
            referencedColumns: ["id"]
          },
        ]
      }
      matches: {
        Row: {
          address: string | null
          created_at: string
          duration_min: number
          id: string
          lat: number
          level: string
          lng: number
          neighborhood: string | null
          organizer_id: string
          pix_key: string | null
          platform_fee_cents: number
          price_cents: number
          rules: string[]
          sport: string
          starts_at: string
          status: Database["public"]["Enums"]["match_status"]
          title: string
          total_slots: number
          updated_at: string
          venue: string
        }
        Insert: {
          address?: string | null
          created_at?: string
          duration_min?: number
          id?: string
          lat: number
          level?: string
          lng: number
          neighborhood?: string | null
          organizer_id: string
          pix_key?: string | null
          platform_fee_cents?: number
          price_cents?: number
          rules?: string[]
          sport?: string
          starts_at: string
          status?: Database["public"]["Enums"]["match_status"]
          title: string
          total_slots?: number
          updated_at?: string
          venue: string
        }
        Update: {
          address?: string | null
          created_at?: string
          duration_min?: number
          id?: string
          lat?: number
          level?: string
          lng?: number
          neighborhood?: string | null
          organizer_id?: string
          pix_key?: string | null
          platform_fee_cents?: number
          price_cents?: number
          rules?: string[]
          sport?: string
          starts_at?: string
          status?: Database["public"]["Enums"]["match_status"]
          title?: string
          total_slots?: number
          updated_at?: string
          venue?: string
        }
        Relationships: []
      }
      notifications: {
        Row: {
          body: string | null
          created_at: string
          id: string
          match_id: string | null
          read_at: string | null
          title: string
          type: string
          user_id: string
        }
        Insert: {
          body?: string | null
          created_at?: string
          id?: string
          match_id?: string | null
          read_at?: string | null
          title: string
          type?: string
          user_id: string
        }
        Update: {
          body?: string | null
          created_at?: string
          id?: string
          match_id?: string | null
          read_at?: string | null
          title?: string
          type?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "notifications_match_id_fkey"
            columns: ["match_id"]
            isOneToOne: false
            referencedRelation: "matches"
            referencedColumns: ["id"]
          },
        ]
      }
      payments: {
        Row: {
          amount_cents: number
          created_at: string
          id: string
          kind: string
          match_id: string | null
          method: string
          paid_at: string | null
          pix_key: string | null
          pix_payload: string | null
          status: Database["public"]["Enums"]["payment_status"]
          txid: string | null
          user_id: string
        }
        Insert: {
          amount_cents: number
          created_at?: string
          id?: string
          kind?: string
          match_id?: string | null
          method?: string
          paid_at?: string | null
          pix_key?: string | null
          pix_payload?: string | null
          status?: Database["public"]["Enums"]["payment_status"]
          txid?: string | null
          user_id: string
        }
        Update: {
          amount_cents?: number
          created_at?: string
          id?: string
          kind?: string
          match_id?: string | null
          method?: string
          paid_at?: string | null
          pix_key?: string | null
          pix_payload?: string | null
          status?: Database["public"]["Enums"]["payment_status"]
          txid?: string | null
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "payments_match_id_fkey"
            columns: ["match_id"]
            isOneToOne: false
            referencedRelation: "matches"
            referencedColumns: ["id"]
          },
        ]
      }
      profiles: {
        Row: {
          available_until: string | null
          avatar_url: string | null
          city: string | null
          created_at: string
          full_name: string
          id: string
          is_available: boolean
          is_banned: boolean
          lat: number | null
          level: string
          lng: number | null
          matches_played: number
          nickname: string | null
          phone: string | null
          pix_key: string | null
          position: string | null
          radius_km: number
          rating: number
          updated_at: string
        }
        Insert: {
          available_until?: string | null
          avatar_url?: string | null
          city?: string | null
          created_at?: string
          full_name?: string
          id: string
          is_available?: boolean
          is_banned?: boolean
          lat?: number | null
          level?: string
          lng?: number | null
          matches_played?: number
          nickname?: string | null
          phone?: string | null
          pix_key?: string | null
          position?: string | null
          radius_km?: number
          rating?: number
          updated_at?: string
        }
        Update: {
          available_until?: string | null
          avatar_url?: string | null
          city?: string | null
          created_at?: string
          full_name?: string
          id?: string
          is_available?: boolean
          is_banned?: boolean
          lat?: number | null
          level?: string
          lng?: number | null
          matches_played?: number
          nickname?: string | null
          phone?: string | null
          pix_key?: string | null
          position?: string | null
          radius_km?: number
          rating?: number
          updated_at?: string
        }
        Relationships: []
      }
      ratings: {
        Row: {
          comment: string | null
          created_at: string
          id: string
          match_id: string
          rated_id: string
          rater_id: string
          stars: number
        }
        Insert: {
          comment?: string | null
          created_at?: string
          id?: string
          match_id: string
          rated_id: string
          rater_id: string
          stars: number
        }
        Update: {
          comment?: string | null
          created_at?: string
          id?: string
          match_id?: string
          rated_id?: string
          rater_id?: string
          stars?: number
        }
        Relationships: [
          {
            foreignKeyName: "ratings_match_id_fkey"
            columns: ["match_id"]
            isOneToOne: false
            referencedRelation: "matches"
            referencedColumns: ["id"]
          },
        ]
      }
      reports: {
        Row: {
          created_at: string
          details: string | null
          id: string
          match_id: string | null
          reason: string
          reported_user_id: string | null
          reporter_id: string
          status: string
        }
        Insert: {
          created_at?: string
          details?: string | null
          id?: string
          match_id?: string | null
          reason: string
          reported_user_id?: string | null
          reporter_id: string
          status?: string
        }
        Update: {
          created_at?: string
          details?: string | null
          id?: string
          match_id?: string | null
          reason?: string
          reported_user_id?: string | null
          reporter_id?: string
          status?: string
        }
        Relationships: [
          {
            foreignKeyName: "reports_match_id_fkey"
            columns: ["match_id"]
            isOneToOne: false
            referencedRelation: "matches"
            referencedColumns: ["id"]
          },
        ]
      }
      subscriptions: {
        Row: {
          current_period_end: string | null
          plan: Database["public"]["Enums"]["plan_tier"]
          status: Database["public"]["Enums"]["payment_status"]
          updated_at: string
          user_id: string
        }
        Insert: {
          current_period_end?: string | null
          plan?: Database["public"]["Enums"]["plan_tier"]
          status?: Database["public"]["Enums"]["payment_status"]
          updated_at?: string
          user_id: string
        }
        Update: {
          current_period_end?: string | null
          plan?: Database["public"]["Enums"]["plan_tier"]
          status?: Database["public"]["Enums"]["payment_status"]
          updated_at?: string
          user_id?: string
        }
        Relationships: []
      }
      user_roles: {
        Row: {
          created_at: string
          id: string
          role: Database["public"]["Enums"]["app_role"]
          user_id: string
        }
        Insert: {
          created_at?: string
          id?: string
          role?: Database["public"]["Enums"]["app_role"]
          user_id: string
        }
        Update: {
          created_at?: string
          id?: string
          role?: Database["public"]["Enums"]["app_role"]
          user_id?: string
        }
        Relationships: []
      }
    }
    Views: {
      [_ in never]: never
    }
    Functions: {
      distance_km: {
        Args: { lat1: number; lat2: number; lng1: number; lng2: number }
        Returns: number
      }
      has_role: {
        Args: {
          _role: Database["public"]["Enums"]["app_role"]
          _user_id: string
        }
        Returns: boolean
      }
    }
    Enums: {
      app_role: "admin" | "moderator" | "user"
      match_status: "open" | "full" | "cancelled" | "finished"
      participant_status: "confirmed" | "waitlist" | "cancelled"
      payment_status: "pending" | "paid" | "expired" | "refunded"
      plan_tier: "free" | "pro" | "business"
    }
    CompositeTypes: {
      [_ in never]: never
    }
  }
}

type DatabaseWithoutInternals = Omit<Database, "__InternalSupabase">

type DefaultSchema = DatabaseWithoutInternals[Extract<keyof Database, "public">]

export type Tables<
  DefaultSchemaTableNameOrOptions extends
    | keyof (DefaultSchema["Tables"] & DefaultSchema["Views"])
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
        DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
      DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])[TableName] extends {
      Row: infer R
    }
    ? R
    : never
  : DefaultSchemaTableNameOrOptions extends keyof (DefaultSchema["Tables"] &
        DefaultSchema["Views"])
    ? (DefaultSchema["Tables"] &
        DefaultSchema["Views"])[DefaultSchemaTableNameOrOptions] extends {
        Row: infer R
      }
      ? R
      : never
    : never

export type TablesInsert<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Insert: infer I
    }
    ? I
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Insert: infer I
      }
      ? I
      : never
    : never

export type TablesUpdate<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Update: infer U
    }
    ? U
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Update: infer U
      }
      ? U
      : never
    : never

export type Enums<
  DefaultSchemaEnumNameOrOptions extends
    | keyof DefaultSchema["Enums"]
    | { schema: keyof DatabaseWithoutInternals },
  EnumName extends DefaultSchemaEnumNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"]
    : never = never,
> = DefaultSchemaEnumNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"][EnumName]
  : DefaultSchemaEnumNameOrOptions extends keyof DefaultSchema["Enums"]
    ? DefaultSchema["Enums"][DefaultSchemaEnumNameOrOptions]
    : never

export type CompositeTypes<
  PublicCompositeTypeNameOrOptions extends
    | keyof DefaultSchema["CompositeTypes"]
    | { schema: keyof DatabaseWithoutInternals },
  CompositeTypeName extends PublicCompositeTypeNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"]
    : never = never,
> = PublicCompositeTypeNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"][CompositeTypeName]
  : PublicCompositeTypeNameOrOptions extends keyof DefaultSchema["CompositeTypes"]
    ? DefaultSchema["CompositeTypes"][PublicCompositeTypeNameOrOptions]
    : never

export const Constants = {
  public: {
    Enums: {
      app_role: ["admin", "moderator", "user"],
      match_status: ["open", "full", "cancelled", "finished"],
      participant_status: ["confirmed", "waitlist", "cancelled"],
      payment_status: ["pending", "paid", "expired", "refunded"],
      plan_tier: ["free", "pro", "business"],
    },
  },
} as const
