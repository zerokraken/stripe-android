package com.stripe.android.view

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.stripe.android.ApiRequest
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.StripeApiRepository
import com.stripe.android.databinding.ActivityCheckoutBinding
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.CheckoutActivity.Args.Companion.putCheckoutArgs
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class CheckoutActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityCheckoutBinding.inflate(layoutInflater)
    }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.root.setOnClickListener {
            animateOut()
        }

        val bottomSheet: View = viewBinding.bottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.state = STATE_HIDDEN

        supportFragmentManager
            .beginTransaction()
            .replace(viewBinding.fragmentContainer.id, CheckoutPaymentMethodListFragment())
            .commitAllowingStateLoss()

        withDelay(300) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == STATE_HIDDEN) {
                        finish()
                    }
                }
            })
        }
//        viewModel.transition.observe(this, Observer {
//            when(it) {
//                Transition.PUSH -> {
//                    supportFragmentManager.beginTransaction()
//                        .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
//                        .replace(R.id.fragment_container, AMCPushFragment())
//                        .addToBackStack(null)
//                        .commit()
//                }
//            }
//        })
    }

    private fun withDelay(delayMillis: Long, fn: () -> Unit) {
        lifecycleScope.launch {
            delay(delayMillis)
            withContext(Dispatchers.Main) {
                fn()
            }
        }
    }

    private fun animateOut() {
        // When the bottom sheet finishes animating to its new state,
        // the callback will finish the activity
        bottomSheetBehavior.state = STATE_HIDDEN
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            super.onBackPressed()
        } else {
            animateOut()
        }
    }

    internal class ViewModel(application: Application) : AndroidViewModel(application) {
        val config = PaymentConfiguration.getInstance(application)

        val publishableKey = config.publishableKey
        private val stripeRepository = StripeApiRepository(application, publishableKey, Stripe.appInfo)

        fun getPaymentMethods(customerId: String, privateKey: String): LiveData<List<PaymentMethod>> = liveData(Dispatchers.IO) {
            val result = stripeRepository.getPaymentMethods(
                ListPaymentMethodsParams(
                    customerId = customerId,
                    paymentMethodType = PaymentMethod.Type.Card
                ),
                publishableKey,
                setOf(),
                ApiRequest.Options(privateKey, config.stripeAccountId)
            )
            emit(result)
        }
    }

    override fun finish() {
        super.finish()
        // TOOD set result
        overridePendingTransition(0, 0)
    }

    @Parcelize
    internal data class Args(
        val clientSecret: String,
        val ephemeralKey: String,
        val customer: String
    ) : Parcelable {

        internal companion object {
            private const val EXTRA_ARGS = "checkout_activity_args"

            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }

            fun Intent.putCheckoutArgs(args: Args): Intent = this.putExtra(EXTRA_ARGS, args)
        }
    }

    internal class Contract : ActivityResultContract<Args, String?>() {
        override fun createIntent(context: Context, input: Args): Intent {
            return Intent(context, CheckoutActivity::class.java)
                .putCheckoutArgs(input)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            // TODO: use a real result
            return null
        }
    }

    companion object {
        internal const val EXTRA_ARGS = "checkout_activity_args"
    }
}
