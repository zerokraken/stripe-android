package com.stripe.android

import androidx.activity.ComponentActivity

class Checkout(val clientSecret: String, val ephemeralKey: String, val customerId: String) {
    fun confirm(activity: ComponentActivity, callback: (CompletionStatus) -> Unit) {
        val launcher = activity.registerForActivityResult(CheckoutContract()) {
            callback(it)
        }
        launcher.launch(CheckoutContract.Args(clientSecret, ephemeralKey, customerId))
    }

    sealed class CompletionStatus {
        object Succeeded : CompletionStatus()
        data class Failed(val error: Throwable) : CompletionStatus()
        object Cancelled : CompletionStatus()
    }
}
