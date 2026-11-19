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
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTime(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("groups").where("memberUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("inviteCodes").doc("CREW7X").get());
  await assertFails(
    unauthDb.collection("groups").doc("grp_1").set({
      name: "Hackers",
      inviteCode: "HACK99",
      creatorUid: ALICE_UID,
      memberUids: [ALICE_UID],
      createdAt: pastTime(),
      updatedAt: pastTime(),
    })
  );
});

test("UserProfile: owner can create/update/read; non-owner blocked; ghost fields rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTime(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      birthdayMonth: 5,
      birthdayDay: 14,
      avatarIndex: 1,
      activeGroupId: "grp_1",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Partial update preserving createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Wonder",
      updatedAt: pastTime(1000),
    })
  );

  // Bob cannot read or modify Alice's profile (PII isolation)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked" }));

  // Ghost field & future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastTime(1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Future",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Groups & InviteCodes: atomic batch creation, code lookup, join, and query isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastTime(10000);

  // Atomic batch create group + inviteCode
  const batch = aliceDb.batch();
  const groupRef = aliceDb.collection("groups").doc("grp_1");
  const codeRef = aliceDb.collection("inviteCodes").doc("CREW7X");
  batch.set(groupRef, {
    name: "Weekend Crew",
    inviteCode: "CREW7X",
    creatorUid: ALICE_UID,
    memberUids: [ALICE_UID],
    createdAt: ts,
    updatedAt: ts,
  });
  batch.set(codeRef, {
    groupId: "grp_1",
    groupName: "Weekend Crew",
    creatorUid: ALICE_UID,
    createdAt: ts,
  });
  await assertSucceeds(batch.commit());

  // Listing inviteCodes is strictly forbidden
  await assertFails(bobDb.collection("inviteCodes").get());

  // Bob can get a specific inviteCode if he knows it
  await assertSucceeds(bobDb.collection("inviteCodes").doc("CREW7X").get());

  // Bob cannot read grp_1 before joining, and unfiltered groups query fails
  await assertFails(bobDb.collection("groups").doc("grp_1").get());
  await assertFails(bobDb.collection("groups").get());

  // Bob joins grp_1 by adding ONLY himself to memberUids
  await assertSucceeds(
    bobDb.collection("groups").doc("grp_1").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: pastTime(2000),
    })
  );

  // Now Bob's filtered query succeeds, while Charlie (outsider) cannot read grp_1
  await assertSucceeds(bobDb.collection("groups").where("memberUids", "array-contains", BOB_UID).get());
  await assertFails(charlieDb.collection("groups").doc("grp_1").get());
});

test("Group subcollections: members, birthday wishes, statuses, and memories follow strict rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastTime(10000);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("groups").doc("grp_1").set({
      name: "Weekend Crew",
      inviteCode: "CREW7X",
      creatorUid: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    });
  });

  // 1. GroupMember create + update birthday/status
  await assertSucceeds(
    aliceDb.collection("groups").doc("grp_1").collection("members").doc(ALICE_UID).set({
      userId: ALICE_UID,
      groupId: "grp_1",
      memberUids: [ALICE_UID, BOB_UID],
      displayName: "Alice",
      avatarIndex: 0,
      birthdayMonth: 11,
      birthdayDay: 20,
      statusCategory: "HOSPITAL",
      statusText: "At City Hospital for checkup",
      statusPlace: "City General Hospital",
      availability: "NEED_SUPPORT",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot overwrite Alice's member status
  await assertFails(
    bobDb.collection("groups").doc("grp_1").collection("members").doc(ALICE_UID).update({
      statusText: "Spoofed",
      updatedAt: pastTime(1000),
    })
  );

  // Filtered member list succeeds for Bob; unfiltered, cross-user, and direct get fail for Charlie
  await assertSucceeds(
    bobDb.collection("groups").doc("grp_1").collection("members").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertFails(
    charlieDb.collection("groups").doc("grp_1").collection("members").get()
  );
  await assertFails(
    charlieDb.collection("groups").doc("grp_1").collection("members").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(
    charlieDb.collection("groups").doc("grp_1").collection("members").doc(ALICE_UID).get()
  );

  // 2. BirthdayWish create + unfiltered list denial
  await assertSucceeds(
    bobDb.collection("groups").doc("grp_1").collection("wishes").doc("wish_1").set({
      groupId: "grp_1",
      targetUserId: ALICE_UID,
      targetName: "Alice",
      senderUserId: BOB_UID,
      senderName: "Bob",
      message: "Happy early birthday Alice!!",
      giftEmoji: "🎂",
      year: 2026,
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(aliceDb.collection("groups").doc("grp_1").collection("wishes").get());
  await assertSucceeds(
    aliceDb.collection("groups").doc("grp_1").collection("wishes").where("memberUids", "array-contains", ALICE_UID).get()
  );

  // 3. StatusPost create + support toggle
  await assertSucceeds(
    aliceDb.collection("groups").doc("grp_1").collection("statuses").doc("st_1").set({
      groupId: "grp_1",
      userId: ALICE_UID,
      authorName: "Alice",
      avatarIndex: 0,
      category: "WORK",
      message: "Starting my shift at the clinic!",
      placeName: "Metro Clinic",
      availability: "BUSY",
      supportUids: [],
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("groups").doc("grp_1").collection("statuses").doc("st_1").update({
      supportUids: [BOB_UID],
      updatedAt: pastTime(1000),
    })
  );

  // 4. Memory create + like toggle + @DocumentId 'id' forbidden
  await assertSucceeds(
    aliceDb.collection("groups").doc("grp_1").collection("memories").doc("mem_1").set({
      groupId: "grp_1",
      userId: ALICE_UID,
      authorName: "Alice",
      mediaType: "PHOTO",
      caption: "Sunset coffee with the crew",
      tag: "Hangout",
      mediaBase64: "AAAA",
      thumbnailBase64: "",
      externalUrl: "",
      likedByUids: [],
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("groups").doc("grp_1").collection("memories").doc("mem_1").update({
      likedByUids: [BOB_UID],
      updatedAt: pastTime(1000),
    })
  );
  await assertFails(
    aliceDb.collection("groups").doc("grp_1").collection("memories").doc("mem_bad").set({
      id: "mem_bad",
      groupId: "grp_1",
      userId: ALICE_UID,
      authorName: "Alice",
      mediaType: "PHOTO",
      caption: "Should fail",
      tag: "",
      mediaBase64: "AAAA",
      thumbnailBase64: "",
      externalUrl: "",
      likedByUids: [],
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("GameRoom: state transitions, real-time move, and terminal FINISHED lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTime(10000);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("groups").doc("grp_1").set({
      name: "Weekend Crew",
      inviteCode: "CREW7X",
      creatorUid: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    });
  });

  // Host creates WAITING Tic-Tac-Toe room
  await assertSucceeds(
    aliceDb.collection("groups").doc("grp_1").collection("games").doc("game_1").set({
      groupId: "grp_1",
      gameType: "TIC_TAC_TOE",
      status: "WAITING",
      hostUid: ALICE_UID,
      hostName: "Alice",
      guestUid: "",
      guestName: "",
      currentTurnUid: ALICE_UID,
      winnerUid: "",
      board: ["", "", "", "", "", "", "", "", ""],
      rpsRound: 1,
      rpsHostMove: "",
      rpsGuestMove: "",
      rpsHostScore: 0,
      rpsGuestScore: 0,
      rpsSummary: "",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob joins room
  await assertSucceeds(
    bobDb.collection("groups").doc("grp_1").collection("games").doc("game_1").update({
      guestUid: BOB_UID,
      guestName: "Bob",
      status: "PLAYING",
      currentTurnUid: ALICE_UID,
      updatedAt: pastTime(3000),
    })
  );

  // Alice makes winning move and marks FINISHED
  await assertSucceeds(
    aliceDb.collection("groups").doc("grp_1").collection("games").doc("game_1").update({
      board: ["X", "X", "X", "O", "O", "", "", "", ""],
      currentTurnUid: "",
      status: "FINISHED",
      winnerUid: ALICE_UID,
      updatedAt: pastTime(2000),
    })
  );

  // Terminal State Lock: no further moves allowed once FINISHED
  await assertFails(
    bobDb.collection("groups").doc("grp_1").collection("games").doc("game_1").update({
      board: ["X", "X", "X", "O", "O", "O", "", "", ""],
      currentTurnUid: "",
      status: "FINISHED",
      winnerUid: BOB_UID,
      updatedAt: pastTime(1000),
    })
  );
});
