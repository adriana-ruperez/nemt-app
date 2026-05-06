import * as admin from "firebase-admin";
import {HttpsError, onCall} from "firebase-functions/v2/https";

admin.initializeApp();

type CreateRidePayload = {
  origin?: unknown;
  destination?: unknown;
  scheduledAtMillis?: unknown;
  dateTimeDisplay?: unknown;
  vehicle?: unknown;
};

type RideStopPayload = {
  title?: unknown;
  address?: unknown;
  placeId?: unknown;
  latitude?: unknown;
  longitude?: unknown;
};

type UserProfileDoc = {
  firstName?: string | null;
  lastName?: string | null;
  email?: string | null;
  mobilitySupport?: string | null;
  accessibilityNeeds?: string | null;
};

function asRequiredString(value: unknown, field: string): string {
  if (typeof value != "string" || value.trim() === "") {
    throw new HttpsError("invalid-argument", `${field} is required.`);
  }
  return value.trim();
}

function asOptionalString(value: unknown): string | null {
  if (value == null) return null;
  if (typeof value != "string") {
    throw new HttpsError("invalid-argument", "Invalid string field.");
  }
  const trimmed = value.trim();
  return trimmed === "" ? null : trimmed;
}

function asRequiredNumber(value: unknown, field: string): number {
  if (typeof value != "number" || !Number.isFinite(value)) {
    throw new HttpsError("invalid-argument", `${field} must be a valid number.`);
  }
  return value;
}

function asRideStop(value: unknown, field: string) {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    throw new HttpsError("invalid-argument", `${field} must be an object.`);
  }
  const stop = value as RideStopPayload;
  const title = asRequiredString(stop.title, `${field}.title`);
  const address = asOptionalString(stop.address) ?? title;
  const placeId = asOptionalString(stop.placeId);
  const latitude = asRequiredNumber(stop.latitude, `${field}.latitude`);
  const longitude = asRequiredNumber(stop.longitude, `${field}.longitude`);
  return {
    title,
    address,
    placeId,
    location: {
      latitude,
      longitude,
    },
  };
}

function displayName(profile: UserProfileDoc, fallbackEmail: string): string {
  const first = profile.firstName?.trim() ?? "";
  const last = profile.lastName?.trim() ?? "";
  const combined = [first, last].filter(Boolean).join(" ").trim();
  if (combined) return combined;
  const localPart = fallbackEmail.split("@")[0]?.trim() ?? "";
  if (!localPart) return "Account";
  return localPart.charAt(0).toUpperCase() + localPart.slice(1);
}

export const createRide = onCall({region: "us-central1", cors: true}, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Authentication is required.");
  }

  const data = (request.data ?? {}) as CreateRidePayload;
  const origin = asRideStop(data.origin, "origin");
  const destination = asRideStop(data.destination, "destination");
  const scheduledAtMillis = asRequiredNumber(data.scheduledAtMillis, "scheduledAtMillis");
  const dateTimeDisplay = asRequiredString(data.dateTimeDisplay, "dateTimeDisplay");
  const vehicle = asRequiredString(data.vehicle, "vehicle");

  if (scheduledAtMillis < Date.now() - 60_000) {
    throw new HttpsError("invalid-argument", "scheduledAtMillis must be in the future.");
  }

  const firestore = admin.firestore();
  const userSnapshot = await firestore.collection("users").doc(uid).get();
  const profile = (userSnapshot.data() ?? {}) as UserProfileDoc;
  const authUser = await admin.auth().getUser(uid);
  const riderEmail = profile.email ?? authUser.email ?? null;
  const riderDisplayName = displayName(profile, riderEmail ?? "");

  const rideRef = firestore.collection("rides").doc();
  await rideRef.set({
    id: rideRef.id,
    riderUid: uid,
    lifecycleStatus: "ACCEPTED",
    scheduledAtMillis,
    route: {
      origin,
      destination,
    },
    schedule: {
      scheduledAtMillis,
      dateTimeDisplay,
    },
    vehicle: {
      label: vehicle,
    },
    rider: {
      uid,
      email: riderEmail,
      displayName: riderDisplayName,
    },
    timestamps: {
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    },
  });

  return {rideId: rideRef.id};
});
