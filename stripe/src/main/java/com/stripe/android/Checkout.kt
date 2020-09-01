package com.stripe.android

import androidx.activity.ComponentActivity
import com.stripe.android.view.CheckoutActivity

class Checkout(val clientSecret: String, val ephemeralKey: String, val customerId: String) {
    fun confirm(activity: ComponentActivity, callback: (CompletionStatus) -> Unit) {
        val launcher = activity.registerForActivityResult(CheckoutActivity.Contract()) {
            // TODO: actually handle result
            callback(CompletionStatus.SUCCEEDED)
        }
        launcher.launch(CheckoutActivity.Args(clientSecret, ephemeralKey, customerId))
    }

    sealed class CompletionStatus {
        object SUCCEEDED : CompletionStatus()
        data class FAILED(val error: Throwable) : CompletionStatus()
        object CANCELLED : CompletionStatus()
    }
}
