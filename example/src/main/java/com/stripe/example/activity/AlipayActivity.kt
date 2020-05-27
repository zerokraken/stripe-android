package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.alipay.sdk.app.PayTask
import com.stripe.android.AlipayAuthenticator
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentIntentResult
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.example.databinding.AlipayActivityBinding

class AlipayActivity : StripeIntentActivity() {

    private val viewBinding by lazy {
        AlipayActivityBinding.inflate(layoutInflater)
    }

    private val callback = object : ApiResultCallback<PaymentIntentResult> {
        override fun onSuccess(result: PaymentIntentResult) = onConfirmSuccess(result)
        override fun onError(e: Exception) = onConfirmError(e)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.status.observe(this, Observer(viewBinding.status::setText))
        viewModel.inProgress.observe(this, Observer {
            viewBinding.progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
        })

        viewBinding.fakeButton.setOnClickListener {
            confirmWithSdk(object : AlipayAuthenticator {
                override fun onAuthenticationRequest(data: String): Map<String, String> {
                    return mapOf("resultStatus" to viewBinding.resultCode.text.toString())
                }
            })
        }
        viewBinding.sdkButton.setOnClickListener {
            confirmWithSdk(object : AlipayAuthenticator {
                override fun onAuthenticationRequest(data: String): Map<String, String> {
                    return PayTask(this@AlipayActivity).payV2(data, true)
                }
            })
        }

        viewBinding.webButton.setOnClickListener {
            confirmWithWeb()
        }
    }

    private fun confirmWithSdk(authenticator: AlipayAuthenticator) {
        viewModel.createPaymentIntent("sg").observe(this, Observer {
            it.fold(
                onSuccess = {
                    viewModel.status.postValue(viewModel.status.value + "\n\nPayment Intent created")
                    val secret = it.getString("secret")

                    stripe.confirmAlipayPayment(
                        ConfirmPaymentIntentParams.createAlipay(secret, "example://return_url"),
                        authenticator = authenticator,
                        callback = callback
                    )
                },
                onFailure = ::onCreatePaymentIntentError
            )
        })
    }

    private fun confirmWithWeb() {
        viewModel.createPaymentIntent("sg").observe(this, Observer {
            it.fold(
                onSuccess = {
                    viewModel.status.postValue(viewModel.status.value + "\n\nPayment Intent created")
                    val secret = it.getString("secret")

                    stripe.confirmPayment(this,
                        ConfirmPaymentIntentParams
                            .createAlipay(secret, "example://return_url")
                    )
                },
                onFailure = ::onCreatePaymentIntentError
            )
        })
    }

    private fun onCreatePaymentIntentError(e: Throwable) {
        viewModel.status.postValue(viewModel.status.value + "\n\nFailed to create payment intent\n${e.message}")
        viewModel.inProgress.postValue(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        stripe.onPaymentResult(requestCode, data, callback)
    }
}
