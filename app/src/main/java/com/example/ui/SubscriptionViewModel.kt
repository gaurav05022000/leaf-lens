package com.example.ui

import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.Offerings
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel bridge for screens that use SubscriptionViewModel,
 * backed by the centralized SubscriptionManager.
 */
class SubscriptionViewModel : ViewModel() {

    val isPremium: StateFlow<Boolean> = SubscriptionManager.isPremium
    val subscriptionStatus: StateFlow<String> = SubscriptionManager.subscriptionStatus
    val activeProduct: StateFlow<String?> = SubscriptionManager.activeProduct
    val activePlanName: StateFlow<String?> = SubscriptionManager.activePlanName
    val expirationDate: StateFlow<String?> = SubscriptionManager.expirationDate
    val offerings: StateFlow<Offerings?> = SubscriptionManager.offerings
    val isLoading: StateFlow<Boolean> = SubscriptionManager.isLoading
    val isBillingAvailable: StateFlow<Boolean> = SubscriptionManager.isBillingAvailable
    val errorMessage: StateFlow<String?> = SubscriptionManager.errorMessage

    val dailyFreeScansRemaining: StateFlow<Int> = SubscriptionManager.dailyFreeScansRemaining

    fun refresh() {
        SubscriptionManager.refreshCustomerInfo()
        SubscriptionManager.fetchOfferings()
    }
}
