import { Link } from "@tanstack/react-router";
import { MapPin, Clock, Star } from "lucide-react";
import { formatBRL, slotsLeft, sportOf, type Match } from "@/lib/mock-data";

export function MatchCard({ match, showScore = false }: { match: Match; showScore?: boolean }) {
  const sport = sportOf(match.sport);
  const left = slotsLeft(match);
  const full = left <= 0;

  return (
    <Link
      to="/partida/$id"
      params={{ id: match.id }}
      className="block rounded-3xl border border-border/70 bg-card p-4 shadow-lift transition-transform active:scale-[0.985]"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
            {sport.emoji} {sport.label} · {match.level}
          </p>
          <h3 className="mt-1 truncate text-lg font-bold">{match.venue}</h3>
          <p className="mt-0.5 flex items-center gap-1 text-xs text-muted-foreground">
            <MapPin className="size-3.5" /> {match.neighborhood} · {match.distanceKm} km
          </p>
        </div>
        <span
          className={`shrink-0 rounded-full px-3 py-1 text-xs font-bold ${
            full ? "bg-muted text-muted-foreground" : "bg-success/15 text-success"
          }`}
        >
          {full ? "Completo" : `${left} vaga${left > 1 ? "s" : ""}`}
        </span>
      </div>

      <div className="mt-4 flex items-center justify-between gap-2 text-sm">
        <span className="flex items-center gap-1.5 font-semibold">
          <Clock className="size-4 text-primary" /> {match.day} · {match.time}
        </span>
        <span className="font-bold text-primary">{formatBRL(match.price)}</span>
      </div>

      <div className="mt-3 flex items-center justify-between border-t border-border/60 pt-3 text-xs text-muted-foreground">
        <span className="flex items-center gap-1">
          <Star className="size-3.5 fill-warning text-warning" />
          {match.organizer.rating.toFixed(1)} · {match.organizer.name}
        </span>
        <span className="font-semibold text-foreground">
          {match.filledSlots}/{match.totalSlots}
        </span>
      </div>

      {showScore && (
        <div className="mt-3 flex items-center gap-2">
          <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-secondary">
            <div className="h-full action-gradient" style={{ width: `${match.matchScore}%` }} />
          </div>
          <span className="text-xs font-bold text-primary">match {match.matchScore}%</span>
        </div>
      )}
    </Link>
  );
}
