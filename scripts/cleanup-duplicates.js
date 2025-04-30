   // scripts/cleanup-duplicates.js
   const admin = require("firebase-admin");

   // Initialize Admin SDK
   try {
     admin.initializeApp({
       projectId: "finanzaspersonales-6c62be72",
       // Ensure credential is used if env var is set
       credential: admin.credential.applicationDefault(), 
     });
   } catch (initError) {
     console.error("[Initialization Error] Failed to initialize Firebase Admin SDK:", initError);
     process.exit(1);
   }

   const db = admin.firestore();
   console.log("Admin SDK Initialized.");

   (async () => {
     console.log('Starting script using collectionGroup query...');

     try {
       // Query the 'transactions' collection group directly
       const txGroupSnap = await db.collectionGroup('transactions').get();
       console.log(`Found ${txGroupSnap.size} total transaction document(s) via collectionGroup query.`);

       // Group docs by their description
       const byDescription = {};
       for (const doc of txGroupSnap.docs) {
         const data = doc.data();
         // Use description as the key, handle potential null/undefined descriptions
         const desc = data.description || "__MISSING_DESCRIPTION__";
         if (!byDescription[desc]) byDescription[desc] = [];
         byDescription[desc].push(doc); // Store the full DocumentSnapshot
       }

       console.log(`  Finished grouping. Found ${Object.keys(byDescription).length} unique description keys.`);
       let deletedCount = 0;
       for (const [desc, docs] of Object.entries(byDescription)) {
         if (docs.length > 1) {
           console.log(`\nFound ${docs.length} duplicates for description: "${desc.substring(0, 50)}..."`);

           // --- Logic to choose the best document to keep ---
           let bestDocToKeep = docs[0]; // Start by assuming the first is best
           for (let i = 1; i < docs.length; i++) {
             const currentData = bestDocToKeep.data();
             const candidateData = docs[i].data();

             // Preference: Keep doc with non-null provider over null provider
             if (currentData.provider == null && candidateData.provider != null) {
               bestDocToKeep = docs[i];
               continue; // Found a better one
             }
             // Preference: Keep doc with non-null categoryId over null categoryId (if providers are same or both null)
             if (currentData.categoryId == null && candidateData.categoryId != null && currentData.provider === candidateData.provider) {
               bestDocToKeep = docs[i];
               continue; // Found a better one
             }
             // Optional: Add more criteria if needed (e.g., latest date?)
           }

           console.log(`  Keeping doc ID: ${bestDocToKeep.id} (Provider: ${bestDocToKeep.data().provider}, Category: ${bestDocToKeep.data().categoryId})`);

           // --- Delete all other documents with the same description ---
           for (const docToDelete of docs) {
             if (docToDelete.id !== bestDocToKeep.id) {
               console.log(`  Deleting doc ID: ${docToDelete.id}`);
               await docToDelete.ref.delete();
               deletedCount++;
             }
           }
         }
       }
       console.log(`\nFinished processing. Deleted ${deletedCount} duplicate transaction(s) based on description.`);

     } catch (error) {
       console.error("[Script Error] Failed during collectionGroup query or processing:", error);
       process.exit(1);
     }

     console.log("\nDone deduping transactions based on description.");
     process.exit(0);
   })();