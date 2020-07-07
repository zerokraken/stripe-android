package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.databinding.ActivityOxxoBinding

class OxxoActivity : StripeIntentActivity() {
    private val viewBinding by lazy {
        ActivityOxxoBinding.inflate(layoutInflater)
    }

    private val billingDetails: PaymentMethod.BillingDetails?
    get() {
        val name = viewBinding.name.text
        val email = viewBinding.email.text
        if (name.isNullOrBlank() || email.isNullOrBlank()) {
            return null
        } else {
            return PaymentMethod.BillingDetails(name = name.toString(), email = email.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        viewModel.inProgress.observe(this, Observer { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        viewBinding.confirm.setOnClickListener {
            billingDetails?.let {
                createAndConfirmPaymentIntent("mx", PaymentMethodCreateParams.createOxxo(it))
            } ?: Toast.makeText(this@OxxoActivity, "Missing details!", Toast.LENGTH_LONG).show()
        }
    }

    private fun enableUi(enabled: Boolean) {
        viewBinding.confirm.isEnabled = enabled
        viewBinding.name.isEnabled = enabled
        viewBinding.email.isEnabled = enabled
        viewBinding.progressBar.visibility = if (enabled) View.INVISIBLE else View.VISIBLE
    }
}