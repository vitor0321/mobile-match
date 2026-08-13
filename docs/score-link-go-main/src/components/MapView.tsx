import { useEffect, useRef } from "react";
import type * as LeafletNS from "leaflet";
import type { Circle, Map as LeafletMap, Marker } from "leaflet";

type LeafletModule = typeof LeafletNS;

export type MapPin = {
  id: string;
  lat: number;
  lng: number;
  label: string;
  sub: string;
  full: boolean;
};

export function MapView({
  center,
  pins,
  activeId,
  radiusKm,
  onSelect,
  className = "",
}: {
  center: { lat: number; lng: number };
  pins: MapPin[];
  activeId?: string | null;
  radiusKm?: number;
  onSelect?: (id: string) => void;
  className?: string;
}) {
  const nodeRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<LeafletMap | null>(null);
  const markersRef = useRef<Marker[]>([]);
  const meRef = useRef<Marker | null>(null);
  const circleRef = useRef<Circle | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const L = (await import("leaflet")).default as LeafletModule;
      await import("leaflet/dist/leaflet.css");
      if (cancelled || !nodeRef.current || mapRef.current) return;

      const map = L.map(nodeRef.current, {
        center: [center.lat, center.lng],
        zoom: 14,
        zoomControl: false,
        attributionControl: false,
      });
      L.tileLayer("https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png", {
        maxZoom: 19,
      }).addTo(map);
      mapRef.current = map;
      renderPins(L, map);
    })();
    return () => {
      cancelled = true;
      mapRef.current?.remove();
      mapRef.current = null;
      markersRef.current = [];
      meRef.current = null;
      circleRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function renderPins(Lmod?: LeafletModule, map?: LeafletMap) {
    const L = Lmod ?? ((await import("leaflet")).default as LeafletModule);
    const m = map ?? mapRef.current;
    if (!m) return;

    markersRef.current.forEach((mk) => mk.remove());
    markersRef.current = [];

    if (circleRef.current) {
      circleRef.current.remove();
      circleRef.current = null;
    }
    if (radiusKm && radiusKm > 0) {
      circleRef.current = L.circle([center.lat, center.lng], {
        radius: radiusKm * 1000,
        color: "#a3e635",
        weight: 1,
        opacity: 0.55,
        fillColor: "#a3e635",
        fillOpacity: 0.05,
        dashArray: "6 8",
      }).addTo(m);
    }

    if (meRef.current) meRef.current.remove();
    meRef.current = L.marker([center.lat, center.lng], {
      icon: L.divIcon({
        className: "",
        html: `<span style="position:relative;display:block;width:18px;height:18px">
          <span style="position:absolute;inset:0;border-radius:9999px;background:#38bdf8;opacity:.35;animation:radar-ping 2.4s ease-out infinite"></span>
          <span style="position:absolute;inset:3px;border-radius:9999px;background:#38bdf8;border:2px solid rgba(255,255,255,.85);box-shadow:0 4px 10px rgba(0,0,0,.6)"></span>
        </span>`,
        iconSize: [18, 18],
        iconAnchor: [9, 9],
      }),
    }).addTo(m);

    pins.forEach((p) => {
      const active = p.id === activeId;
      const open = !p.full;
      const bg = open
        ? "linear-gradient(145deg,#c9f36a,#8ddc2e)"
        : "linear-gradient(145deg,#6b7280,#4b5563)";
      const ink = open ? "#0b1207" : "#e5e7eb";
      const marker = L.marker([p.lat, p.lng], {
        icon: L.divIcon({
          className: "",
          html: `<div style="position:relative;transform:translate(-50%,-100%);white-space:nowrap;">
            ${
              open
                ? `<span style="position:absolute;left:50%;bottom:-6px;width:34px;height:34px;margin-left:-17px;border-radius:9999px;background:#a3e635;opacity:.35;animation:radar-ping 2s ease-out infinite"></span>`
                : ""
            }
            <div style="position:relative;background:${bg};color:${ink};font-weight:900;font-size:11px;letter-spacing:-.01em;padding:5px 10px;border-radius:10px;
              box-shadow:inset 0 1px 0 rgba(255,255,255,.55), inset 0 -2px 0 rgba(0,0,0,.28), 0 8px 18px rgba(0,0,0,.55);
              border:${active ? "2px solid #ffffff" : "1px solid rgba(0,0,0,.35)"};
              transform:${active ? "translateY(-3px) scale(1.06)" : "none"};transition:transform .18s ease">${p.label}</div>
            <div style="position:relative;width:3px;height:12px;margin:0 auto;background:linear-gradient(${ink === "#0b1207" ? "#8ddc2e" : "#4b5563"},rgba(0,0,0,.2))"></div>
            <div style="width:12px;height:4px;margin:0 auto;border-radius:9999px;background:rgba(0,0,0,.5);filter:blur(1px)"></div>
          </div>`,
          iconSize: [0, 0],
        }),
        zIndexOffset: active ? 1000 : open ? 500 : 0,
      }).addTo(m);
      marker.on("click", () => onSelect?.(p.id));
      markersRef.current.push(marker);
    });
  }

  useEffect(() => {
    void renderPins();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pins, activeId, center.lat, center.lng, radiusKm]);

  useEffect(() => {
    mapRef.current?.setView([center.lat, center.lng]);
  }, [center.lat, center.lng]);

  useEffect(() => {
    if (!activeId) return;
    const pin = pins.find((p) => p.id === activeId);
    if (pin) mapRef.current?.panTo([pin.lat, pin.lng]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeId]);

  return (
    <div className="relative">
      <div ref={nodeRef} className={className} />
      <div className="pointer-events-none absolute inset-x-0 top-0 h-16 bg-gradient-to-b from-background/70 to-transparent" />
      <div className="pointer-events-none absolute inset-x-0 bottom-0 h-10 bg-gradient-to-t from-background/80 to-transparent" />
    </div>
  );
}
