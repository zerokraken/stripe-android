package com.stripe.android.checkout

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.stripe.android.R

class CheckoutAddCardFragment : Fragment(R.layout.fragment_checkout_add_card) {
    private val viewModel by activityViewModels<CheckoutViewModel> {
        CheckoutViewModel.Factory(requireActivity().application)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity == null) {
            return
        }

        viewModel.setPaymentMode(CheckoutViewModel.PaymentMode.New)
    }
}
