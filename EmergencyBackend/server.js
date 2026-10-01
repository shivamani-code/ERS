const admin = require("firebase-admin");

const serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

console.log("Backend Server Started...");
console.log("Attaching listener to 'incoming_calls' collection...");

db.collection("incoming_calls").onSnapshot((snapshot) => {
    snapshot.docChanges().forEach(async (change) => {
        if (change.type === "added") {
            const data = change.doc.data();
            const callerNumber = data.caller_number;
            const docId = change.doc.id;

            console.log(`[${new Date().toISOString()}] New incoming_calls document detected: ${docId}`);
            console.log(`Caller number received: ${callerNumber}`);

            if (!callerNumber) {
                console.warn("Document added but caller_number is missing.");
                return;
            }

            // Search matching user
            console.log(`Searching for user with phone: ${callerNumber}`);
            try {
                const userSnapshot = await db.collection("users")
                    .where("phone", "==", callerNumber)
                    .get();

                // No user found
                if (userSnapshot.empty) {
                    console.log(`No matching user found for phone: ${callerNumber}`);
                    return;
                }

                console.log(`Matching user found: ${userSnapshot.docs.length} user(s)`);

                userSnapshot.forEach(async (userDoc) => {
                    const userData = userDoc.data();
                    const deviceToken = userData.device_token;

                    console.log(`User ID: ${userDoc.id}, Device Token: ${deviceToken || "MISSING"}`);

                    if (!deviceToken) {
                        console.error(`User ${userDoc.id} has no device_token. Cannot create trigger.`);
                        return;
                    }

                    // Create trigger document
                    console.log(`Creating trigger for device: ${deviceToken}...`);
                    try {
                        const triggerRef = await db.collection("app_triggers").add({
                            device_token: deviceToken,
                            phone: callerNumber,
                            action: "SEND_LOCATION",
                            status: "pending",
                            timestamp: admin.firestore.FieldValue.serverTimestamp()
                        });
                        console.log(`Trigger Creation SUCCESS! Document ID: ${triggerRef.id}`);
                    } catch (err) {
                        console.error(`Trigger Creation FAILED: ${err.message}`);
                    }
                });
            } catch (err) {
                console.error(`User lookup failed: ${err.message}`);
            }
        }
    });
}, (err) => {
    console.error("Snapshot listener failed:", err);
});
