// ==========================
// REWARDS MANAGEMENT
// ==========================

// Initialize when rewards view is shown
document.addEventListener('DOMContentLoaded', () => {
    // Listen for navigation clicks to load rewards data
    const rewardsNavItem = document.querySelector('[data-tab="rewards"]');
    if (rewardsNavItem) {
        rewardsNavItem.addEventListener('click', () => {
            loadRewards();
            loadRedemptions('pending');
        });
    }
});

// Load all rewards from Firestore
async function loadRewards() {
    const rewardsList = document.getElementById('rewards-list');
    const rewardsCount = document.getElementById('rewards-count');

    try {
        const snapshot = await firebase.firestore().collection('rewards').get();

        rewardsCount.textContent = `${snapshot.size} Rewards`;

        if (snapshot.empty) {
            rewardsList.innerHTML = `
                <div style="text-align: center; padding: 3rem; color: var(--text-muted);">
                    <i class="fa-solid fa-gift" style="font-size: 3rem; opacity: 0.3;"></i>
                    <p style="margin-top: 1rem;">No rewards yet. Click "Add Reward" to create one!</p>
                </div>
            `;
            return;
        }

        let html = '<div style="display: grid; gap: 12px;">';

        snapshot.docs.forEach(doc => {
            const reward = doc.data();
            reward.id = doc.id;

            html += `
                <div class="list-item" style="border: 1px solid var(--border); padding: 12px; border-radius: 8px;">
                    <div style="display: flex; align-items: center; gap: 12px;">
                        <img src="${reward.logoUrl || '/placeholder.png'}" 
                             style="width: 48px; height: 48px; border-radius: 8px; object-fit: contain; background: ${reward.colorHex}; padding: 8px;"
                             onerror="this.src='data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%2248%22 height=%2248%22%3E%3Crect fill=%22%23ddd%22 width=%2248%22 height=%2248%22/%3E%3C/svg%3E'">
                        <div style="flex: 1;">
                            <h4 style="margin: 0; font-size: 1rem;">${reward.name}</h4>
                            <p style="margin: 4px 0 0 0; font-size: 0.85rem; color: var(--text-muted);">${reward.brand} • ${reward.price.toLocaleString()} Mints</p>
                        </div>
                        <span class="status-pill ${reward.isActive !== false ? 'active' : 'inactive'}">${reward.isActive !== false ? 'Active' : 'Inactive'}</span>
                        <div style="display: flex; gap: 6px;">
                            <button class="btn-sm" onclick="editReward('${reward.id}')" title="Edit">
                                <i class="fa-solid fa-edit"></i>
                            </button>
                            <button class="btn-sm" onclick="toggleRewardStatus('${reward.id}', ${reward.isActive !== false})" title="${reward.isActive !== false ? 'Deactivate' : 'Activate'}" 
                                    style="background: ${reward.isActive !== false ? 'var(--red)' : 'var(--green)'};">
                                <i class="fa-solid fa-${reward.isActive !== false ? 'eye-slash' : 'eye'}"></i>
                            </button>
                        </div>
                    </div>
                </div>
            `;
        });

        html += '</div>';
        rewardsList.innerHTML = html;

    } catch (error) {
        console.error('Error loading rewards:', error);
        showMessage('Error', 'Failed to load rewards: ' + error.message, 'error');
    }
}

// Open add/edit reward modal
function openAddRewardModal(rewardId = null) {
    const modal = document.getElementById('modal-add-reward');
    const form = document.getElementById('form-add-reward');
    const title = document.getElementById('reward-modal-title');

    form.reset();
    document.getElementById('reward-id').value = '';

    if (rewardId) {
        title.textContent = 'Edit Reward';
        // Load reward data
        firebase.firestore().collection('rewards').doc(rewardId).get()
            .then(doc => {
                if (doc.exists) {
                    const reward = doc.data();
                    document.getElementById('reward-id').value = rewardId;
                    document.getElementById('reward-name').value = reward.name || '';
                    document.getElementById('reward-brand').value = reward.brand || '';
                    document.getElementById('reward-price').value = reward.price || '';
                    document.getElementById('reward-logo-url').value = reward.logoUrl || '';
                    document.getElementById('reward-color').value = reward.colorHex || '#000000';
                    document.getElementById('reward-redemption-steps').value = reward.redemptionSteps || '';
                    document.getElementById('reward-instructions').value = reward.instructions || '';
                    document.getElementById('reward-timeline').value = reward.verificationTimeline || '';
                    document.getElementById('reward-is-active').checked = reward.isActive !== false;
                }
            });
    } else {
        title.textContent = 'Add New Reward';
    }

    modal.classList.add('active');
}

function editReward(rewardId) {
    openAddRewardModal(rewardId);
}

// Handle add/edit reward form submission
document.getElementById('form-add-reward')?.addEventListener('submit', async (e) => {
    e.preventDefault();

    const rewardId = document.getElementById('reward-id').value;
    const rewardData = {
        name: document.getElementById('reward-name').value,
        brand: document.getElementById('reward-brand').value,
        price: parseInt(document.getElementById('reward-price').value),
        logoUrl: document.getElementById('reward-logo-url').value,
        colorHex: document.getElementById('reward-color').value,
        redemptionSteps: document.getElementById('reward-redemption-steps').value || '1. Click Redeem\n2. Confirm your choice\n3. Copy the code',
        instructions: document.getElementById('reward-instructions').value || 'Use this code at checkout',
        verificationTimeline: document.getElementById('reward-timeline').value || 'Instant',
        isActive: document.getElementById('reward-is-active').checked,
        updatedAt: firebase.firestore.Timestamp.now()
    };

    try {
        if (rewardId) {
            // Update existing
            await firebase.firestore().collection('rewards').doc(rewardId).update(rewardData);
            showMessage('Success', 'Reward updated successfully!', 'success');
        } else {
            // Create new
            rewardData.createdAt = firebase.firestore.Timestamp.now();
            await firebase.firestore().collection('rewards').add(rewardData);
            showMessage('Success', 'Reward created successfully!', 'success');
        }

        document.getElementById('modal-add-reward').classList.remove('active');
        loadRewards();

    } catch (error) {
        console.error('Error saving reward:', error);
        showMessage('Error', 'Failed to save reward: ' + error.message, 'error');
    }
});

// Toggle reward active status
async function toggleRewardStatus(rewardId, currentStatus) {
    const action = currentStatus ? 'deactivate' : 'activate';
    if (!confirm(`Are you sure you want to ${action} this reward?`)) return;

    try {
        await firebase.firestore().collection('rewards').doc(rewardId).update({
            isActive: !currentStatus,
            updatedAt: firebase.firestore.Timestamp.now()
        });

        showMessage('Success', `Reward ${action}d successfully!`, 'success');
        loadRewards();

    } catch (error) {
        console.error('Error toggling reward status:', error);
        showMessage('Error', 'Failed to update reward status: ' + error.message, 'error');
    }
}

// ==========================
// REDEMPTION MANAGEMENT
// ==========================

// Load redemption requests
async function loadRedemptions(statusFilter = 'pending') {
    const redemptionsList = document.getElementById('redemptions-list');

    try {
        let snapshot;

        // Try with index first
        try {
            let query = firebase.firestore().collection('redemptions');

            if (statusFilter !== 'all') {
                query = query.where('status', '==', statusFilter);
            }

            snapshot = await query.orderBy('requestedAt', 'desc').limit(50).get();

        } catch (indexError) {
            // Fallback: Fetch all and filter/sort client-side
            console.warn('Index not found, using client-side filter:', indexError);

            const allSnapshot = await firebase.firestore().collection('redemptions').limit(100).get();

            // Filter and sort client-side
            let docs = [];
            allSnapshot.forEach(doc => {
                const data = doc.data();
                if (statusFilter === 'all' || data.status === statusFilter) {
                    docs.push({ id: doc.id, data: data, exists: true });
                }
            });

            // Sort by requestedAt descending
            docs.sort((a, b) => {
                const timeA = a.data.requestedAt?.toMillis() || 0;
                const timeB = b.data.requestedAt?.toMillis() || 0;
                return timeB - timeA;
            });

            // Create a mock snapshot object
            snapshot = {
                empty: docs.length === 0,
                docs: docs.slice(0, 50).map(d => ({
                    id: d.id,
                    data: () => d.data,
                    exists: d.exists
                }))
            };
        }

        if (snapshot.empty) {
            redemptionsList.innerHTML = `
                <div style="text-align: center; padding: 2rem; color: var(--text-muted);">
                    <i class="fa-solid fa-inbox" style="font-size: 2rem; opacity: 0.3;"></i>
                    <p style="margin-top: 1rem;">No ${statusFilter === 'all' ? '' : statusFilter} redemption requests</p>
                </div>
            `;
            return;
        }

        let html = '';

        snapshot.docs.forEach(doc => {
            const redemption = doc.data();
            redemption.id = doc.id;

            const timeAgo = getTimeAgo(redemption.requestedAt?.toDate());
            const statusClass = redemption.status === 'pending' ? 'pending' : redemption.status === 'approved' ? 'active' : 'inactive';

            html += `
                <div class="list-item" style="margin-bottom: 12px; border: 1px solid var(--border); padding: 12px; border-radius: 8px;">
                    <div style="display: flex; gap: 12px; align-items: start;">
                        <div class="item-icon" style="background: var(--primary);">
                            <i class="fa-solid fa-gift"></i>
                        </div>
                        <div style="flex: 1;">
                            <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 8px;">
                                <div>
                                    <h4 style="margin: 0; font-size: 0.95rem;">${redemption.rewardName}</h4>
                                    <p style="margin: 4px 0; font-size: 0.85rem; color: var(--text-muted);">
                                        <strong>${redemption.userName}</strong> • ${redemption.userPhone}
                                    </p>
                                </div>
                                <span class="status-pill ${statusClass}" style="text-transform: capitalize;">${redemption.status}</span>
                            </div>
                            <div style="display: flex; gap: 16px; font-size: 0.85rem; color: var(--text-muted); margin-bottom: 8px;">
                                <span><i class="fa-solid fa-coins"></i> ${redemption.rewardPrice.toLocaleString()} Mints</span>
                                <span><i class="fa-solid fa-clock"></i> ${timeAgo}</span>
                            </div>
                            ${redemption.status === 'pending' ? `
                            <button class="btn-sm btn-primary" onclick="openProcessRedemptionModal('${redemption.id}')">
                                <i class="fa-solid fa-check"></i> Process Request
                            </button>
                            ` : redemption.status === 'approved' ? `
                            <div style="background: var(--bg-dark); padding: 8px; border-radius: 6px; margin-top: 8px;">
                                <small style="color: var(--text-muted);">Code:</small> 
                                <strong style="color: var(--primary); font-family: monospace;">${redemption.redemptionCode || 'N/A'}</strong>
                            </div>
                            ` : `
                            <div style="background: rgba(248, 113, 113, 0.1); padding: 8px; border-radius: 6px; margin-top: 8px;">
                                <small style="color: var(--red);">Reason:</small> 
                                <span style="font-size: 0.85rem;">${redemption.adminNotes || 'No reason provided'}</span>
                            </div>
                            `}
                        </div>
                    </div>
                </div>
            `;
        });

        redemptionsList.innerHTML = html;

    } catch (error) {
        console.error('Error loading redemptions:', error);
        redemptionsList.innerHTML = `
            <div style="text-align: center; padding: 2rem; color: var(--red);">
                <i class="fa-solid fa-exclamation-triangle"></i>
                <p>Error loading redemptions: ${error.message}</p>
            </div>
        `;
    }
}

// Open process redemption modal
async function openProcessRedemptionModal(redemptionId) {
    const modal = document.getElementById('modal-process-redemption');
    const detailsDiv = document.getElementById('redemption-details');
    const form = document.getElementById('form-process-redemption');

    form.reset();
    document.getElementById('approval-fields').style.display = 'none';
    document.getElementById('rejection-fields').style.display = 'none';

    try {
        const doc = await firebase.firestore().collection('redemptions').doc(redemptionId).get();

        if (!doc.exists) {
            showMessage('Error', 'Redemption not found', 'error');
            return;
        }

        const redemption = doc.data();
        const timeAgo = getTimeAgo(redemption.requestedAt?.toDate());

        document.getElementById('process-redemption-id').value = redemptionId;
        document.getElementById('process-user-id').value = redemption.userId;
        document.getElementById('process-reward-price').value = redemption.rewardPrice;

        detailsDiv.innerHTML = `
            <div style="display: grid; gap: 8px;">
                <div>
                    <small style="color: var(--text-muted);">Reward:</small><br>
                    <strong>${redemption.rewardName}</strong>
                </div>
                <div style="display: flex; gap: 16px;">
                    <div>
                        <small style="color: var(--text-muted);">User:</small><br>
                        ${redemption.userName}
                    </div>
                    <div>
                        <small style="color: var(--text-muted);">Phone:</small><br>
                        ${redemption.userPhone}
                    </div>
                </div>
                <div>
                    <small style="color: var(--text-muted);">Price:</small><br>
                    <strong style="color: var(--gold);">${redemption.rewardPrice.toLocaleString()} Mints</strong>
                </div>
                <div>
                    <small style="color: var(--text-muted);">Requested:</small><br>
                    ${timeAgo}
                </div>
            </div>
        `;

        modal.classList.add('active');

    } catch (error) {
        console.error('Error loading redemption:', error);
        showMessage('Error', 'Failed to load redemption details', 'error');
    }
}

// Toggle redemption action fields
function toggleRedemptionFields() {
    const action = document.getElementById('redemption-action').value;
    document.getElementById('approval-fields').style.display = action === 'approve' ? 'block' : 'none';
    document.getElementById('rejection-fields').style.display = action === 'reject' ? 'block' : 'none';
}

// Process redemption request
document.getElementById('form-process-redemption')?.addEventListener('submit', async (e) => {
    e.preventDefault();

    const redemptionId = document.getElementById('process-redemption-id').value;
    const userId = document.getElementById('process-user-id').value;
    const rewardPrice = parseInt(document.getElementById('process-reward-price').value);
    const action = document.getElementById('redemption-action').value;

    if (!action) {
        alert('Please select an action');
        return;
    }

    try {
        const updateData = {
            status: action === 'approve' ? 'approved' : 'rejected',
            processedAt: firebase.firestore.Timestamp.now(),
            processedBy: 'admin', // You can add actual admin ID if you have auth
            adminNotes: document.getElementById('admin-notes').value || null
        };

        if (action === 'approve') {
            const code = document.getElementById('redemption-code').value;
            if (!code || !code.trim()) {
                alert('Please enter a redemption code');
                return;
            }
            updateData.redemptionCode = code.trim();

            // Deduct balance from user using transaction
            const userRef = firebase.firestore().collection('users').doc(userId);

            await firebase.firestore().runTransaction(async (transaction) => {
                const userDoc = await transaction.get(userRef);
                if (!userDoc.exists) {
                    throw new Error('User not found');
                }

                const userData = userDoc.data();
                // Check both 'balance' and 'mintBalance' fields
                const balanceField = userData.balance !== undefined ? 'balance' : 'mintBalance';
                const currentBalance = userData[balanceField] || 0;

                console.log(`Current ${balanceField}: ${currentBalance}, Reward Price: ${rewardPrice}`);

                if (currentBalance < rewardPrice) {
                    throw new Error(`User has insufficient balance (${currentBalance} < ${rewardPrice})`);
                }

                const newBalance = currentBalance - rewardPrice;

                // Update redemption status
                transaction.update(firebase.firestore().collection('redemptions').doc(redemptionId), updateData);

                // Deduct balance using the correct field
                const balanceUpdate = {};
                balanceUpdate[balanceField] = newBalance;
                balanceUpdate.updatedAt = Date.now();
                transaction.update(userRef, balanceUpdate);

                // Add transaction history
                const txRef = firebase.firestore().collection('users').doc(userId).collection('TransactionHistory').doc();
                transaction.set(txRef, {
                    id: txRef.id,
                    amount: rewardPrice,
                    type: 'debit',
                    title: 'Reward Redemption',
                    description: `Redeemed: ${document.getElementById('redemption-details').querySelector('strong').textContent}`,
                    status: 'completed',
                    timestamp: Date.now()
                });

                console.log(`Balance deducted: ${currentBalance} -> ${newBalance}`);
            });

            showMessage('success', 'Redemption Approved', `Balance deducted successfully! Code: ${code}`);

        } else {
            const reason = document.getElementById('rejection-reason').value;
            if (!reason) {
                alert('Please provide a reason for rejection');
                return;
            }
            updateData.adminNotes = reason;

            await firebase.firestore().collection('redemptions').doc(redemptionId).update(updateData);
            showMessage('Success', 'Redemption rejected', 'success');
        }

        document.getElementById('modal-process-redemption').classList.remove('active');
        loadRedemptions(document.getElementById('filter-redemption-status').value);

    } catch (error) {
        console.error('Error processing redemption:', error);
        showMessage('Error', 'Failed to process redemption: ' + error.message, 'error');
    }
});

// Redemption status filter change
document.getElementById('filter-redemption-status')?.addEventListener('change', (e) => {
    loadRedemptions(e.target.value);
});

// Add reward button
document.getElementById('btn-add-reward')?.addEventListener('click', () => {
    openAddRewardModal();
});

// Helper function to get time ago
function getTimeAgo(date) {
    if (!date) return 'Unknown';
    const seconds = Math.floor((new Date() - date) / 1000);

    if (seconds < 60) return 'Just now';
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    if (seconds < 604800) return `${Math.floor(seconds / 86400)}d ago`;

    return date.toLocaleDateString();
}
