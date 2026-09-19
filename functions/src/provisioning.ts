export const DEFAULT_RADIUS_KM = 15;

export type AuthRecord = {
  displayName?: string | null;
  photoURL?: string | null;
  email?: string | null;
  phoneNumber?: string | null;
};

type Document = Record<string, unknown>;

export function defaultProfile(user: AuthRecord, now: unknown): Document {
  return {
    fullName: user.displayName ?? "",
    nickname: null,
    avatarUrl: user.photoURL ?? null,
    position: null,
    level: "Livre",
    sports: [],
    city: null,
    neighborhood: null,
    rating: 0,
    ratingCount: 0,
    matchesPlayed: 0,
    isBanned: false,
    createdAt: now,
    updatedAt: now,
  };
}

export function defaultPrivateData(user: AuthRecord, now: unknown): Document {
  return {
    email: user.email ?? null,
    phone: user.phoneNumber ?? null,
    pixKey: null,
    lat: null,
    lng: null,
    geohash: null,
    radiusKm: DEFAULT_RADIUS_KM,
    isAvailable: true,
    availableUntil: null,
    availableSports: [],
    updatedAt: now,
  };
}

export function defaultSubscription(now: unknown): Document {
  return {
    plan: "free",
    status: "active",
    currentPeriodEnd: null,
    source: "default",
    updatedAt: now,
  };
}

export function missingFields(existing: Document | undefined, defaults: Document): Document | null {
  if (existing === undefined) return defaults;
  const missing = Object.fromEntries(Object.entries(defaults).filter(([field]) => !(field in existing)));
  return Object.keys(missing).length > 0 ? missing : null;
}

const DEFAULT_CLAIMS: Document = {role: "user", plan: "free"};

export function provisionedClaims(existing: Document | undefined): Document | null {
  const current = existing ?? {};
  const missing = missingFields(current, DEFAULT_CLAIMS);
  return missing === null ? null : {...current, ...missing};
}
