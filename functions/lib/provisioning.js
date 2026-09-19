"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DEFAULT_RADIUS_KM = void 0;
exports.defaultProfile = defaultProfile;
exports.defaultPrivateData = defaultPrivateData;
exports.defaultSubscription = defaultSubscription;
exports.missingFields = missingFields;
exports.provisionedClaims = provisionedClaims;
exports.DEFAULT_RADIUS_KM = 15;
function defaultProfile(user, now) {
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
function defaultPrivateData(user, now) {
    return {
        email: user.email ?? null,
        phone: user.phoneNumber ?? null,
        pixKey: null,
        lat: null,
        lng: null,
        geohash: null,
        radiusKm: exports.DEFAULT_RADIUS_KM,
        isAvailable: true,
        availableUntil: null,
        availableSports: [],
        updatedAt: now,
    };
}
function defaultSubscription(now) {
    return {
        plan: "free",
        status: "active",
        currentPeriodEnd: null,
        source: "default",
        updatedAt: now,
    };
}
function missingFields(existing, defaults) {
    if (existing === undefined)
        return defaults;
    const missing = Object.fromEntries(Object.entries(defaults).filter(([field]) => !(field in existing)));
    return Object.keys(missing).length > 0 ? missing : null;
}
const DEFAULT_CLAIMS = { role: "user", plan: "free" };
function provisionedClaims(existing) {
    const current = existing ?? {};
    const missing = missingFields(current, DEFAULT_CLAIMS);
    return missing === null ? null : { ...current, ...missing };
}
