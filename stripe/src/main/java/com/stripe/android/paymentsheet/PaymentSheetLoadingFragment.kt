package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.stripe.android.R
import com.stripe.android.paymentsheet.PaymentSheetViewModel.TransitionTarget

class PaymentSheetLoadingFragment : Fragment(R.layout.fragment_payment_sheet_loading) {
    private val activityViewModel by activityViewModels<PaymentSheetViewModel> {
        PaymentSheetViewModel.Factory {
            requireActivity().application
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity == null) {
            return
        }

        activityViewModel.paymentIntent.observe(requireActivity()) {
            maybeTransition()
        }
        activityViewModel.paymentMethods.observe(requireActivity()) {
            maybeTransition()
        }
        activityViewModel.updatePaymentMethods(requireActivity().intent)
        activityViewModel.fetchPaymentIntent(requireActivity().intent)
    }

    private fun maybeTransition() {
        if (activityViewModel.paymentIntent.value == null) {
            return
        }
        val paymentMethods = activityViewModel.paymentMethods.value ?: return
        val target = if (paymentMethods.isEmpty()) {
            TransitionTarget.AddPaymentMethodSheet
        } else {
            TransitionTarget.SelectSavedPaymentMethod
        }
        activityViewModel.transitionTo(target)
    }
}
