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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read clipboard items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("clipboard_items").get());
});

test("Bob cannot read Alice's clipboard items", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("clipboard_items").get());
});

test("Alice can create and read her own clipboard items", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const clipRef = aliceDb.collection("users").doc(ALICE_UID).collection("clipboard_items").doc("clip_1");
  await assertSucceeds(
    clipRef.set({
      userId: ALICE_UID,
      originDeviceId: "device_alice_phone",
      title: "Hello World",
      content: "Hello World sync content",
      isPinned: false,
      isImage: false,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("clipboard_items").get());
});

test("Alice cannot create clipboard item with mismatched userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const clipRef = aliceDb.collection("users").doc(ALICE_UID).collection("clipboard_items").doc("clip_bad");
  await assertFails(
    clipRef.set({
      userId: BOB_UID, // Mismatched UID
      originDeviceId: "device_alice_phone",
      title: "Hello Spoof",
      content: "Spoofed content",
      isImage: false,
      createdAt: new Date(),
    })
  );
});
