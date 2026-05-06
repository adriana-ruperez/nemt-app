import * as admin from "firebase-admin";
import {HttpsError, onCall, onRequest} from "firebase-functions/v2/https";

admin.initializeApp();

const DRIVER_POOL = [
  {id: "driver_mason", name: "Mason Reed"},
  {id: "driver_olivia", name: "Olivia Brooks"},
  {id: "driver_elias", name: "Elias Turner"},
  {id: "driver_nora", name: "Nora Bennett"},
  {id: "driver_isaac", name: "Isaac Foster"},
];

type CreateRidePayload = {
  origin?: unknown;
  destination?: unknown;
  scheduledAtMillis?: unknown;
  dateTimeDisplay?: unknown;
  vehicle?: unknown;
};

type CancelRidePayload = {
  rideId?: unknown;
};

type SendMessagePayload = {
  conversationId?: unknown;
  text?: unknown;
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

function asRequiredRideId(value: unknown): string {
  if (typeof value !== "string" || value.trim() === "") {
    throw new HttpsError("invalid-argument", "rideId is required.");
  }
  return value.trim();
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

function asConversationId(value: unknown): string {
  if (typeof value !== "string" || value.trim() === "") {
    throw new HttpsError("invalid-argument", "conversationId is required.");
  }
  return value.trim();
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

function randomDriver() {
  return DRIVER_POOL[Math.floor(Math.random() * DRIVER_POOL.length)];
}

async function upsertConversation(
  firestore: admin.firestore.Firestore,
  params: {
    conversationId: string;
    riderUid: string;
    type: "support" | "driver";
    counterpartId: string;
    counterpartName: string;
    counterpartRole: "support" | "driver";
    title: string;
    subtitle: string;
    messageText: string;
    metadata?: Record<string, unknown>;
  }
) {
  const ref = firestore.collection("conversations").doc(params.conversationId);
  const snapshot = await ref.get();
  const now = admin.firestore.FieldValue.serverTimestamp();
  const baseData = {
    id: params.conversationId,
    riderUid: params.riderUid,
    type: params.type,
    participantIds: [params.riderUid, params.counterpartId],
    counterpartId: params.counterpartId,
    counterpartName: params.counterpartName,
    counterpartRole: params.counterpartRole,
    title: params.title,
    subtitle: params.subtitle,
    lastMessageText: params.messageText,
    lastMessageAt: now,
    updatedAt: now,
    ...params.metadata,
  };

  if (snapshot.exists) {
    await ref.set(baseData, {merge: true});
  } else {
    await ref.set({
      ...baseData,
      createdAt: now,
    });
  }

  await ref.collection("messages").add({
    conversationId: params.conversationId,
    senderRole: "system",
    senderId: params.counterpartId,
    senderName: params.counterpartName,
    text: params.messageText,
    createdAt: now,
  });

  return params.conversationId;
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
  const driver = randomDriver();

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
    driver: {
      id: driver.id,
      name: driver.name,
    },
    timestamps: {
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    },
  });

  const supportConversationId = await upsertConversation(firestore, {
    conversationId: `support_${uid}`,
    riderUid: uid,
    type: "support",
    counterpartId: "support_admin",
    counterpartName: "NEMT Support",
    counterpartRole: "support",
    title: "Support",
    subtitle: "Questions about your rides",
    messageText: `Your ride has been reserved successfully for ${dateTimeDisplay}. If you have any questions, you can ask in this support chat.`,
  });

  const driverConversationId = await upsertConversation(firestore, {
    conversationId: `driver_${uid}_${driver.id}`,
    riderUid: uid,
    type: "driver",
    counterpartId: driver.id,
    counterpartName: driver.name,
    counterpartRole: "driver",
    title: driver.name,
    subtitle: "Assigned driver",
    messageText: `Your driver has been confirmed. ${driver.name} will handle your trip on ${dateTimeDisplay}. If you want to get in touch, you can use this chat.`,
    metadata: {
      driverId: driver.id,
      driverName: driver.name,
    },
  });

  return {
    rideId: rideRef.id,
    driverName: driver.name,
    driverConversationId,
    supportConversationId,
  };
});

export const cancelRide = onCall({region: "us-central1", cors: true}, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Authentication is required.");
  }

  const data = (request.data ?? {}) as CancelRidePayload;
  const rideId = asRequiredRideId(data.rideId);
  const rideRef = admin.firestore().collection("rides").doc(rideId);
  const snapshot = await rideRef.get();

  if (!snapshot.exists) {
    throw new HttpsError("not-found", "Ride not found.");
  }

  const ride = snapshot.data() as {riderUid?: string; lifecycleStatus?: string} | undefined;
  if (ride?.riderUid !== uid) {
    throw new HttpsError("permission-denied", "You cannot cancel this ride.");
  }

  if (ride.lifecycleStatus === "COMPLETED") {
    throw new HttpsError("failed-precondition", "Completed rides cannot be cancelled.");
  }

  if (ride.lifecycleStatus === "CANCELLED") {
    return {rideId, lifecycleStatus: "CANCELLED"};
  }

  await rideRef.update({
    lifecycleStatus: "CANCELLED",
    "timestamps.updatedAt": admin.firestore.FieldValue.serverTimestamp(),
  });

  return {rideId, lifecycleStatus: "CANCELLED"};
});

export const cancelRideHttp = onRequest({region: "us-central1", cors: true}, async (request, response) => {
  if (request.method !== "POST") {
    response.status(405).json({error: "method-not-allowed"});
    return;
  }

  const authHeader = request.header("Authorization") ?? "";
  const tokenMatch = authHeader.match(/^Bearer (.+)$/i);
  if (!tokenMatch) {
    response.status(401).json({error: "missing-auth-token"});
    return;
  }

  let decodedToken;
  try {
    decodedToken = await admin.auth().verifyIdToken(tokenMatch[1]);
  } catch {
    response.status(401).json({error: "invalid-auth-token"});
    return;
  }

  const rideId = asRequiredRideId(request.body?.rideId);
  const uid = decodedToken.uid;
  const rideRef = admin.firestore().collection("rides").doc(rideId);
  const snapshot = await rideRef.get();

  if (!snapshot.exists) {
    response.status(404).json({error: "ride-not-found"});
    return;
  }

  const ride = snapshot.data() as {riderUid?: string; lifecycleStatus?: string} | undefined;
  if (ride?.riderUid !== uid) {
    response.status(403).json({error: "permission-denied"});
    return;
  }

  if (ride.lifecycleStatus === "COMPLETED") {
    response.status(409).json({error: "completed-rides-cannot-be-cancelled"});
    return;
  }

  if (ride.lifecycleStatus !== "CANCELLED") {
    await rideRef.update({
      lifecycleStatus: "CANCELLED",
      "timestamps.updatedAt": admin.firestore.FieldValue.serverTimestamp(),
    });
  }

  response.status(200).json({rideId, lifecycleStatus: "CANCELLED"});
});

export const sendMessageHttp = onRequest({region: "us-central1", cors: true}, async (request, response) => {
  if (request.method !== "POST") {
    response.status(405).json({error: "method-not-allowed"});
    return;
  }

  const authHeader = request.header("Authorization") ?? "";
  const tokenMatch = authHeader.match(/^Bearer (.+)$/i);
  if (!tokenMatch) {
    response.status(401).json({error: "missing-auth-token"});
    return;
  }

  let decodedToken;
  try {
    decodedToken = await admin.auth().verifyIdToken(tokenMatch[1]);
  } catch {
    response.status(401).json({error: "invalid-auth-token"});
    return;
  }

  const payload = (request.body ?? {}) as SendMessagePayload;
  const conversationId = asConversationId(payload.conversationId);
  const text = asRequiredString(payload.text, "text");
  const uid = decodedToken.uid;
  const firestore = admin.firestore();
  const conversationRef = firestore.collection("conversations").doc(conversationId);
  const conversationSnapshot = await conversationRef.get();

  if (!conversationSnapshot.exists) {
    response.status(404).json({error: "conversation-not-found"});
    return;
  }

  const conversation = conversationSnapshot.data() as {
    participantIds?: string[];
  } | undefined;
  const participantIds = conversation?.participantIds ?? [];
  if (!participantIds.includes(uid)) {
    response.status(403).json({error: "permission-denied"});
    return;
  }

  const authUser = await admin.auth().getUser(uid);
  const senderName = authUser.displayName?.trim() || authUser.email?.split("@")[0] || "Passenger";
  const now = admin.firestore.FieldValue.serverTimestamp();

  await conversationRef.collection("messages").add({
    conversationId,
    senderRole: "rider",
    senderId: uid,
    senderName,
    text,
    createdAt: now,
  });

  await conversationRef.set({
    lastMessageText: text,
    lastMessageAt: now,
    updatedAt: now,
  }, {merge: true});

  response.status(200).json({conversationId, text});
});
