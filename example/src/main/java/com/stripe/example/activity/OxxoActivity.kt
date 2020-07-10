package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.stripe.example.databinding.ActivityOxxoBinding
import com.stripe.example.module.StripeIntentViewModel

class OxxoActivity : AppCompatActivity() {
    private val viewModel: StripeIntentViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(application)
        )[StripeIntentViewModel::class.java]
    }
    private val viewBinding by lazy {
        ActivityOxxoBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        viewModel.inProgress.observe(this, Observer { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        viewBinding.confirm.setOnClickListener {
            startCheckout()
        }

        // TODO: create payment intent
    }

    private fun startCheckout() {
        enableUi(false)
        val email = viewBinding.email.text.toString()
        val name = viewBinding.name.text.toString()
        // TODO: confirm payment intent
    }

    private fun createPaymentIntent() {
        enableUi(false)
        viewModel.createPaymentIntent("mx")
            .observe(
                this,
                Observer { result ->
                    result.fold(
                        onSuccess = {
                            val secret = it.getString("secret")
                            // TODO: Use the client secrets
                        },
                        onFailure = {
                            appendStatus("Creating Payment Intent Failed!")
                            appendStatus(it.message ?: "No error message")
                        }
                    )
                    enableUi(true)
                }
            )
    }

    private fun appendStatus(text: String) {
        viewModel.status.postValue("${viewModel.status.value}\n\n$text")
    }

    private fun enableUi(enabled: Boolean) {
        viewBinding.confirm.isEnabled = enabled
        viewBinding.name.isEnabled = enabled
        viewBinding.email.isEnabled = enabled
        viewBinding.progressBar.visibility = if (enabled) View.INVISIBLE else View.VISIBLE
    }
}
