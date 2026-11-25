package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.util.BillingHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Centralized Subscription & Premium Access Manager for LeafLens.
 *
 * Entitlement: leaflens pro
 * Product IDs:
 *   - leaflens_weekly (Weekly)
 *   - leaflens_monthly (Monthly)
 *   - leaflens_yearly (Yearly)
 */
object SubscriptionManager {
    private const val TAG = "SubscriptionManager"
    const val ENTITLEMENT_ID = "leaflens pro"
    const val PRODUCT_ID_WEEKLY = "leaflens_weekly"
    const val PRODUCT_ID_MONTHLY = "leaflens_monthly"
    const val PRODUCT_ID_YEARLY = "leaflens_yearly"

    const val MAX_FREE_DAILY_SCANS = 3
    const val FREE_TRIAL_DURATION_DAYS = 3
    private const val PREFS_NAME = "leaflens_subscription_prefs"
    private const val KEY_FREE_SCANS_COUNT_PREFIX = "free_scans_count_"
    private const val KEY_FREE_SCANS_DATE_PREFIX = "free_scans_date_"
    private const val KEY_TRIAL_START_TIME_PREFIX = "trial_start_time_"

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _isTrialActive = MutableStateFlow(false)
    val isTrialActive: StateFlow<Boolean> = _isTrialActive.asStateFlow()

    private val _trialDaysRemaining = MutableStateFlow(0)
    val trialDaysRemaining: StateFlow<Int> = _trialDaysRemaining.asStateFlow()

    private val _dailyFreeScansRemaining = MutableStateFlow(MAX_FREE_DAILY_SCANS)
    val dailyFreeScansRemaining: StateFlow<Int> = _dailyFreeScansRemaining.asStateFlow()

    private val _dailyFreeScansUsed = MutableStateFlow(0)
    val dailyFreeScansUsed: StateFlow<Int> = _dailyFreeScansUsed.asStateFlow()

    private val _hasFullAccess = MutableStateFlow(false)
    val hasFullAccess: StateFlow<Boolean> = _hasFullAccess.asStateFlow()

    private val _subscriptionStatus = MutableStateFlow("Free")
    val subscriptionStatus: StateFlow<String> = _subscriptionStatus.asStateFlow()

    private val _activePlanName = MutableStateFlow<String?>(null)
    val activePlanName: StateFlow<String?> = _activePlanName.asStateFlow()

    private val _activeProduct = MutableStateFlow<String?>(null)
    val activeProduct: StateFlow<String?> = _activeProduct.asStateFlow()

    private val _expirationDate = MutableStateFlow<String?>(null)
    val expirationDate: StateFlow<String?> = _expirationDate.asStateFlow()

    private val _offerings = MutableStateFlow<Offerings?>(null)
    val offerings: StateFlow<Offerings?> = _offerings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isBillingAvailable = MutableStateFlow(true)
    val isBillingAvailable: StateFlow<Boolean> = _isBillingAvailable.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun initialize(context: Context, revenueCatApiKey: String) {
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val billingSupported = BillingHelper.isBillingServiceAvailable(appContext)
        _isBillingAvailable.value = billingSupported
        if (!billingSupported) {
            Log.w(TAG, "Google Play Billing is not supported on this device/emulator.")
        }

        updateTrialState()
        updateDailyFreeScansState()

        if (revenueCatApiKey.isNotBlank() && billingSupported) {
            try {
                Purchases.configure(
                    PurchasesConfiguration.Builder(appContext, revenueCatApiKey)
                        .build()
                )
                Log.d(TAG, "RevenueCat configured successfully")

                Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
                    applyCustomerInfo(customerInfo)
                }

                refreshCustomerInfo()
                fetchOfferings()
            } catch (e: Exception) {
                Log.e(TAG, "RevenueCat initialization error", e)
            }
        } else {
            Log.w(TAG, "RevenueCat API key missing or empty or Billing unavailable")
        }
    }

    /**
     * Updates the free trial state by calculating from the persistent trial start time.
     */
    fun updateTrialState() {
        if (!::prefs.isInitialized) return
        val auth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
        val user = auth?.currentUser

        if (user == null) {
            _isTrialActive.value = false
            _trialDaysRemaining.value = 0
            _hasFullAccess.value = _isPremium.value
            return
        }

        val uid = user.uid
        val safeUid = uid.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val key = KEY_TRIAL_START_TIME_PREFIX + safeUid
        var trialStart = prefs.getLong(key, 0L)

        // Fallback: check Firebase User creation timestamp
        if (trialStart == 0L) {
            val creationTimestamp = user.metadata?.creationTimestamp ?: 0L
            trialStart = if (creationTimestamp > 0L) creationTimestamp else System.currentTimeMillis()
            prefs.edit().putLong(key, trialStart).apply()
        }

        val elapsedMs = System.currentTimeMillis() - trialStart
        val trialDurationMs = FREE_TRIAL_DURATION_DAYS * 24 * 60 * 60 * 1000L

        if (elapsedMs in 0 until trialDurationMs) {
            _isTrialActive.value = true
            val remainingMs = trialDurationMs - elapsedMs
            val remainingDays = kotlin.math.ceil(remainingMs.toDouble() / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(1)
            _trialDaysRemaining.value = remainingDays
            _hasFullAccess.value = true
            if (!_isPremium.value) {
                _subscriptionStatus.value = "Free Trial ($remainingDays day${if (remainingDays > 1) "s" else ""} left)"
            }
        } else {
            _isTrialActive.value = false
            _trialDaysRemaining.value = 0
            _hasFullAccess.value = _isPremium.value
            if (!_isPremium.value) {
                _subscriptionStatus.value = "Trial Expired"
            }
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(Date())
    }

    fun updateDailyFreeScansState() {
        updateTrialState()

        if (!::prefs.isInitialized) return

        val uid = try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        }

        if (uid.isNullOrBlank()) {
            _dailyFreeScansUsed.value = 0
            _dailyFreeScansRemaining.value = MAX_FREE_DAILY_SCANS
            return
        }

        val safeUid = uid.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val countKey = KEY_FREE_SCANS_COUNT_PREFIX + safeUid
        val dateKey = KEY_FREE_SCANS_DATE_PREFIX + safeUid
        val today = getTodayDateString()

        val lastDate = prefs.getString(dateKey, "") ?: ""
        val count = if (lastDate == today) {
            prefs.getInt(countKey, 0)
        } else {
            prefs.edit()
                .putString(dateKey, today)
                .putInt(countKey, 0)
                .apply()
            0
        }

        _dailyFreeScansUsed.value = count.coerceIn(0, MAX_FREE_DAILY_SCANS)
        _dailyFreeScansRemaining.value = (MAX_FREE_DAILY_SCANS - count).coerceAtLeast(0)
    }

    /**
     * Checks if user is eligible to scan:
     * 1. Active RevenueCat Premium: unlimited scans.
     * 2. Active 3-day Free Trial: up to 3 scans per day.
     * 3. After 3-day Free Trial ends: user MUST purchase a plan to scan.
     */
    fun canScan(): Boolean {
        if (_isPremium.value) return true
        updateTrialState()
        if (!_isTrialActive.value) return false
        updateDailyFreeScansState()
        return _dailyFreeScansRemaining.value > 0
    }

    /**
     * Consumes one scan locally and syncs with state.
     */
    fun consumeScan() {
        if (_isPremium.value || !::prefs.isInitialized) return

        val uid = try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        } ?: return

        val safeUid = uid.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val countKey = KEY_FREE_SCANS_COUNT_PREFIX + safeUid
        val dateKey = KEY_FREE_SCANS_DATE_PREFIX + safeUid
        val today = getTodayDateString()

        val current = if (prefs.getString(dateKey, "") == today) {
            prefs.getInt(countKey, 0) + 1
        } else {
            1
        }

        prefs.edit()
            .putString(dateKey, today)
            .putInt(countKey, current)
            .apply()

        updateDailyFreeScansState()
    }

    /**
     * Synchronizes user's trial start time and daily scans directly with Firestore.
     * If user logs in on a new device or re-logs in, trial start time is fetched from Firestore
     * so it CANNOT be reset to 0 or restarted.
     */
    fun syncUser(userId: String?) {
        if (userId.isNullOrBlank()) return
        if (FirebaseAuth.getInstance().currentUser?.uid != userId) return
        _isPremium.value = false
        _hasFullAccess.value = false

        val safeUid = userId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val key = KEY_TRIAL_START_TIME_PREFIX + safeUid

        // Sync trial start time from Firestore first
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userDocRef = db.collection("users").document(userId)
                val snapshot = userDocRef.get().await()

                val cloudTrialStart = snapshot.getLong("trialStartTime")
                if (cloudTrialStart != null && cloudTrialStart > 0L) {
                    // Always honor the cloud trial start time (cannot be reset)
                    prefs.edit().putLong(key, cloudTrialStart).apply()
                } else {
                    // Record trial start time in Firestore permanently
                    val existingLocal = prefs.getLong(key, 0L)
                    val auth = FirebaseAuth.getInstance()
                    val creationTime = auth.currentUser?.metadata?.creationTimestamp ?: 0L
                    val finalTrialStart = when {
                        existingLocal > 0L -> existingLocal
                        creationTime > 0L -> creationTime
                        else -> System.currentTimeMillis()
                    }
                    prefs.edit().putLong(key, finalTrialStart).apply()
                    userDocRef.set(
                        mapOf(
                            "trialStartTime" to finalTrialStart,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()
                }

                // Sync today's usage from Firestore
                val today = getTodayDateString()
                val usageSnap = userDocRef.collection("usage").document(today).get().await()
                if (usageSnap.exists()) {
                    val count = usageSnap.getLong("scanCount")?.toInt() ?: 0
                    val countKey = KEY_FREE_SCANS_COUNT_PREFIX + safeUid
                    val dateKey = KEY_FREE_SCANS_DATE_PREFIX + safeUid
                    prefs.edit()
                        .putString(dateKey, today)
                        .putInt(countKey, count)
                        .apply()
                }

                launch(Dispatchers.Main) {
                    updateTrialState()
                    updateDailyFreeScansState()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing trial state with firestore: ${e.message}")
                launch(Dispatchers.Main) {
                    updateTrialState()
                    updateDailyFreeScansState()
                }
            }
        }

        if (!Purchases.isConfigured) return

        Purchases.sharedInstance.logIn(
            userId,
            object : com.revenuecat.purchases.interfaces.LogInCallback {
                override fun onReceived(customerInfo: CustomerInfo, created: Boolean) {
                    Log.d(TAG, "RevenueCat user logged in: $userId")
                    if (FirebaseAuth.getInstance().currentUser?.uid != userId) return
                    applyCustomerInfo(customerInfo)
                    updateTrialState()
                    updateDailyFreeScansState()
                }

                override fun onError(error: PurchasesError) {
                    Log.w(TAG, "RevenueCat login sync note: ${error.message}")
                    updateTrialState()
                }
            }
        )
    }

    fun resetUser() {
        _isTrialActive.value = false
        _trialDaysRemaining.value = 0
        _hasFullAccess.value = false
        _isPremium.value = false
        _subscriptionStatus.value = "Free"
        _activePlanName.value = null
        _activeProduct.value = null
        _expirationDate.value = null
        _dailyFreeScansUsed.value = 0
        _dailyFreeScansRemaining.value = MAX_FREE_DAILY_SCANS

        if (!Purchases.isConfigured) return
        Purchases.sharedInstance.logOut(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                Log.d(TAG, "RevenueCat user logged out")
                // Anonymous billing callbacks must not restore the previous account entitlement.
            }

            override fun onError(error: PurchasesError) {
                Log.w(TAG, "RevenueCat logout note: ${error.message}")
                _isPremium.value = false
                _subscriptionStatus.value = "Free"
                updateTrialState()
            }
        })
    }

    fun refreshCustomerInfo(onComplete: ((Boolean) -> Unit)? = null) {
        if (!Purchases.isConfigured) {
            _isPremium.value = false
            _subscriptionStatus.value = "Free"
            onComplete?.invoke(false)
            return
        }

        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                val active = applyCustomerInfo(customerInfo)
                onComplete?.invoke(active)
            }

            override fun onError(error: PurchasesError) {
                Log.w(TAG, "Failed to get customer info: ${error.message}")
                onComplete?.invoke(_isPremium.value)
            }
        })
    }

    private fun applyCustomerInfo(customerInfo: CustomerInfo): Boolean {
        val currentUid = try { FirebaseAuth.getInstance().currentUser?.uid } catch (_: Exception) { null }
        if (currentUid == null || !Purchases.isConfigured || Purchases.sharedInstance.appUserID != currentUid) {
            _isPremium.value = false
            _hasFullAccess.value = false
            return false
        }
        val entitlement = customerInfo.entitlements[ENTITLEMENT_ID]

        val isActive = entitlement?.isActive == true

        _isPremium.value = isActive
        _hasFullAccess.value = isActive || _isTrialActive.value

        if (isActive) {
            _subscriptionStatus.value = "Active"
            val prodId = entitlement.productIdentifier
            _activeProduct.value = prodId

            _activePlanName.value = when {
                prodId.contains("weekly", ignoreCase = true) -> "Weekly Pro"
                prodId.contains("monthly", ignoreCase = true) -> "Monthly Pro"
                prodId.contains("yearly", ignoreCase = true) || prodId.contains("annual", ignoreCase = true) -> "Yearly Pro"
                else -> "Pro"
            }

            val expDate = entitlement.expirationDate
            if (expDate != null) {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                _expirationDate.value = sdf.format(expDate)
            } else {
                _expirationDate.value = "Lifetime / No Expiry"
            }
        } else {
            _activeProduct.value = null
            _activePlanName.value = null
            _expirationDate.value = null
            updateTrialState()
        }

        return isActive
    }

    fun fetchOfferings(onOfferingsFetched: ((Offerings?) -> Unit)? = null) {
        if (!Purchases.isConfigured) {
            onOfferingsFetched?.invoke(null)
            return
        }

        _isLoading.value = true
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                _isLoading.value = false
                _offerings.value = offerings
                onOfferingsFetched?.invoke(offerings)
            }

            override fun onError(error: PurchasesError) {
                _isLoading.value = false
                _errorMessage.value = error.message
                Log.w(TAG, "Error fetching offerings: ${error.message}")
                onOfferingsFetched?.invoke(null)
            }
        })
    }

    fun purchasePackage(
        activity: Activity,
        packageToPurchase: Package,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Purchases.isConfigured) {
            onError("In-app purchases are not configured or supported.")
            return
        }

        _isLoading.value = true
        val params = PurchaseParams.Builder(activity, packageToPurchase).build()
        Purchases.sharedInstance.purchase(params, object : PurchaseCallback {
            override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                _isLoading.value = false
                val active = applyCustomerInfo(customerInfo)
                if (active) {
                    onSuccess()
                } else {
                    onError("Purchase completed, but entitlement verification is pending.")
                }
            }

            override fun onError(error: PurchasesError, userCancelled: Boolean) {
                _isLoading.value = false
                if (userCancelled) {
                    onError("Purchase was cancelled.")
                } else {
                    onError(error.message)
                }
            }
        })
    }

    fun restorePurchases(onResult: (isSuccess: Boolean, message: String) -> Unit) {
        if (!Purchases.isConfigured) {
            onResult(false, "Billing is not available on this device.")
            return
        }

        _isLoading.value = true
        Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                _isLoading.value = false
                val active = applyCustomerInfo(customerInfo)
                if (active) {
                    onResult(true, "Purchases restored! LeafLens Premium is active.")
                } else {
                    onResult(false, "No active subscription found for this account.")
                }
            }

            override fun onError(error: PurchasesError) {
                _isLoading.value = false
                onResult(false, error.message)
            }
        })
    }
}
