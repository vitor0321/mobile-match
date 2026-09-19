// Server-side mirror of core/geo (GeoHash.kt + Distance.kt) in the KMP client.
//
// Both sides MUST agree: the client writes a geohash and queries by prefix
// ranges, the Functions read those same ranges to find nearby matches and
// players. The shared unit-test vectors in test/unit/geo.test.ts are what keep
// the two implementations from drifting.

const BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
const BITS = [16, 8, 4, 2, 1];
const BITS_PER_CHAR = 5;

export const DEFAULT_GEOHASH_PRECISION = 9;

const EARTH_RADIUS_KM = 6371.0;
const EARTH_MERIDIONAL_CIRCUMFERENCE = 40007.86;
const METERS_PER_DEGREE_LATITUDE = 110574.0;

export type Coordinates = {lat: number; lng: number};

/** A closed range of geohash prefixes that participate in a radius query. */
export type GeoHashRange = {start: string; endInclusive: string};

/**
 * Encodes a coordinate as a base-32 geohash of `precision` characters.
 * Standard GeoFire algorithm; precision 9 ≈ 4.8 m at the equator.
 */
export function encodeGeohash(
  coords: Coordinates,
  precision: number = DEFAULT_GEOHASH_PRECISION,
): string {
  if (precision < 1 || precision > 22) {
    throw new Error("geohash precision must be in 1..22");
  }

  let hash = "";
  let latMin = -90.0;
  let latMax = 90.0;
  let lngMin = -180.0;
  let lngMax = 180.0;
  let even = true;
  let bit = 0;
  let ch = 0;

  while (hash.length < precision) {
    if (even) {
      const mid = (lngMin + lngMax) / 2;
      if (coords.lng >= mid) {
        ch |= BITS[bit];
        lngMin = mid;
      } else {
        lngMax = mid;
      }
    } else {
      const mid = (latMin + latMax) / 2;
      if (coords.lat >= mid) {
        ch |= BITS[bit];
        latMin = mid;
      } else {
        latMax = mid;
      }
    }
    even = !even;
    if (bit < 4) {
      bit++;
    } else {
      hash += BASE32[ch];
      bit = 0;
      ch = 0;
    }
  }

  return hash;
}

/**
 * Great-circle distance in km between two coordinates (haversine).
 * Same Earth radius as the client's `distanceKm` and the legacy Postgres
 * `distance_km`, so all three agree.
 */
export function distanceKm(a: Coordinates, b: Coordinates): number {
  const dLat = toRadians(b.lat - a.lat);
  const dLng = toRadians(b.lng - a.lng);
  const lat1 = toRadians(a.lat);
  const lat2 = toRadians(b.lat);

  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;
  const c = 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
  return EARTH_RADIUS_KM * c;
}

/**
 * Query intervals covering the bounding box of a circle of `radiusKm` around
 * `center`. Issue one Firestore `orderBy(geohash).startAt(a).endAt(b)` per
 * range, then discard the excess with `distanceKm` on the reading side.
 */
export function boundsForRadius(
  center: Coordinates,
  radiusKm: number,
): GeoHashRange[] {
  if (radiusKm <= 0) throw new Error("radius must be positive");

  const precision = precisionForRadius(radiusKm);

  const latDegrees = radiusKm / (METERS_PER_DEGREE_LATITUDE / 1000.0);
  const lngDegrees = kmToLongitudeDegrees(radiusKm, center.lat);

  const latMin = center.lat - latDegrees;
  const latMax = center.lat + latDegrees;
  const lngMin = center.lng - lngDegrees;
  const lngMax = center.lng + lngDegrees;

  const corners: Coordinates[] = [
    {lat: latMin, lng: lngMin},
    {lat: latMin, lng: lngMax},
    {lat: latMax, lng: lngMin},
    {lat: latMax, lng: lngMax},
    {lat: center.lat, lng: lngMin},
    {lat: center.lat, lng: lngMax},
    {lat: latMin, lng: center.lng},
    {lat: latMax, lng: center.lng},
    center,
  ];

  const hashes = new Set(corners.map((corner) => encodeGeohash(corner, precision)));

  // "~" sorts after any base-32 character, so startAt(h).endAt(h+"~") includes
  // every longer geohash that begins with h.
  const ranges = [...hashes]
    .sort()
    .map((hash): GeoHashRange => ({start: hash, endInclusive: hash + "~"}));

  return mergeAdjacent(ranges);
}

function precisionForRadius(radiusKm: number): number {
  let precision = DEFAULT_GEOHASH_PRECISION;
  while (precision > 1) {
    if (latitudeSpanKm(precision) > radiusKm * 2) return precision;
    precision--;
  }
  return 1;
}

function latitudeSpanKm(precision: number): number {
  const latBits = Math.floor((precision * BITS_PER_CHAR + 1) / 2);
  const spanDegrees = 180.0 / (1 << latBits);
  return (spanDegrees / 360.0) * EARTH_MERIDIONAL_CIRCUMFERENCE;
}

function kmToLongitudeDegrees(radiusKm: number, latitude: number): number {
  const radians = ((radiusKm / (METERS_PER_DEGREE_LATITUDE / 1000.0)) * Math.PI) / 180.0;
  const latRad = (latitude * Math.PI) / 180.0;
  const numerator = Math.sin(radians);
  const denom = Math.cos(latRad);
  if (denom === 0.0 || numerator > denom) return 360.0;
  return (Math.asin(numerator / denom) * 180.0) / Math.PI;
}

function mergeAdjacent(ranges: GeoHashRange[]): GeoHashRange[] {
  if (ranges.length <= 1) return ranges;
  const merged: GeoHashRange[] = [];
  let current = ranges[0];
  for (let i = 1; i < ranges.length; i++) {
    const next = ranges[i];
    if (current.endInclusive >= next.start) {
      current = {
        start: current.start,
        endInclusive:
          current.endInclusive >= next.endInclusive ? current.endInclusive : next.endInclusive,
      };
    } else {
      merged.push(current);
      current = next;
    }
  }
  merged.push(current);
  return merged;
}

function toRadians(degrees: number): number {
  return (degrees * Math.PI) / 180.0;
}
