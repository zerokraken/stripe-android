package com.stripe.example.activity

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.stripe.android.CustomerSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.AddPaymentMethodActivityStarter
import com.stripe.android.view.CardDisplayTextFactory
import com.stripe.android.view.DeletePaymentMethodDialogFactory
import com.stripe.android.view.PaymentMethodsActivityStarter
import com.stripe.android.view.PaymentMethodsAdapter
import com.stripe.android.view.PaymentMethodsViewModel
import com.stripe.example.R
import com.stripe.example.databinding.ActivityTransparentBottomSheetBinding
import com.stripe.example.databinding.BottomSheetMainBinding
import com.stripe.example.databinding.McPushBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransparentBottomSheetActivity : AppCompatActivity() {
    val viewBinding by lazy {
        ActivityTransparentBottomSheetBinding.inflate(layoutInflater)
    }

    val viewModel: AMCViewModel by viewModels()
    lateinit var bottomSheetBehavior: BottomSheetBehavior<View>


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
            .replace(viewBinding.fragmentContainer.id, AMCRootFragment())
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
        viewModel.transition.observe(this, Observer {
            when(it) {
                Transition.PUSH -> {
                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                        .replace(R.id.fragment_container, AMCPushFragment())
                        .addToBackStack(null)
                        .commit()
                }
            }
        })
    }

    fun withDelay(delayMillis: Long, fn: () -> Unit) {
        lifecycleScope.launch {
            delay(delayMillis)
            withContext(Dispatchers.Main) {
                fn()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun animateOut() {
        bottomSheetBehavior.state = STATE_HIDDEN
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            super.onBackPressed()
        } else {
            animateOut()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}

class AMCViewModel(application: Application) : BaseViewModel(application) {
    val transition = MutableLiveData<Transition>()
}

class AMCRootFragment : Fragment(R.layout.bottom_sheet_main) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewBinding = BottomSheetMainBinding.bind(view)
        val viewModel by activityViewModels<AMCViewModel>()
        viewBinding.push.setOnClickListener {
            viewModel.transition.postValue(Transition.PUSH)
        }
    }
}

class AMCPushFragment : Fragment(R.layout.mc_push) {
    private val args = PaymentMethodsActivityStarter.Args.Builder().build()

    private val customerSession: CustomerSession by lazy {
        CustomerSession.getInstance()
    }

    private val viewModel: PaymentMethodsViewModel by lazy {
        ViewModelProvider(
            this,
            PaymentMethodsViewModel.Factory(
                requireActivity().application,
                customerSession,
                null,
                false
            )
        )[PaymentMethodsViewModel::class.java]
    }

    private val adapter: PaymentMethodsAdapter by lazy {
        PaymentMethodsAdapter(
            args,
            addableTypes = listOf(PaymentMethod.Type.Card),
            initiallySelectedPaymentMethodId = null,
            shouldShowGooglePay = false,
            useGooglePay = false,
            canDeletePaymentMethods = false
        )
    }

    private val cardDisplayTextFactory: CardDisplayTextFactory by lazy {
        CardDisplayTextFactory(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewBinding = McPushBinding.bind(view)
        viewModel.progressData.observe(viewLifecycleOwner, Observer {
            viewBinding.progressBar.visibility = if (it) {
                View.VISIBLE
            } else {
                View.GONE
            }
        })

        setupRecyclerView(viewBinding)

        fetchCustomerPaymentMethods()

        // This prevents the first click from being eaten by the focus.
        viewBinding.recycler.requestFocusFromTouch()
    }

    private fun setupRecyclerView(viewBinding: McPushBinding) {
        val deletePaymentMethodDialogFactory = DeletePaymentMethodDialogFactory(
            requireContext(),
            adapter,
            cardDisplayTextFactory,
            customerSession,
            viewModel.productUsage
        ) { viewModel.onPaymentMethodRemoved(it) }

        adapter.listener = object : PaymentMethodsAdapter.Listener {
            override fun onPaymentMethodClick(paymentMethod: PaymentMethod) {
                viewBinding.recycler.tappedPaymentMethod = paymentMethod
            }

            override fun onGooglePayClick() {

            }

            override fun onDeletePaymentMethodAction(paymentMethod: PaymentMethod) {
                deletePaymentMethodDialogFactory.create(paymentMethod).show()
            }
        }

        viewBinding.recycler.adapter = adapter
    }

    private fun onPaymentMethodCreated(data: Intent?) {
        data?.let {
            val result =
                AddPaymentMethodActivityStarter.Result.fromIntent(data)
            when (result) {
                is AddPaymentMethodActivityStarter.Result.Success -> {
                    onAddedPaymentMethod(result.paymentMethod)
                }
                is AddPaymentMethodActivityStarter.Result.Failure -> {
                    // TODO(mshafrir-stripe): notify user that payment method can not be added at this time
                }
                else -> {
                    // no-op
                }
            }
        } ?: fetchCustomerPaymentMethods()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AddPaymentMethodActivityStarter.REQUEST_CODE &&
            resultCode == Activity.RESULT_OK) {
            onPaymentMethodCreated(data)
        }
    }

    private fun onAddedPaymentMethod(paymentMethod: PaymentMethod) {
        if (paymentMethod.type?.isReusable == true) {
            // Refresh the list of Payment Methods with the new reusable Payment Method.
            fetchCustomerPaymentMethods()
            viewModel.onPaymentMethodAdded(paymentMethod)
        }
    }

    private fun fetchCustomerPaymentMethods() {
        viewModel.getPaymentMethods().observe(viewLifecycleOwner, Observer { result ->
            result.fold(
                onSuccess = { adapter.setPaymentMethods(it) },
                onFailure = {
                }
            )
        })
    }

}