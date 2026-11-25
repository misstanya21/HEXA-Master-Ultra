const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validCloudSavePayload(uid, nowDate = new Date(Date.now() - 5000)) {
  return {
    userId: uid,
    displayName: "Hexa Hero",
    currentLevel: 4,
    highestLevel: 4,
    coins: 650,
    gems: 45,
    totalScore: 3200,
    bestTimeAttackScore: 1200,
    bestEndlessScore: 1850,
    totalLevelsCompleted: 3,
    totalMerges: 48,
    totalStacksCleared: 14,
    bestComboEver: 5,
    totalObstaclesCleared: 9,
    consecutiveDays: 2,
    unlockedTrophies: 3,
    hasNoAds: false,
    isColorblindMode: false,
    createdAt: nowDate,
    updatedAt: nowDate,
  };
}

function validLeaderboardPayload(uid, nowDate = new Date(Date.now() - 5000)) {
  return {
    userId: uid,
    playerName: "Hexa Hero",
    rankTitle: "Puzzle Master",
    rankEmoji: "🔥",
    totalScore: 3200,
    highestLevel: 4,
    bestTimeAttackScore: 1200,
    bestEndlessScore: 1850,
    unlockedTrophies: 3,
    createdAt: nowDate,
    updatedAt: nowDate,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write cloud saves or leaderboard", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validCloudSavePayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("leaderboard").where("totalScore", ">=", 0).get()
  );
  await assertFails(
    unauthDb
      .collection("leaderboard")
      .doc(ALICE_UID)
      .set(validLeaderboardPayload(ALICE_UID))
  );
});

test("Cloud save: owner can create, read, and update own progress", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const payload = validCloudSavePayload(ALICE_UID);
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(payload));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("users").where("userId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 900,
      totalScore: 4500,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Cloud save: cross-user access and unfiltered list fail", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set(validCloudSavePayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set(validCloudSavePayload(BOB_UID))
  );
  await assertFails(aliceDb.collection("users").get());
});

test("Cloud save: rejects shadow ghost fields and immutable mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const payload = validCloudSavePayload(ALICE_UID);
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(payload));

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isVerified: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: -50,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Leaderboard: owner can publish/update own entry and cannot spoof another player", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("leaderboard")
      .doc(ALICE_UID)
      .set(validLeaderboardPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).update({
      totalScore: 5000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb
      .collection("leaderboard")
      .doc(BOB_UID)
      .set(validLeaderboardPayload(BOB_UID))
  );
  await assertFails(
    aliceDb.collection("leaderboard").doc(ALICE_UID).update({
      totalScore: -10,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("leaderboard").doc(ALICE_UID).update({
      adminOverride: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Leaderboard: aligned category queries succeed and unfiltered list fails", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("leaderboard")
      .doc(ALICE_UID)
      .set(validLeaderboardPayload(ALICE_UID));
    await ctx
      .firestore()
      .collection("leaderboard")
      .doc(BOB_UID)
      .set(validLeaderboardPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("leaderboard")
      .where("totalScore", ">=", 0)
      .orderBy("totalScore", "desc")
      .limit(50)
      .get()
  );
  await assertSucceeds(
    aliceDb
      .collection("leaderboard")
      .where("bestTimeAttackScore", ">=", 0)
      .orderBy("bestTimeAttackScore", "desc")
      .limit(50)
      .get()
  );
  await assertSucceeds(
    aliceDb
      .collection("leaderboard")
      .where("bestEndlessScore", ">=", 0)
      .orderBy("bestEndlessScore", "desc")
      .limit(50)
      .get()
  );
  await assertSucceeds(
    aliceDb
      .collection("leaderboard")
      .where("highestLevel", ">=", 1)
      .orderBy("highestLevel", "desc")
      .limit(50)
      .get()
  );
  await assertFails(aliceDb.collection("leaderboard").get());
});
