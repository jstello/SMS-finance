const express = require('express'); // Import Express
const admin = require("firebase-admin");
const path = require('path');
const fs = require('fs'); // Require file system module
const csv = require('fast-csv'); // Require fast-csv

// --- IMPORTANT: Path to your service account key --- 
// Adjust this path if your key file is located elsewhere relative to this script.
const serviceAccountPath = path.resolve(__dirname, '../scripts/finanzaspersonales-6c62be72-firebase-adminsdk-fbsvc-49a6c72b7d.json');

console.log(`Initializing Firebase Admin SDK with key: ${serviceAccountPath}`);

try {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccountPath),
    // You might need your projectId here if it's not in the key file or default env vars
    // projectId: "finanzaspersonales-6c62be72", 
  });
  console.log("Firebase Admin SDK Initialized Successfully.");
} catch (initError) {
  console.error("[Initialization Error] Failed to initialize Firebase Admin SDK:", initError);
  console.error("\nPlease ensure the path to your service account key is correct and the file exists.");
  process.exit(1); // Exit if initialization fails
}

const db = admin.firestore();

// --- Express App Setup ---
const app = express();
const PORT = process.env.PORT || 3000; // Use environment port or default to 3000

// --- Helper Function to Generate HTML ---
function generateHtml(searchTerm, results, error) {
  let resultsHtml = '';
  if (error) {
    resultsHtml = `<p style="color: red;">Error: ${error}</p>`;
  } else if (results && results.length > 0) {
    resultsHtml = `
      <h2>Results for "${searchTerm}" (${results.length} found):</h2>
      <table border="1" cellpadding="5" cellspacing="0" style="border-collapse: collapse; width: 100%;">
        <thead>
          <tr>
            <th>Date</th>
            <th>Description</th>
            <th>Amount</th>
            <th>Income?</th>
            <th>ID</th>
          </tr>
        </thead>
        <tbody>
          ${results.map(item => `
            <tr>
              <td>${item.date}</td>
              <td style="white-space: pre-wrap; word-break: break-word;">${item.description}</td>
              <td>${item.amount}</td>
              <td>${item.isIncome}</td>
              <td>${item.id}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
  } else if (results) { // Results array exists but is empty
    resultsHtml = `<p>No transactions found containing "${searchTerm}".</p>`;
  } 

  return `
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>Firestore Transaction Search</title>
      <style>
        body { font-family: sans-serif; line-height: 1.6; padding: 20px; }
        input[type="text"] { padding: 8px; margin-right: 5px; }
        button { padding: 8px 12px; cursor: pointer; }
        table { margin-top: 20px; }
        th, td { text-align: left; vertical-align: top; }
      </style>
    </head>
    <body>
      <h1>Search Transactions by Description</h1>
      <form action="/search" method="GET">
        <input type="text" name="term" placeholder="Enter search term (e.g., EPAM)" value="${searchTerm || ''}" required>
        <button type="submit">Search</button>
      </form>
      <hr style="margin: 20px 0;">
      ${resultsHtml}
      <p style="margin-top: 20px; font-size: 0.8em; color: #666;">
        Note: This search fetches all transactions and filters them. It may be slow for large datasets.
      </p>
    </body>
    </html>
  `;
}

// --- Routes ---

// Root route: Display the search form
app.get('/', (req, res) => {
  res.send(generateHtml('', null, null)); // Initial page with no results
});

// Search route: Fetch data and display results
app.get('/search', async (req, res) => {
  const searchTerm = req.query.term; // Get search term from query param
  if (!searchTerm) {
    return res.status(400).send(generateHtml('', null, 'Search term is required.'));
  }

  console.log(`\nReceived search request for term: "${searchTerm}"`);
  let foundTransactions = [];
  let searchError = null;

  try {
    console.log('Fetching ALL transactions...');
    const txGroupSnap = await db.collectionGroup('transactions').get();
    console.log(`Fetched ${txGroupSnap.size} total transaction(s). Filtering...`);

    if (!txGroupSnap.empty) {
      txGroupSnap.forEach(doc => {
        const data = doc.data();
        const description = data.description || "";
        if (description.toLowerCase().includes(searchTerm.toLowerCase())) {
          foundTransactions.push({
            id: doc.id,
            date: data.date?.toDate ? data.date.toDate().toLocaleString() : 'Invalid Date',
            description: description,
            amount: data.amount || 'N/A', // Add amount if available
            isIncome: data.isIncome // Add income flag if available
          });
        }
      });
    }
    console.log(`Found ${foundTransactions.length} transactions matching "${searchTerm}"`);
  } catch (error) {
    console.error("[Firestore Error] Failed during transaction fetch or processing:", error);
    searchError = "Failed to fetch or process transactions. Check server logs.";
  }

  // Send HTML response with results or error
  res.send(generateHtml(searchTerm, foundTransactions, searchError));
});

// --- CSV Backup Function for Transactions ---
async function backupTransactionsToCsv() {
  const backupFilePath = path.resolve(__dirname, 'transactions_backup.csv');
  console.log(`\nStarting Firestore backup to CSV: ${backupFilePath}`);

  try {
    console.log('Fetching ALL transactions from collection group \'transactions\'...');
    const snapshot = await db.collectionGroup('transactions').get();
    console.log(`Fetched ${snapshot.size} transaction documents.`);

    if (snapshot.empty) {
      console.log("No transactions found to backup.");
      return Promise.resolve(); // Resolve promise even if empty
    }

    const headersSet = new Set();
    snapshot.docs.forEach(doc => {
      Object.keys(doc.data()).forEach(key => headersSet.add(key));
    });
    const headers = Array.from(headersSet).sort();
    console.log("Determined transaction headers:", headers.join(', '));

    const dataForCsv = snapshot.docs.map(doc => {
      const docData = doc.data();
      const row = { id: doc.id };
      headers.forEach(header => {
        let value = docData[header];
        if (value && typeof value.toDate === 'function') {
          value = value.toDate().toISOString();
        } else if (typeof value === 'object' && value !== null) {
          try {
            value = JSON.stringify(value);
          } catch (e) {
            value = '[Error stringifying object]';
          }
        }
        row[header] = value !== undefined && value !== null ? value : '';
      });
      return row;
    });

    // Return a promise that resolves on finish or rejects on error
    return new Promise((resolve, reject) => {
        const ws = fs.createWriteStream(backupFilePath);
        csv.write(dataForCsv, { headers: ['id', ...headers], includeEndRowDelimiter: true })
           .pipe(ws)
           .on('finish', () => {
               console.log(`Successfully wrote ${dataForCsv.length} transaction records to ${backupFilePath}`);
               resolve(); // Resolve the promise on successful finish
           })
           .on('error', (error) => {
               console.error("Error writing transactions CSV file:", error);
               reject(error); // Reject the promise on error
           });
    });

  } catch (error) {
    console.error("[Firestore Transaction Backup Error] Failed:", error);
    return Promise.reject(error); // Return rejected promise
  }
}

// --- CSV Backup Function for Categories ---
async function backupCategoriesToCsv() {
    const backupFilePath = path.resolve(__dirname, 'categories_backup.csv');
    console.log(`\n[Categories] Starting Firestore backup to CSV: ${backupFilePath}`);

    try {
        console.log('[Categories] Fetching ALL categories from collection group \'categories\'...');
        const snapshot = await db.collectionGroup('categories').get(); 
        console.log(`[Categories] Fetched ${snapshot.size} category documents.`);

        if (snapshot.empty) {
            console.log("[Categories] No categories found to backup.");
            return Promise.resolve(); 
        }

        console.log("[Categories] Determining headers...");
        const headersSet = new Set();
        snapshot.docs.forEach(doc => {
            Object.keys(doc.data()).forEach(key => headersSet.add(key));
        });
        const headers = Array.from(headersSet).sort();
        console.log("[Categories] Determined headers:", headers.join(', '));

        console.log("[Categories] Preparing data for CSV...");
        const dataForCsv = snapshot.docs.map(doc => {
            const docData = doc.data();
            const row = { id: doc.id }; 
            row.parentPath = doc.ref.parent.parent.path; // e.g., users/userId
            headers.forEach(header => {
                let value = docData[header];
                if (typeof value === 'object' && value !== null) {
                    try {
                        value = JSON.stringify(value);
                    } catch (e) {
                        value = '[Error stringifying object]';
                    }
                }
                row[header] = value !== undefined && value !== null ? value : '';
            });
            return row;
        });
        console.log(`[Categories] Prepared ${dataForCsv.length} rows for CSV.`);

        console.log("[Categories] Writing data to CSV...");
        return new Promise((resolve, reject) => {
            const ws = fs.createWriteStream(backupFilePath);
            csv.write(dataForCsv, { headers: ['id', 'parentPath', ...headers], includeEndRowDelimiter: true })
               .pipe(ws)
               .on('finish', () => {
                   console.log(`[Categories] Successfully wrote ${dataForCsv.length} category records to ${backupFilePath}`);
                   resolve(); 
               })
               .on('error', (error) => {
                   console.error("[Categories] Error writing categories CSV file:", error);
                   reject(error); 
               });
        });

    } catch (error) {
        console.error("[Firestore Category Backup Error] Failed:", error);
        return Promise.reject(error); 
    }
}

// --- Main Execution Logic ---
// Check command line arguments for mode
const args = process.argv.slice(2);
const modeArg = args.find(arg => arg.startsWith('--mode='));
const mode = modeArg ? modeArg.split('=')[1] : 'default';

if (mode === 'backup') {
    console.log("Starting backup process for transactions and categories...");
    backupTransactionsToCsv()
        .then(() => backupCategoriesToCsv()) // Chain the category backup
        .then(() => {
            console.log("\nAll backup operations completed successfully.");
            process.exit(0); // Exit cleanly after both backups
        })
        .catch(err => {
            console.error("\nAn error occurred during the backup process:", err);
            process.exit(1); // Exit with error code
        });
} else {
    console.log("Running in default mode (no action). Use 'npm run backup' to create CSV backups.");
    process.exit(0); // Exit cleanly in default mode
}

// Remove or comment out the standalone app.listen call if it exists below
// app.listen(PORT, () => { ... }); 