import {describe, expect, it} from "vitest";
import {boundsForRadius, distanceKm, encodeGeohash} from "../../src/geo.js";

// These vectors are shared verbatim with the KMP client tests in
// core/src/commonTest/.../geo/GeoHashTest.kt and DistanceTest.kt. If either
// side changes its algorithm, one of the two suites goes red — which is the
// whole point: client and server must encode and measure identically.

const SAO_PAULO = {lat: -23.5505, lng: -46.6333};
const RIO = {lat: -22.9068, lng: -43.1729};

describe("encodeGeohash — mirrors GeoHash.kt", () => {
  it("encodes São Paulo at precision 9", () => {
    expect(encodeGeohash(SAO_PAULO, 9)).toBe("6gyf4bf8m");
  });

  it("encodes Rio de Janeiro at precision 9", () => {
    expect(encodeGeohash(RIO, 9)).toBe("75cm9tfqn");
  });

  it("encodes at precision 1", () => {
    expect(encodeGeohash(SAO_PAULO, 1)).toBe("6");
  });

  it("honors the requested precision length", () => {
    expect(encodeGeohash(SAO_PAULO, 22)).toHaveLength(22);
  });

  it("rejects out-of-range precision", () => {
    expect(() => encodeGeohash(SAO_PAULO, 0)).toThrow();
    expect(() => encodeGeohash(SAO_PAULO, 23)).toThrow();
  });
});

describe("distanceKm — mirrors Distance.kt (R = 6371)", () => {
  it("measures São Paulo to Rio at ~357 km", () => {
    const distance = distanceKm(SAO_PAULO, RIO);
    expect(distance).toBeGreaterThan(350);
    expect(distance).toBeLessThan(365);
  });

  it("is zero for the same point and symmetric", () => {
    expect(distanceKm(SAO_PAULO, SAO_PAULO)).toBe(0);
    expect(distanceKm(SAO_PAULO, RIO)).toBeCloseTo(distanceKm(RIO, SAO_PAULO), 9);
  });

  it("measures a quarter of the equator to ~10007 km", () => {
    const distance = distanceKm({lat: 0, lng: 0}, {lat: 0, lng: 90});
    expect(distance).toBeGreaterThan(10000);
    expect(distance).toBeLessThan(10020);
  });
});

describe("boundsForRadius — mirrors GeoHash.kt", () => {
  it("always covers the center's own cell", () => {
    const ranges = boundsForRadius(SAO_PAULO, 5);
    expect(ranges.length).toBeGreaterThan(0);

    const centerHash = encodeGeohash(SAO_PAULO);
    const covered = ranges.some(
      (range) => range.start <= centerHash && centerHash <= range.endInclusive,
    );
    expect(covered).toBe(true);
  });

  it("returns sorted, non-overlapping ranges", () => {
    const ranges = boundsForRadius(SAO_PAULO, 50);
    for (let i = 1; i < ranges.length; i++) {
      expect(ranges[i].start > ranges[i - 1].endInclusive).toBe(true);
    }
  });

  it("rejects a non-positive radius", () => {
    expect(() => boundsForRadius(SAO_PAULO, 0)).toThrow();
  });
});
