package com.stripe.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.stripe.android.ApiResultCallback
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams.Companion.createAlipaySingleUseParams
import com.stripe.example.activity.StripeIntentActivity
import com.stripe.example.databinding.ActivityAlipayBinding



class AlipayActivity : AppCompatActivity() {
    private val stripeAccountId: String? by lazy {
        Settings(this).stripeAccountId
    }
    private val stripe: Stripe by lazy {
        StripeFactory(this, stripeAccountId).create()
    }
    private val viewBinding: ActivityAlipayBinding by lazy {
        ActivityAlipayBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

//        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        viewBinding.payNow.setOnClickListener {
//            viewBinding.status.append("creating source\n")
//            val alipaySingleUseParams = createAlipaySingleUseParams(
//                50L,  // Amount is a long int in the lowest denomination. 50 cents in USD is the minimum
//                "USD",
//                "Mr. Sample",  // customer name
//                "sample@sample.smp",  // customer email
//                "stripe-example://alipay") // a redirect address to get the user back into your app
//
//            stripe.createSource(
//                alipaySingleUseParams,
//                "some_unique_key_2",
//                callback = object : ApiResultCallback<Source> {
//                    override fun onSuccess(result: Source) {
//                        viewBinding.status.append("creating source succeeded\n")
////                        invokeAlipayWeb(result)
//
//                    }
//
//                    override fun onError(e: Exception) {
//                        viewBinding.status.append("creating source failed\n${e.message}\n")
//                    }
//                }
//            )
            val params = PaymentMethodCreateParams.createAlipay()
            viewBinding.status.text = "creating payment method\n"
            stripe.createPaymentMethod(params, callback = object : ApiResultCallback<PaymentMethod> {
                override fun onSuccess(result: PaymentMethod) {
                    viewBinding.status.append("Created payment method of type ${result.type}\n")
                }

                override fun onError(e: Exception) {
                    viewBinding.status.append("Failed to create payment method\n")
                    viewBinding.status.append("${e.message}\n")
                }

            })
        }
    }

    fun invokeAlipayApp(source: Source) {

    }



    fun invokeAlipayWeb(source: Source) {
        viewBinding.status.append("creating web intent\n")
        val redirectUrl = source.redirect?.url;
        val intent = Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(redirectUrl));
        startActivity(intent);
    }
}
