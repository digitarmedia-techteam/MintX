// Firebase Configuration
const firebaseConfig = {
    apiKey: "AIzaSyALSJhoUIei6Xp-fdMK7htxlaFfSY24pSs",
    authDomain: "mintx-a0bf2.firebaseapp.com",
    projectId: "mintx-a0bf2",
    storageBucket: "mintx-a0bf2.firebasestorage.app",
    messagingSenderId: "150365317778",
    appId: "1:150365317778:web:placeholder" // Web App ID is not strict for Firestore access usually
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);
const db = firebase.firestore();

document.addEventListener('DOMContentLoaded', () => {
    // Navigation
    setupNavigation();

    // Modals
    setupModals();



    // NOTE: For this Admin Panel to work without Login, you MUST set your Firestore Rules to:
    // allow read, write: if true;
    console.log("Admin Panel: Initializing data...");

    // Load Initial Data
    loadUsers();
    loadCategories();
    loadQuestions();
    loadDashboardStats();
});

function setupNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    const sections = document.querySelectorAll('.view-section');

    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            navItems.forEach(nav => nav.classList.remove('active'));
            sections.forEach(section => section.style.display = 'none');

            item.classList.add('active');
            const tabId = item.getAttribute('data-tab');
            const targetId = `view-${tabId}`;
            const targetSection = document.getElementById(targetId);

            if (targetSection) {
                targetSection.style.display = 'block';
                targetSection.classList.add('active');
            }
        });
    });
}

function setupModals() {
    // Open Buttons
    document.getElementById('btn-add-category').addEventListener('click', () => {
        document.getElementById('modal-add-category').classList.add('active');
    });

    document.getElementById('btn-add-question').addEventListener('click', () => {
        document.getElementById('modal-add-question').classList.add('active');
        populateCategorySelect(); // Refresh categories in dropdown
    });

    document.getElementById('btn-bulk-upload').addEventListener('click', () => {
        document.getElementById('modal-bulk-upload').classList.add('active');
    });

    // Close Buttons
    document.querySelectorAll('.close-modal').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.modal').forEach(m => m.classList.remove('active'));
        });
    });

    // Close on click outside
    window.addEventListener('click', (e) => {
        if (e.target.classList.contains('modal')) {
            e.target.classList.remove('active');
        }
    });

    // Forms
    document.getElementById('form-add-category').addEventListener('submit', async (e) => {
        e.preventDefault();
        const name = document.getElementById('cat-name').value;
        const desc = document.getElementById('cat-desc').value;

        try {
            await db.collection('quiz_categories').add({
                name: name,
                description: desc,
                createdAt: firebase.firestore.FieldValue.serverTimestamp()
            });
            alert('Category Added!');
            document.getElementById('modal-add-category').classList.remove('active');
            e.target.reset();
            loadCategories();
        } catch (error) {
            console.error(error);
            alert('Error creating category: ' + error.message);
        }
    });

    document.getElementById('form-add-question').addEventListener('submit', async (e) => {
        e.preventDefault();

        // Validation
        const levelInput = document.getElementById('q-level').value;
        if (levelInput) {
            const validation = validateLevel(levelInput);
            if (!validation.valid) {
                alert(validation.message);
                return;
            }
        }

        // Gather Options Dynamically
        const answers = {};
        const correct_answers = {};
        const correctVal = document.getElementById('q-correct').value;

        const optionsContainer = document.getElementById('add-options-container');
        Array.from(optionsContainer.children).forEach((row, idx) => {
            const char = getOptionChar(idx); // a, b, c...
            const key = `answer_${char}`;
            const val = row.querySelector('input').value;
            answers[key] = val;

            // Assume single correct answer logic (since dropdown is single select)
            correct_answers[`${key}_correct`] = (key === correctVal) ? "true" : "false";
        });

        const questionData = {
            category: document.getElementById('q-category').value,
            difficulty: document.getElementById('q-difficulty').value,
            level: document.getElementById('q-level').value ? parseInt(document.getElementById('q-level').value) : null,
            question: document.getElementById('q-text').value,
            explanation: document.getElementById('q-explanation').value,
            answers: answers,
            correct_answers: correct_answers,
            createdAt: firebase.firestore.FieldValue.serverTimestamp()
        };

        try {
            await db.collection('questions').add(questionData);
            alert('Question Added!');
            document.getElementById('modal-add-question').classList.remove('active');
            e.target.reset();
            loadQuestionsV2(questionData.category);
        } catch (error) {
            console.error(error);
            alert('Error adding question: ' + error.message);
        }
    });

    // Bulk Upload UI
    document.getElementById('btn-show-json-help').addEventListener('click', () => {
        const help = document.getElementById('json-format-help');
        help.style.display = help.style.display === 'none' ? 'block' : 'none';
    });

    document.getElementById('btn-import-file').addEventListener('click', () => {
        document.getElementById('bulk-file-input').click();
    });

    const textArea = document.getElementById('bulk-json');
    const lineNumbers = document.getElementById('bulk-line-numbers');

    const updateLineNumbers = () => {
        const lines = textArea.value.split('\n').length;
        lineNumbers.innerHTML = Array(lines).fill(0).map((_, i) => i + 1).join('<br>');
    };

    const syncScroll = () => {
        lineNumbers.scrollTop = textArea.scrollTop;
    };

    textArea.addEventListener('input', updateLineNumbers);
    textArea.addEventListener('scroll', syncScroll);

    document.getElementById('bulk-file-input').addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = (event) => {
            try {
                const json = JSON.parse(event.target.result);
                textArea.value = JSON.stringify(json, null, 4);
                updateLineNumbers();
            } catch (err) {
                alert("Error parsing JSON file: " + err.message);
            }
        };
        reader.readAsText(file);
        e.target.value = ''; // Reset
    });

    document.getElementById('btn-format-json').addEventListener('click', () => {
        try {
            const val = textArea.value;
            if (!val.trim()) return;
            const obj = JSON.parse(val);
            textArea.value = JSON.stringify(obj, null, 4);
            updateLineNumbers();
        } catch (e) {
            alert("Invalid JSON: " + e.message);
        }
    });

    // Bulk Upload Form
    document.getElementById('form-bulk-upload').addEventListener('submit', async (e) => {
        e.preventDefault();
        const jsonStr = document.getElementById('bulk-json').value;

        try {
            let questions;
            try {
                questions = JSON.parse(jsonStr);
            } catch (err) {
                // Try to extract line number from position
                const match = err.message.match(/at position (\d+)/);
                if (match) {
                    const pos = parseInt(match[1], 10);
                    const linesUpToError = jsonStr.substring(0, pos).split('\n').length;
                    throw new Error(`Invalid JSON Syntax at Line ${linesUpToError}: ${err.message}`);
                }
                throw new Error("Invalid JSON Syntax: " + err.message);
            }

            if (!Array.isArray(questions)) throw new Error("Root must be a JSON Array [ ... ].");
            if (questions.length === 0) throw new Error("The array is empty.");

            // Validation Phase
            const validationErrors = [];

            questions.forEach((q, index) => {
                const errContext = `Item #${index + 1}`;

                if (typeof q !== 'object' || q === null) {
                    validationErrors.push(`${errContext}: Not an object.`);
                    return;
                }

                // 1. Question (Required String)
                if (!q.question || typeof q.question !== 'string' || !q.question.trim()) {
                    validationErrors.push(`${errContext}: Missing or invalid 'question'.`);
                }

                // 2. Category (Optional in JSON, but required globally)
                const cat = q.category || selectedCategory;
                if (!cat) {
                    validationErrors.push(`${errContext}: Missing 'category'. Please select a category or include it in JSON.`);
                }

                // 3. Level (Required Number)
                if (q.level === undefined || q.level === null) {
                    validationErrors.push(`${errContext}: Missing 'level'.`);
                } else if (typeof q.level !== 'number') {
                    validationErrors.push(`${errContext}: 'level' must be a number (e.g. 1).`);
                }

                // 4. Answers (Strict Map of Strings)
                if (!q.answers || typeof q.answers !== 'object') {
                    validationErrors.push(`${errContext}: Missing or invalid 'answers' object.`);
                } else {
                    ['answer_a', 'answer_b', 'answer_c', 'answer_d'].forEach(key => {
                        const val = q.answers[key];
                        if (val === undefined || val === null) {
                            validationErrors.push(`${errContext}: Missing '${key}' in answers.`);
                        } else if (typeof val !== 'string') {
                            validationErrors.push(`${errContext}: '${key}' in answers must be a string.`);
                        } else if (!val.trim()) {
                            validationErrors.push(`${errContext}: '${key}' in answers cannot be empty.`);
                        }
                    });
                }

                // 5. Correct Answers (Strict Map of "true"/"false" strings)
                const ca = q.correct_answers || q.correctAnswers;
                if (!ca || typeof ca !== 'object') {
                    validationErrors.push(`${errContext}: Missing 'correct_answers' object.`);
                } else {
                    ['answer_a_correct', 'answer_b_correct', 'answer_c_correct', 'answer_d_correct'].forEach(key => {
                        const val = ca[key];
                        if (val === undefined || val === null) {
                            validationErrors.push(`${errContext}: Missing '${key}' in correct_answers.`);
                        } else if (val !== "true" && val !== "false") {
                            validationErrors.push(`${errContext}: '${key}' must be explicitly "true" or "false" (string).`);
                        }
                    });
                }
            });

            if (validationErrors.length > 0) {
                // Show errors
                const maxShow = 5;
                let msg = "Validation Failed (Upload Cancelled):\n";
                msg += validationErrors.slice(0, maxShow).join("\n");
                if (validationErrors.length > maxShow) {
                    msg += `\n...and ${validationErrors.length - maxShow} more errors.`;
                }
                throw new Error(msg);
            }

            // Upload Phase - Batching (Firestore limit 500)
            const chunkSize = 450;
            const chunks = [];
            for (let i = 0; i < questions.length; i += chunkSize) {
                chunks.push(questions.slice(i, i + chunkSize));
            }

            let totalUploaded = 0;

            for (const chunk of chunks) {
                const batch = db.batch();
                chunk.forEach(q => {
                    const docRef = db.collection('questions').doc();

                    const ca = q.correct_answers || q.correctAnswers;

                    const qData = {
                        category: q.category || selectedCategory,
                        difficulty: q.difficulty || "Medium",
                        level: Number(q.level),
                        question: q.question.trim(),
                        explanation: q.explanation || "",
                        answers: {
                            answer_a: q.answers.answer_a.toString(),
                            answer_b: q.answers.answer_b.toString(),
                            answer_c: q.answers.answer_c.toString(),
                            answer_d: q.answers.answer_d.toString()
                        },
                        correct_answers: {
                            answer_a_correct: ca.answer_a_correct,
                            answer_b_correct: ca.answer_b_correct,
                            answer_c_correct: ca.answer_c_correct,
                            answer_d_correct: ca.answer_d_correct
                        },
                        createdAt: firebase.firestore.FieldValue.serverTimestamp()
                    };
                    batch.set(docRef, qData);
                    totalUploaded++;
                });
                await batch.commit();
            }

            showMessage('success', 'Bulk Upload', `Successfully uploaded ${totalUploaded} questions.`);
            document.getElementById('modal-bulk-upload').classList.remove('active');
            document.getElementById('bulk-json').value = '';

            // Reload
            if (selectedCategory) loadQuestionsV2(selectedCategory);

        } catch (error) {
            console.error(error);
            showMessage('error', 'Upload Failed', error.message);
        }
    });
}

async function loadUsers() {
    const tableBody = document.getElementById('users-table-body');
    if (!tableBody) return;
    tableBody.innerHTML = '<tr><td colspan="7">Loading...</td></tr>';

    try {
        const snapshot = await db.collection('users').orderBy('createdAt', 'desc').get();
        userMap = {}; // Reset

        let html = '';
        snapshot.forEach(doc => {
            const user = doc.data();
            userMap[doc.id] = user; // Store

            const initials = user.name ? getInitials(user.name) : '?';
            const color = getRandomColor();
            const status = user.status || 'Active';
            const statusClass = status.toLowerCase() === 'active' ? 'active' : 'pending'; // simple logic

            html += `
            <tr onclick="openEditUser('${doc.id}')" style="cursor: pointer; transition: background 0.2s;">
                <td><span style="font-family: monospace; color: var(--text-muted);">${doc.id.substring(0, 8)}...</span></td>
                <td>
                    <div class="user-cell">
                        <div class="avatar-sm" style="background: ${color}">${initials}</div>
                        <span>${user.name || 'Anonymous'}</span>
                    </div>
                </td>
                <td>${user.phone || '-'}</td>
                <td style="font-weight: bold; color: var(--gold);">${(user.mintBalance || 0).toLocaleString()}</td>
                <td>${user.categories ? user.categories.length : 0}</td>
                <td><span class="status-pill ${statusClass}">${status}</span></td>
                <td>
                    <button class="btn-sm" style="color: var(--text-muted);"><i class="fa-solid fa-pen-to-square"></i></button>
                </td>
            </tr>`;
        });
        tableBody.innerHTML = html;
    } catch (error) {
        console.error("Error loading users:", error);
        if (error.code === 'permission-denied') {
            tableBody.innerHTML = '<tr><td colspan="7" style="color:#F87171; text-align:center; padding: 20px;"><strong>⚠️ Permission Denied</strong><br>Go to Firebase Console &gt; Firestore &gt; Rules and set:<br><code>allow read, write: if true;</code></td></tr>';
        } else {
            tableBody.innerHTML = '<tr><td colspan="7" style="color:red">Error loading data: ' + error.message + '</td></tr>';
        }
    }
}

// Edit User Logic
// Edit User Logic
window.currentEditingUserId = null;

window.openEditUser = function (id) {
    const user = userMap[id];
    if (!user) return;

    window.currentEditingUserId = id;

    document.getElementById('edit-user-id').value = id;
    document.getElementById('edit-user-name').value = user.name || '';
    document.getElementById('edit-user-phone').value = user.phone || '';
    document.getElementById('edit-user-balance').value = user.mintBalance || 0;

    // Display fields
    if (document.getElementById('edit-user-display-name')) document.getElementById('edit-user-display-name').innerText = user.name || 'User';
    if (document.getElementById('edit-user-display-phone')) document.getElementById('edit-user-display-phone').innerText = user.phone || '';

    // Status
    const statusSelect = document.getElementById('edit-user-status');
    statusSelect.value = user.status || 'Active';

    // Stats Preview
    const categories = user.categories || [];
    if (document.getElementById('edit-user-cats-count')) document.getElementById('edit-user-cats-count').innerText = categories.length;

    // Render Chips (Hidden in new UI)
    const chipsContainer = document.getElementById('edit-user-cats-list');
    if (chipsContainer) {
        chipsContainer.innerHTML = '';
        if (categories.length > 0) {
            categories.forEach(cat => {
                const chip = document.createElement('div');
                chip.style.cssText = "background: rgba(52, 211, 153, 0.2); color: var(--primary); padding: 2px 8px; border-radius: 12px; font-size: 0.75rem; border: 1px solid rgba(52, 211, 153, 0.3); text-transform: capitalize;";
                chip.innerText = cat;
                chipsContainer.appendChild(chip);
            });
        }
    }

    document.getElementById('modal-edit-user').classList.add('active');
}

window.openUserHistory = function () {
    if (!window.currentEditingUserId) return;
    document.getElementById('modal-user-history').classList.add('active');
    loadUserTransactions(window.currentEditingUserId);
}

// Load Transactions (Real-time)
async function loadUserTransactions(userId) {
    const container = document.getElementById('edit-user-transactions');
    container.innerHTML = '<div style="padding: 40px; text-align: center; color: var(--text-muted);"><i class="fa-solid fa-spinner fa-spin"></i> Loading...</div>';

    // Unsubscribe from previous listener if active
    if (window.currentTxUnsubscribe) {
        window.currentTxUnsubscribe();
        window.currentTxUnsubscribe = null;
    }

    const renderTransactions = (docs) => {
        if (docs.length === 0) {
            container.innerHTML = '<div style="padding: 40px; text-align: center; color: var(--text-muted); font-style: italic;">No transactions found.</div>';
            return;
        }

        let html = '<div class="tx-list">';
        docs.forEach(t => {
            const isCredit = t.type === 'credit';
            const colorClass = isCredit ? 'credit' : 'debit';
            const iconName = isCredit ? 'fa-arrow-down' : 'fa-arrow-up';
            const sign = isCredit ? '+' : '-';

            // Robust Date Formatting
            let dateStr = '-';
            if (t.timestamp) {
                const millis = (typeof t.timestamp === 'object' && t.timestamp.toMillis) ? t.timestamp.toMillis() : t.timestamp;
                dateStr = new Date(millis).toLocaleString();
            }

            html += `
            <div class="tx-item">
                <div class="tx-icon ${colorClass}">
                    <i class="fa-solid ${iconName}"></i>
                </div>
                <div class="tx-content">
                    <div class="tx-title">${t.title || 'Transaction'}</div>
                    ${(t.description && t.description !== t.title) ? `<div class="tx-desc">${t.description}</div>` : ''}
                    <div class="tx-meta">${dateStr}</div>
                </div>
                <div class="tx-amount ${colorClass}">
                    ${sign}${t.amount}
                </div>
                <button class="btn-sm" onclick="event.stopPropagation(); deleteUserTransaction('${userId}', '${t._docId}')" 
                        style="background: transparent; color: var(--text-muted); padding: 8px; margin-left: 10px; opacity: 0.5; transition: 0.2s; cursor: pointer;"
                        onmouseover="this.style.opacity='1'; this.style.color='var(--red)'"
                        onmouseout="this.style.opacity='0.5'; this.style.color='var(--text-muted)'"
                        title="Delete Transaction">
                    <i class="fa-solid fa-trash"></i>
                </button>
            </div>`;
        });
        html += '</div>';
        container.innerHTML = html;
    };

    try {
        window.currentTxUnsubscribe = db.collection('users').doc(userId).collection('TransactionHistory')
            .orderBy('timestamp', 'desc')
            .limit(20)
            .onSnapshot(snap => {
                // Convert to array and Client-side Sort
                let docs = [];
                snap.forEach(doc => {
                    let d = doc.data();
                    d._docId = doc.id;
                    docs.push(d);
                });

                docs.sort((a, b) => {
                    const tA = (a.timestamp && typeof a.timestamp === 'object' && a.timestamp.toMillis) ? a.timestamp.toMillis() : (a.timestamp || 0);
                    const tB = (b.timestamp && typeof b.timestamp === 'object' && b.timestamp.toMillis) ? b.timestamp.toMillis() : (b.timestamp || 0);
                    return tB - tA; // Descending
                });

                renderTransactions(docs);

            }, error => {
                console.error("Tx Listener Error:", error);

                // Fallback for missing index
                if (error && error.code === 'failed-precondition') {
                    console.warn("Index missing. Falling back to client-side sort (One-time fetch).");

                    db.collection('users').doc(userId).collection('TransactionHistory')
                        .limit(50)
                        .get()
                        .then(snap => {
                            const docs = [];
                            snap.forEach(doc => {
                                let d = doc.data();
                                d._docId = doc.id;
                                docs.push(d);
                            });

                            // Sort descending
                            docs.sort((a, b) => {
                                const tA = (a.timestamp && typeof a.timestamp === 'object' && a.timestamp.toMillis) ? a.timestamp.toMillis() : (a.timestamp || 0);
                                const tB = (b.timestamp && typeof b.timestamp === 'object' && b.timestamp.toMillis) ? b.timestamp.toMillis() : (b.timestamp || 0);
                                return tB - tA;
                            });

                            // Take top 20
                            renderTransactions(docs.slice(0, 20));
                        })
                        .catch(e2 => {
                            container.innerHTML = '<div style="text-align: center; color: var(--red); padding: 20px;">Error loading history (fallback failed).</div>';
                        });
                } else {
                    container.innerHTML = '<div style="text-align: center; color: var(--red); padding: 20px;">Error loading history.</div>';
                }
            });

    } catch (e) {
        console.error("Setup Error:", e);
        container.innerHTML = '<div style="text-align: center; color: var(--red);">Error setup.</div>';
    }
}

// Bonus Logic
// Bonus Logic
window.addBonus = function (amount) {
    const balanceInput = document.getElementById('edit-user-balance');
    let current = parseFloat(balanceInput.value) || 0;
    let newValue = current + amount;

    // Enforce Limits
    const MAX_BALANCE = 500000;

    if (newValue < 0) {
        newValue = 0;
        showMessage('error', 'Limit Reached', 'Balance cannot be negative.');
    } else if (newValue > MAX_BALANCE) {
        newValue = MAX_BALANCE;
        showMessage('error', 'Limit Reached', 'Maximum Wallet Balance is 500,000.');
    }

    balanceInput.value = newValue;
}

window.applyCustomBonus = function () {
    const customInput = document.getElementById('custom-bonus');
    const amount = parseFloat(customInput.value);
    if (amount && !isNaN(amount)) {
        addBonus(amount);
        customInput.value = ''; // Reset
    }
}

// Submit Edit User
document.getElementById('form-edit-user').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('edit-user-id').value;
    const name = document.getElementById('edit-user-name').value;
    let newBalance = parseFloat(document.getElementById('edit-user-balance').value);
    const status = document.getElementById('edit-user-status').value;

    if (newBalance < 0) {
        newBalance = 0;
        document.getElementById('edit-user-balance').value = 0;
        showMessage('error', 'Limit Reached', 'Balance cannot be negative.');
    } else if (newBalance > 500000) {
        newBalance = 500000;
        document.getElementById('edit-user-balance').value = 500000;
        showMessage('error', 'Limit Reached', 'Maximum Wallet Balance is 500,000.');
    }

    const oldUser = userMap[id];
    const oldBalance = oldUser ? (oldUser.mintBalance || 0) : 0;
    const diff = newBalance - oldBalance;

    try {
        const batch = db.batch();
        const userRef = db.collection('users').doc(id);

        batch.update(userRef, {
            name: name,
            mintBalance: newBalance,
            status: status,
            updatedAt: Date.now()
        });

        // Log Transaction if balance changed
        if (diff !== 0) {
            const txRef = db.collection('users').doc(id).collection('TransactionHistory').doc();
            const isCredit = diff > 0;
            batch.set(txRef, {
                id: txRef.id,
                amount: Math.abs(diff),
                type: isCredit ? 'credit' : 'debit',
                title: isCredit ? 'Bonus' : 'Penalty',
                description: isCredit ? 'Bonus added by Admin' : 'Penalty applied by Admin',
                status: 'completed',
                timestamp: Date.now()
            });
        }

        await batch.commit();

        showMessage('success', 'User Updated', 'User details saved successfully.');
        document.getElementById('modal-edit-user').classList.remove('active');
        loadUsers(); // Refresh list

    } catch (error) {
        console.error(error);
        showMessage('error', 'Update Failed', error.message);
    }
});

// State
let selectedCategory = null;
let currentDifficulty = null; // New State
let questionMap = {};
let allCategories = [];
let userMap = {}; // New State

// Modals
function showMessage(type, title, message) {
    const modal = document.getElementById('modal-message');
    const icon = document.getElementById('msg-icon');
    const titleEl = document.getElementById('msg-title');
    const textEl = document.getElementById('msg-text');

    modal.classList.add('active');

    if (type === 'success') {
        icon.className = 'fa-solid fa-circle-check msg-icon success';
    } else {
        icon.className = 'fa-solid fa-circle-xmark msg-icon error';
    }

    titleEl.innerText = title;
    textEl.innerText = message;
}

window.closeMessageModal = function () {
    document.getElementById('modal-message').classList.remove('active');
}

// Logic
async function loadCategories() {
    const container = document.getElementById('categories-list-full');
    if (!container) return;

    try {
        const snapshot = await db.collection('quiz_categories').orderBy('name').get();
        if (snapshot.empty) {
            container.innerHTML = '<div style="padding:10px">No categories.</div>';
            return;
        }

        allCategories = []; // Reset
        let html = '';
        snapshot.forEach(doc => {
            const cat = doc.data();
            allCategories.push(cat.name); // Store for select
            const isActive = selectedCategory === cat.name ? 'active' : '';

            html += `
            <div class="category-item ${isActive}" onclick="selectCategory('${cat.name}')">
                <span style="font-weight:500;">${cat.name}</span>
                <div class="cat-actions">
                    <i class="fa-solid fa-pen-to-square" 
                       onclick="event.stopPropagation(); openEditCategory('${doc.id}', '${cat.name}', '${cat.description || ''}')"
                       style="font-size: 0.9rem;" title="Edit"></i>
                </div>
            </div>`;
        });
        container.innerHTML = html;

    } catch (error) {
        console.error("Error loading categories:", error);
        if (error.code === 'permission-denied') {
            container.innerHTML = '<div style="color:#F87171; padding:10px;">⚠️ Permission Denied</div>';
        } else {
            // Fallback: try loading without sort
            try {
                const snapshot = await db.collection('quiz_categories').get();
                // ... same rendering logic ...
                // actually simpler to just alert error or show simpler list.
                // For now, just show error message in container.
                container.innerHTML = `<div style="color:#F87171; padding:10px;">Error: ${error.message}</div>`;
            } catch (e2) {
                container.innerHTML = '<div style="color:#F87171; padding:10px;">Error loading categories.</div>';
            }
        }
    }
}

window.populateCategorySelect = function () {
    const selects = ['q-category', 'edit-q-category'];
    selects.forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            let opts = '<option value="" disabled selected>Select Category</option>';
            allCategories.forEach(c => {
                opts += `<option value="${c}">${c}</option>`;
            });
            el.innerHTML = opts;

            // Pre-select if we are in a category
            if (selectedCategory && id === 'q-category') {
                el.value = selectedCategory;
            }
        }
    });
}

window.selectCategory = function (name) {
    selectedCategory = name;
    currentDifficulty = null; // Reset difficulty

    // Update Header Text
    document.getElementById('selected-category-title').innerText = name;

    // Show Add Button and Chips
    document.getElementById('btn-add-question').style.display = 'block';
    document.getElementById('btn-bulk-upload').style.display = 'block'; // Added Bulk Upload Button
    document.getElementById('difficulty-chips').style.display = 'flex';

    // Reset Chips UI (Set All as active)
    document.querySelectorAll('.chip').forEach(c => {
        if (c.innerText === 'All') c.classList.add('active');
        else c.classList.remove('active');
    });

    // Refresh UI highlights
    loadCategories();
    // Load questions for this category
    loadQuestionsV2(name);
}

// Select Difficulty Logic
window.selectDifficulty = function (level) {
    if (level === 'All') {
        currentDifficulty = null;
    } else {
        // If clicking the same one, toggle it off (back to all)
        if (currentDifficulty === level) {
            currentDifficulty = null;
        } else {
            currentDifficulty = level;
        }
    }

    // Update Chips UI
    document.querySelectorAll('.chip').forEach(c => {
        const text = c.innerText;
        if (currentDifficulty === text) {
            c.classList.add('active');
        } else if (!currentDifficulty && text === 'All') {
            c.classList.add('active');
        } else {
            c.classList.remove('active');
        }
    });

    // Update Header Title
    const titleEl = document.getElementById('selected-category-title');
    if (selectedCategory) {
        titleEl.innerText = selectedCategory + (currentDifficulty ? ` - ${currentDifficulty}` : ' - All');
    }

    // Reload Questions (Client side filter handled inside)
    renderQuestions();
}

// Edit Category Logic
window.openEditCategory = function (id, name, desc) {
    document.getElementById('edit-cat-id').value = id;
    document.getElementById('edit-cat-old-name').value = name;
    document.getElementById('edit-cat-name').value = name;
    document.getElementById('edit-cat-desc').value = desc;
    document.getElementById('modal-edit-category').classList.add('active');
}

document.getElementById('form-edit-category').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('edit-cat-id').value;
    const oldName = document.getElementById('edit-cat-old-name').value;
    const newName = document.getElementById('edit-cat-name').value;
    const desc = document.getElementById('edit-cat-desc').value;

    try {
        const batch = db.batch();

        // 1. Update Category
        const catRef = db.collection('quiz_categories').doc(id);
        batch.update(catRef, { name: newName, description: desc });

        // 2. If name changed, update all questions (This can be expensive if many questions!)
        // Ideally data model uses ID linking, but if using name string:
        if (oldName !== newName) {
            const qSnapshot = await db.collection('questions').where('category', '==', oldName).get();
            qSnapshot.forEach(doc => {
                const qRef = db.collection('questions').doc(doc.id);
                batch.update(qRef, { category: newName });
            });
        }

        await batch.commit();
        showMessage('success', 'Updated', 'Category updated successfully!');
        document.getElementById('modal-edit-category').classList.remove('active');

        // Reload
        if (selectedCategory === oldName) selectedCategory = newName;
        loadCategories();
        if (selectedCategory) loadQuestionsV2(selectedCategory);

    } catch (error) {
        console.error(error);
        showMessage('error', 'Error', error.message);
    }
});


async function loadQuestions(category) {
    const container = document.getElementById('questions-list-full');
    if (!container) return;

    if (!category) {
        container.innerHTML = '<div style="text-align:center; padding: 2rem; color:var(--text-muted);"><i class="fa-solid fa-arrow-left"></i> Select a category</div>';
        return;
    }

    container.innerHTML = 'Loading questions...';
    questionMap = {}; // Reset

    try {
        const snapshot = await db.collection('questions')
            .where('category', '==', category)
            .orderBy('createdAt', 'desc')
            .get();

        if (snapshot.empty) {
            container.innerHTML = '<div style="padding:2rem; text-align:center; color:var(--text-muted)">No questions in this category.</div>';
            return;
        }

        let html = '';
        let index = 1;

        let questionCount = 0;

        snapshot.forEach(doc => {
            const q = doc.data();
            questionMap[doc.id] = q; // Store data

            // Client Side Filter
            if (currentDifficulty && q.difficulty !== currentDifficulty) {
                return; // Skip
            }

            questionCount++;

            html += `
            <div class="question-card" onclick="openEditQuestion('${doc.id}')" style="cursor: pointer;">
                <div class="q-number">${index++}</div>
                <div class="q-content">
                    <div class="q-text">${q.question || 'No Text'}</div>
                    ${q.explanation ? `<div style="font-size:0.8rem; color:var(--primary); margin-top:4px;"><em>💡 ${q.explanation}</em></div>` : ''}
                    <div class="q-meta">
                        <span><i class="fa-solid fa-layer-group"></i> ${q.difficulty}</span>
                        <span><i class="fa-solid fa-check"></i> ${getCorrectAnswerText(q)}</span>
                        <span style="color:var(--text-muted); font-size: 0.8em; margin-left: auto;">${formatDate(q.createdAt)}</span>
                    </div>
                </div>
                <div class="q-actions">
                    <button class="btn-sm" onclick="event.stopPropagation(); deleteQuestion('${doc.id}')" style="color:var(--red); background:rgba(248,113,113,0.1)">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                    <!-- Edit Button implied by Card Click -->
                </div>
            </div>`;
        });

        if (questionCount === 0) {
            container.innerHTML = `<div style="padding:2rem; text-align:center; color:var(--text-muted)">No ${currentDifficulty || ''} questions found in this category.</div>`;
        } else {
            container.innerHTML = html;
        }

    } catch (e) {
        console.error(e);
        // Fallback for missing index
        if (e.code === 'failed-precondition') {
            // Likely invalid query indexing
            container.innerHTML = 'Index missing. Loading without sort...';
            const fallbackSnap = await db.collection('questions').where('category', '==', category).get();
            // Render fallback
            let html = '';
            let index = 1;
            fallbackSnap.forEach(doc => {
                const q = doc.data();
                questionMap[doc.id] = q; // Store data

                // Client Side Filter Fallback
                if (currentDifficulty && q.difficulty !== currentDifficulty) return;

                html += `
                <div class="question-card" onclick="openEditQuestion('${doc.id}')" style="cursor: pointer;">
                    <div class="q-number">${index++}</div>
                    <div class="q-content">
                        <div class="q-text">${q.question || 'No Text'}</div>
                        ${q.explanation ? `<div style="font-size:0.8rem; color:var(--primary); margin-top:4px;"><em>💡 ${q.explanation}</em></div>` : ''}
                        <div class="q-meta">
                            <span><i class="fa-solid fa-layer-group"></i> ${q.difficulty}</span>
                            <span><i class="fa-solid fa-check"></i> ${getCorrectAnswerText(q)}</span>
                            <span style="color:var(--text-muted); font-size: 0.8em; margin-left: auto;">${formatDate(q.createdAt)}</span>
                        </div>
                    </div>
                    <div class="q-actions">
                        <button class="btn-sm" onclick="event.stopPropagation(); deleteQuestion('${doc.id}')" style="color:var(--red); background:rgba(248,113,113,0.1)">
                            <i class="fa-solid fa-trash"></i>
                        </button>
                    </div>
                </div>`;
            });
            container.innerHTML = html;
        } else {
            container.innerHTML = 'Error loading questions.';
        }
    }
}

function formatDate(timestamp) {
    if (!timestamp) return '';
    // Handle Firebase Timestamp or Date object
    const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
    return date.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit'
    });
}

// Edit Question Logic
window.openEditQuestion = function (id) {
    const q = questionMap[id];
    if (!q) return;

    populateCategorySelect(); // Ensure categories are ready

    document.getElementById('edit-q-id').value = id;
    document.getElementById('edit-q-category').value = q.category;
    document.getElementById('edit-q-difficulty').value = q.difficulty;
    document.getElementById('edit-q-level').value = q.level || '';
    document.getElementById('edit-q-text').value = q.question;
    document.getElementById('edit-q-explanation').value = q.explanation || '';

    // Clear and Populate Options
    const container = document.getElementById('edit-options-container');
    container.innerHTML = ''; // Start clean

    // Filter and sort keys: answer_a, answer_b...
    const keys = Object.keys(q.answers).filter(k => k.startsWith('answer_') && q.answers[k]);
    keys.sort(); // Should give answer_a, answer_b...

    keys.forEach((key, idx) => {
        // Reuse addOption logic or manually create (simpler to manually create for control)
        const char = getOptionChar(idx);
        const val = q.answers[key];

        const div = document.createElement('div');
        div.className = 'form-row option-row';
        div.dataset.index = idx;

        div.innerHTML = `
            <input type="text" value="${val}" placeholder="Option ${char.toUpperCase()}" required>
            <button type="button" class="btn-sm" onclick="removeOption(this, 'edit')" style="background: var(--red); color: white; border: none;">
                <i class="fa-solid fa-trash"></i>
            </button>
        `;
        container.appendChild(div);
    });

    // Update Dropdown AFTER populating inputs
    updateCorrectDropdown('edit');

    // Determine Correct Answer
    let correctVal = '';
    // Check which one is marked true in correct_answers
    for (const key of keys) {
        if (q.correct_answers[`${key}_correct`] === "true") {
            correctVal = key;
            break;
        }
    }

    // If we re-indexed (e.g. data had answer_a and answer_c but not b), 
    // the keys loop above creates index 0, 1. The dropdown values are answer_a, answer_b.
    // So we need to map the original key to the new key if we want to be strict, 
    // BUT usually data is consistent. If specific key (answer_c) was correct, and it is now the 2nd item, it becomes answer_b in our new simplified logical list.
    // For specific requirement "add as many options", simpler is:
    // We generated inputs for index 0,1,2... corresponding to the SORTED keys found. 
    // So if answer_c was correct and it is the 3rd key, we select 'answer_c'.

    if (correctVal) {
        document.getElementById('edit-q-correct').value = correctVal;
    }

    document.getElementById('modal-edit-question').classList.add('active');
}

async function loadDashboardStats() {
    const totalQEl = document.getElementById('stat-total-questions');
    if (!totalQEl) return;

    try {
        const [qSnap, uSnap] = await Promise.all([
            db.collection('questions').get(),
            db.collection('users').get()
        ]);

        totalQEl.innerText = qSnap.size.toLocaleString();

        const totalUEl = document.getElementById('stat-total-users');
        if (totalUEl) totalUEl.innerText = uSnap.size.toLocaleString();

    } catch (e) {
        console.error("Error loading stats:", e);
        totalQEl.innerText = 'Error';
    }
}

// Submit Edit
document.getElementById('form-edit-question').addEventListener('submit', async (e) => {
    e.preventDefault();

    const levelInput = document.getElementById('edit-q-level').value;
    if (levelInput) {
        const validation = validateLevel(levelInput);
        if (!validation.valid) {
            showMessage('error', 'Validation Error', validation.message);
            return;
        }
    }

    const id = document.getElementById('edit-q-id').value;
    const category = document.getElementById('edit-q-category').value;
    const difficulty = document.getElementById('edit-q-difficulty').value;
    const level = document.getElementById('edit-q-level').value;
    const questionText = document.getElementById('edit-q-text').value;
    const explanation = document.getElementById('edit-q-explanation').value;

    const correctVal = document.getElementById('edit-q-correct').value;

    // Gather Options Dynamically
    const answers = {};
    const correct_answers = {};

    const optionsContainer = document.getElementById('edit-options-container');
    Array.from(optionsContainer.children).forEach((row, idx) => {
        const char = getOptionChar(idx); // a, b, c...
        const key = `answer_${char}`;
        const val = row.querySelector('input').value;
        answers[key] = val;

        correct_answers[`${key}_correct`] = (key === correctVal) ? "true" : "false";
    });

    const questionData = {
        category: category,
        difficulty: difficulty,
        level: level ? parseInt(level) : null,
        question: questionText,
        explanation: explanation,
        answers: answers,
        correct_answers: correct_answers
    };

    try {
        await db.collection('questions').doc(id).update(questionData);
        showMessage('success', 'Updated', 'Question updated successfully!');
        document.getElementById('modal-edit-question').classList.remove('active');
        loadQuestionsV2(selectedCategory);
    } catch (error) {
        console.error(error);
        showMessage('error', 'Error', error.message);
    }
});

function getCorrectAnswerText(q) {
    if (q.correct_answers.answer_a_correct === "true") return "A: " + q.answers.answer_a;
    if (q.correct_answers.answer_b_correct === "true") return "B: " + q.answers.answer_b;
    if (q.correct_answers.answer_c_correct === "true") return "C: " + q.answers.answer_c;
    if (q.correct_answers.answer_d_correct === "true") return "D: " + q.answers.answer_d;
    return "Unknown";
}

// End of Utils


window.deleteQuestion = async function (id) {
    if (!confirm("Are you sure you want to delete this question?")) return;

    try {
        await db.collection('questions').doc(id).delete();
        showMessage('success', 'Deleted', 'Question deleted.');
        loadQuestions(selectedCategory);
    } catch (e) {
        console.error(e);
        showMessage('error', 'Error', e.message);
    }
}

// Utils
function getInitials(name) {
    if (!name) return '??';
    return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
}

function getRandomColor() {
    const colors = ['#EF4444', '#F59E0B', '#10B981', '#3B82F6', '#6366F1', '#EC4899'];
    return colors[Math.floor(Math.random() * colors.length)];
}

// Delete Single Transaction
window.deleteUserTransaction = async function (userId, txId) {
    if (!confirm('Are you sure you want to delete this transaction record?')) return;

    try {
        await db.collection('users').doc(userId).collection('TransactionHistory').doc(txId).delete();
        // UI updates automatically via listener
    } catch (e) {
        alert('Error deleting transaction: ' + e.message);
    }
}

// Delete All Transactions
window.deleteAllUserTransactions = async function () {
    const userId = window.currentEditingUserId;
    if (!userId) return;
    if (!confirm('WARNING: using this will PERMANENTLY DELETE ALL transaction logs for this user.\n\nAre you sure?')) return;

    const container = document.getElementById('edit-user-transactions');
    const originalContent = container.innerHTML;
    container.innerHTML = '<div style="padding:40px; text-align:center;"><i class="fa-solid fa-spinner fa-spin"></i> Deleting...</div>';

    try {
        const batchSize = 500;
        const ref = db.collection('users').doc(userId).collection('TransactionHistory');
        const snapshot = await ref.limit(batchSize).get();

        if (snapshot.empty) {
            alert('No transactions to delete.');
            container.innerHTML = originalContent;
            return;
        }

        const batch = db.batch();
        snapshot.docs.forEach(doc => batch.delete(doc.ref));
        await batch.commit();

        alert('Batch deletion complete. If there are more than 500, repeat this action.');

    } catch (e) {
        alert('Error deleting history: ' + e.message);
        loadUserTransactions(userId); // Restore
    }
}

// Reset Account
window.resetUserAccount = async function () {
    const userId = window.currentEditingUserId;
    if (!userId) return;

    if (!confirm("DANGER ZONE: This will perform a FULL RESET of this user's account.\n\n1. Wallet Balance set to 0.\n2. ALL Transaction History deleted.\n\nThis action cannot be undone. Proceed?")) return;

    try {
        // 1. Reset Balance
        await db.collection('users').doc(userId).update({
            mintBalance: 0,
            updatedAt: Date.now()
        });

        // 2. Clear History (Reuse logic or do it manually)
        const ref = db.collection('users').doc(userId).collection('TransactionHistory');
        const snapshot = await ref.limit(500).get();

        if (!snapshot.empty) {
            const batch = db.batch();
            snapshot.docs.forEach(doc => batch.delete(doc.ref));
            await batch.commit();
        }

        alert('Account Reset Successfully.');
        document.getElementById('edit-user-balance').value = 0;
        document.getElementById('modal-edit-user').classList.remove('active');
        loadUsers(); // Refresh Grid

    } catch (e) {
        alert('Error resetting account: ' + e.message);
    }
}

// --- New Level Filtering Logic V2 ---
let allQuestionsCache = [];
let currentFilterLevel = 'All';

async function loadQuestionsV2(category) {
    const container = document.getElementById('questions-list-full');
    if (!container) return;

    if (!category) {
        container.innerHTML = '<div style="text-align:center; padding: 2rem; color:var(--text-muted);"><i class="fa-solid fa-arrow-left"></i> Select a category</div>';
        return;
    }

    container.innerHTML = 'Loading questions...';
    questionMap = {}; // Reset
    allQuestionsCache = []; // Reset Cache

    try {
        const snapshot = await db.collection('questions')
            .where('category', '==', category)
            .orderBy('createdAt', 'desc')
            .get();

        if (snapshot.empty) {
            container.innerHTML = '<div style="padding:2rem; text-align:center; color:var(--text-muted)">No questions in this category.</div>';
            populateLevelDropdown([]);
            return;
        }

        snapshot.forEach(doc => {
            const q = doc.data();
            q.id = doc.id;
            questionMap[doc.id] = q;
            allQuestionsCache.push(q);
        });

        populateLevelDropdown(allQuestionsCache);
        renderQuestions();

    } catch (e) {
        console.error(e);
        // Fallback for missing index
        if (e.code === 'failed-precondition') {
            container.innerHTML = 'Index missing. Loading without sort...';
            try {
                const fallbackSnap = await db.collection('questions').where('category', '==', category).get();
                fallbackSnap.forEach(doc => {
                    const q = doc.data();
                    q.id = doc.id;
                    questionMap[doc.id] = q;
                    allQuestionsCache.push(q);
                });
                populateLevelDropdown(allQuestionsCache);
                renderQuestions();
            } catch (e2) {
                container.innerHTML = 'Error loading questions.';
            }
        } else {
            container.innerHTML = 'Error loading questions.';
        }
    }
}

function populateLevelDropdown(questions) {
    const uniqueLevels = new Set();
    questions.forEach(q => {
        if (q.level) uniqueLevels.add(parseInt(q.level));
    });

    // Sort Levels Numeric
    const sortedLevels = Array.from(uniqueLevels).sort((a, b) => a - b);

    const filterSelect = document.getElementById('filter-level');
    if (sortedLevels.length > 0) {
        filterSelect.style.display = 'block';
        filterSelect.innerHTML = '<option value="All">All Levels</option>' +
            sortedLevels.map(l => `<option value="${l}">Level ${l}</option>`).join('');
        filterSelect.value = currentFilterLevel;

        // Validation: If selected level gone (e.g. deleted), reset
        if (currentFilterLevel !== 'All' && !uniqueLevels.has(parseInt(currentFilterLevel))) {
            filterSelect.value = 'All';
            currentFilterLevel = 'All';
        }
    } else {
        filterSelect.style.display = 'none';
        currentFilterLevel = 'All';
    }

    const datalistHTML = sortedLevels.map(l => `<option value="${l}">`).join('');
    const addDataList = document.getElementById('level-options');
    const editDataList = document.getElementById('level-options-edit');
    if (addDataList) addDataList.innerHTML = datalistHTML;
    if (editDataList) editDataList.innerHTML = datalistHTML;
}

// Helper: Validate Level Sequence
function validateLevel(inputLevel) {
    const level = parseInt(inputLevel);
    if (isNaN(level) || level <= 0) {
        return { valid: false, message: "Level must be a positive number." };
    }

    // Get existing unique levels from current cache
    const existingLevels = new Set();
    allQuestionsCache.forEach(q => {
        if (q.level) existingLevels.add(parseInt(q.level));
    });

    if (existingLevels.has(level)) {
        return { valid: true }; // Valid: Adding to existing level
    }

    // Check Sequence
    const maxLevel = existingLevels.size > 0 ? Math.max(...existingLevels) : 0;
    if (level === maxLevel + 1) {
        return { valid: true }; // Valid: Next sequential level
    }

    return {
        valid: false,
        message: `Invalid Level Sequence. Current Max Level is ${maxLevel}. You can only add Level ${maxLevel + 1} next.`
    };
}

window.filterQuestionsByLevel = function () {
    const filterSelect = document.getElementById('filter-level');
    currentFilterLevel = filterSelect.value;
    renderQuestions();
}

function renderQuestions() {
    const container = document.getElementById('questions-list-full');
    let html = '';
    let index = 1;
    let visibleCount = 0;

    allQuestionsCache.forEach(q => {
        if (currentDifficulty && q.difficulty !== currentDifficulty) return;
        if (currentFilterLevel !== 'All' && q.level != currentFilterLevel) return;

        visibleCount++;

        html += `
        <div class="question-card" onclick="openEditQuestion('${q.id}')" style="cursor: pointer;">
            <div class="q-number">${index++}</div>
            <div class="q-content">
                <div class="q-text">${q.question || 'No Text'}</div>
                ${q.explanation ? `<div style="font-size:0.8rem; color:var(--primary); margin-top:4px;"><em>💡 ${q.explanation}</em></div>` : ''}
                <div class="q-meta">
                    <span><i class="fa-solid fa-layer-group"></i> ${q.difficulty}</span>
                    ${q.level ? `<span><i class="fa-solid fa-star"></i> Level ${q.level}</span>` : ''}
                    <span><i class="fa-solid fa-check"></i> ${getCorrectAnswerText(q)}</span>
                    <span style="color:var(--text-muted); font-size: 0.8em; margin-left: auto;">${formatDate(q.createdAt)}</span>
                </div>
            </div>
            <div class="q-actions">
                <button class="btn-sm" onclick="event.stopPropagation(); deleteQuestion('${q.id}')" style="color:var(--red); background:rgba(248,113,113,0.1)">
                    <i class="fa-solid fa-trash"></i>
                </button>
            </div>
        </div>`;
    });

    if (visibleCount === 0) {
        container.innerHTML = `<div style="padding:2rem; text-align:center; color:var(--text-muted)">No questions found match filters.</div>`;
    } else {
        container.innerHTML = html;
    }
}


// --- Dynamic Options Logic ---

window.addOption = function (mode) { // mode: 'add' or 'edit'
    const containerId = mode === 'add' ? 'add-options-container' : 'edit-options-container';
    const container = document.getElementById(containerId);

    // Determine new index
    const currentCount = container.children.length;
    const char = getOptionChar(currentCount); // a, b, c...

    const div = document.createElement('div');
    div.className = 'form-row option-row';
    div.dataset.index = currentCount;

    div.innerHTML = `
        <input type="text" placeholder="Option ${char.toUpperCase()}" required>
        <button type="button" class="btn-sm" onclick="removeOption(this, '${mode}')" style="background: var(--red); color: white; border: none;">
            <i class="fa-solid fa-trash"></i>
        </button>
    `;

    container.appendChild(div);
    updateCorrectDropdown(mode);
}

window.removeOption = function (btn, mode) {
    const row = btn.parentElement;
    const container = row.parentElement;
    container.removeChild(row);

    // Re-index remaining options (optional but good for clean A, B, C labels placeholder)
    Array.from(container.children).forEach((child, idx) => {
        child.dataset.index = idx;
        const char = getOptionChar(idx).toUpperCase();
        const input = child.querySelector('input');
        input.placeholder = `Option ${char}`;
    });

    updateCorrectDropdown(mode);
}

function updateCorrectDropdown(mode) {
    const containerId = mode === 'add' ? 'add-options-container' : 'edit-options-container';
    const selectId = mode === 'add' ? 'q-correct' : 'edit-q-correct';

    const container = document.getElementById(containerId);
    const select = document.getElementById(selectId);

    const currentVal = select.value;

    // Clear
    select.innerHTML = '';

    Array.from(container.children).forEach((child, idx) => {
        const char = getOptionChar(idx); // a, b, c
        const val = `answer_${char}`;
        const inputVal = child.querySelector('input').value;
        const label = inputVal ? `${char.toUpperCase()}: ${inputVal.substring(0, 15)}${inputVal.length > 15 ? '...' : ''}` : `Option ${char.toUpperCase()}`;

        const option = document.createElement('option');
        option.value = val;
        option.innerText = label;
        select.appendChild(option);
    });

    // Restore selection if valid
    if (currentVal) {
        // Check if currentVal still exists in new options
        let exists = false;
        Array.from(select.options).forEach(opt => {
            if (opt.value === currentVal) exists = true;
        });
        if (exists) select.value = currentVal;
    }
}

// Hook up input comparison for realtime dropdown update (optional polish)
function setupOptionInputListeners(mode) {
    const containerId = mode === 'add' ? 'add-options-container' : 'edit-options-container';
    document.getElementById(containerId).addEventListener('input', () => updateCorrectDropdown(mode));
}

// Helper: 0 -> a, 1 -> b...
function getOptionChar(index) {
    return String.fromCharCode(97 + index); // 97 is 'a'
}

// Init Listeners
document.addEventListener('DOMContentLoaded', () => {
    // ... existing setup ...
    setupOptionInputListeners('add');
    setupOptionInputListeners('edit');
});
