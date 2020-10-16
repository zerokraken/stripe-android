package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentsheetAddCardBinding
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class PaymentSheetAddCardFragment : Fragment(R.layout.fragment_paymentsheet_add_card) {
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

        val binding = FragmentPaymentsheetAddCardBinding.bind(view)

        if (activityViewModel.sheetMode.value == PaymentSheetViewModel.SheetMode.FULL) {
            binding.cardMultilineWidget.cardNumberEditText.requestFocus()
            val imm: InputMethodManager? = getSystemService(requireContext(), InputMethodManager::class.java)
            imm?.showSoftInput(binding.cardMultilineWidget.cardNumberEditText, InputMethodManager.SHOW_IMPLICIT)
        }

        binding.cardMultilineWidget.setCardValidCallback { isValid, _ ->
            val selection = if (isValid) {
                binding.cardMultilineWidget.paymentMethodCreateParams?.let {
                    PaymentSelection.New(it)
                }
            } else {
                null
            }
            activityViewModel.updateSelection(selection)
        }
    }
}
