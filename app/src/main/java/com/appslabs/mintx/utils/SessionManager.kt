package com.appslabs.mintx.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "MintXSession"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_IS_PROFILE_COMPLETED = "isProfileCompleted"
        private const val KEY_MOBILE_NUMBER = "mobileNumber"
        private const val KEY_USER_NAME = "userName"
        private const val KEY_USER_AGE = "userAge"
        private const val KEY_IS_CATEGORIES_SELECTED = "isCategoriesSelected"
        private const val KEY_MINT_BALANCE = "mintBalance"
        private const val KEY_USER_EMAIL = "userEmail"
        private const val KEY_USER_PHOTO = "userPhoto"
    }

    fun createLoginSession(mobileNumber: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_MOBILE_NUMBER, mobileNumber)
            apply()
        }
    }

    fun createGoogleSession(email: String, photoUrl: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_PHOTO, photoUrl)
            apply()
        }
    }

    fun completeProfile(name: String, age: Int, email: String = "", photoUrl: String = "") {
        prefs.edit().apply {
            putBoolean(KEY_IS_PROFILE_COMPLETED, true)
            putString(KEY_USER_NAME, name)
            putInt(KEY_USER_AGE, age)
            if (email.isNotEmpty()) putString(KEY_USER_EMAIL, email)
            if (photoUrl.isNotEmpty()) putString(KEY_USER_PHOTO, photoUrl)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun isProfileCompleted(): Boolean = prefs.getBoolean(KEY_IS_PROFILE_COMPLETED, false)

    fun getUserMobile(): String? = prefs.getString(KEY_MOBILE_NUMBER, null)

    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, "")

    fun getUserPhoto(): String? = prefs.getString(KEY_USER_PHOTO, "")

    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, "User")

    fun getUserAge(): Int = prefs.getInt(KEY_USER_AGE, 0)

    fun setCategoriesSelected(isSelected: Boolean) {
        prefs.edit().putBoolean(KEY_IS_CATEGORIES_SELECTED, isSelected).apply()
    }

    fun isCategoriesSelected(): Boolean = prefs.getBoolean(KEY_IS_CATEGORIES_SELECTED, false)
    
    fun saveMintBalance(balance: Long) {
        prefs.edit().putLong(KEY_MINT_BALANCE, balance).apply()
    }

    fun getMintBalance(): Long = prefs.getLong(KEY_MINT_BALANCE, 0L)

    fun updateAge(age: Int) {
        prefs.edit().putInt(KEY_USER_AGE, age).apply()
    }

    fun logout() {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        prefs.edit().clear().apply()
    }
}

