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

    // Modals
    setupModals();

    // NOTE: For this Admin Panel to work without Login, you MUST set your Firestore Rules to:
    // allow read, write: if true;
    console.log("Admin Panel: Initializing data...");

    // Load Initial Data
    loadUsers();
    loadCategories();
    loadQuestions();
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

        const questionData = {
            category: document.getElementById('q-category').value,
            difficulty: document.getElementById('q-difficulty').value,
            question: document.getElementById('q-text').value,
            answers: {
                answer_a: document.getElementById('q-opt-a').value,
                answer_b: document.getElementById('q-opt-b').value,
                answer_c: document.getElementById('q-opt-c').value,
                answer_d: document.getElementById('q-opt-d').value
            },
            correct_answers: {
                answer_a_correct: document.getElementById('q-correct').value === 'answer_a' ? "true" : "false",
                answer_b_correct: document.getElementById('q-correct').value === 'answer_b' ? "true" : "false",
                answer_c_correct: document.getElementById('q-correct').value === 'answer_c' ? "true" : "false",
                answer_d_correct: document.getElementById('q-correct').value === 'answer_d' ? "true" : "false",
            },
            createdAt: firebase.firestore.FieldValue.serverTimestamp()
        };

        try {
            await db.collection('questions').add(questionData);
            alert('Question Added!');
            document.getElementById('modal-add-question').classList.remove('active');
            e.target.reset();
            loadQuestions();
        } catch (error) {
            console.error(error);
            alert('Error adding question: ' + error.message);
        }
    });
}

async function loadUsers() {
    const tableBody = document.getElementById('users-table-body');
    if (!tableBody) return;
    tableBody.innerHTML = '<tr><td colspan="7">Loading...</td></tr>';

    try {
        const snapshot = await db.collection('users').limit(20).get();

        let html = '';
        snapshot.forEach(doc => {
            const user = doc.data();
            const initials = user.name ? getInitials(user.name) : '?';
            const color = getRandomColor();

            html += `
            <tr>
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
                <td><span class="status-pill active">Active</span></td>
                <td>
                    <button class="btn-sm" style="color: var(--text-muted);"><i class="fa-solid fa-ellipsis-vertical"></i></button>
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

// Render Categories with Delete Option
// State
let selectedCategory = null;

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

        let html = '';
        snapshot.forEach(doc => {
            const cat = doc.data();
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
        }
    }
}

window.selectCategory = function (name) {
    selectedCategory = name;
    document.getElementById('selected-category-title').innerText = name;
    document.getElementById('btn-add-question').style.display = 'block';

    // Refresh UI highlights
    loadCategories();
    // Load questions for this category
    loadQuestions(name);
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
        if (selectedCategory) loadQuestions(selectedCategory);

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
        let index = snapshot.size; // Reverse numbering since we ordered desc (newest first)

        // Or if user wants 1 to N, we should sort asc or just map index
        // Let's do 1..N based on display order. 
        // If query is desc, top item is newly added. Let's start from 1.
        index = 1;

        snapshot.forEach(doc => {
            const q = doc.data();
            html += `
            <div class="question-card">
                <div class="q-number">${index++}</div>
                <div class="q-content">
                    <div class="q-text">${q.question || 'No Text'}</div>
                    <div class="q-meta">
                        <span><i class="fa-solid fa-layer-group"></i> ${q.difficulty}</span>
                        <span><i class="fa-solid fa-check"></i> ${getCorrectAnswerText(q)}</span>
                    </div>
                </div>
                <div class="q-actions">
                    <button class="btn-sm" onclick="deleteQuestion('${doc.id}')" style="color:var(--red); background:rgba(248,113,113,0.1)">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                    <!-- Future: Edit Button -->
                </div>
            </div>`;
        });
        container.innerHTML = html;

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
                html += `<div class="question-card"><div class="q-number">${index++}</div><div class="q-content"><div class="q-text">${q.question}</div></div><div class="q-actions"><button class="btn-sm" onclick="deleteQuestion('${doc.id}')" style="color:var(--red);"><i class="fa-solid fa-trash"></i></button></div></div>`;
            });
            container.innerHTML = html;
        } else {
            container.innerHTML = 'Error loading questions.';
        }
    }
}

function getCorrectAnswerText(q) {
    if (q.correct_answers.answer_a_correct === "true") return "A: " + q.answers.answer_a;
    if (q.correct_answers.answer_b_correct === "true") return "B: " + q.answers.answer_b;
    if (q.correct_answers.answer_c_correct === "true") return "C: " + q.answers.answer_c;
    if (q.correct_answers.answer_d_correct === "true") return "D: " + q.answers.answer_d;
    return "Unknown";
}

// End of Utils


// Utils
function getInitials(name) {
    if (!name) return '??';
    return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
}

function getRandomColor() {
    const colors = ['#EF4444', '#F59E0B', '#10B981', '#3B82F6', '#6366F1', '#EC4899'];
    return colors[Math.floor(Math.random() * colors.length)];
}
