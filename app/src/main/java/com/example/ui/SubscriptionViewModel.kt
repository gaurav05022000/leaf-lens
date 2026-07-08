package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubscriptionViewModel : ViewModel() {

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _offerings = MutableStateFlow<Offerings?>(null)
    val offerings: StateFlow<Offerings?> = _offerings.asStateFlow()

    init {
        checkPremiumStatus()
        fetchOfferings()
    }

    private fun checkPremiumStatus() {
        if (!Purchases.isConfigured) {
            _isPremium.value = false
            return
        }
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                android.util.Log.d("RevenueCat", "Customer info received. Active entitlements: ${customerInfo.entitlements.active.keys}")
                // Check if the user has the "premium" entitlement active
                _isPremium.value = customerInfo.entitlements["leaflens Pro"]?.isActive == true
            }

            override fun onError(error: PurchasesError) {
                android.util.Log.e("RevenueCat", "Error fetching customer info: ${error.code} - ${error.message} - ${error.underlyingErrorMessage}")
                _isPremium.value = false
            }
        })
    }

    private fun fetchOfferings() {
        if (!Purchases.isConfigured) {
            _offerings.value = null
            return
        }
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                android.util.Log.d("RevenueCat", "Offerings received. Current offering ID: ${offerings.current?.identifier}")
                _offerings.value = offerings
            }

            override fun onError(error: PurchasesError) {
                android.util.Log.e("RevenueCat", "Error fetching offerings: ${error.code} - ${error.message} - ${error.underlyingErrorMessage}")
                _offerings.value = null
            }
        })
    }
}
