package com.stripe.android.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.R
import com.stripe.android.databinding.FragmentCheckoutPaymentMethodListBinding
import com.stripe.android.databinding.LayoutCheckoutPaymentMethodItemBinding
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod

class CheckoutPaymentMethodListFragment : Fragment(R.layout.fragment_checkout_payment_method_list) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentCheckoutPaymentMethodListBinding.bind(view)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val viewModel by activityViewModels<CheckoutActivity.ViewModel>()
        val intent = requireActivity().intent
        val args: CheckoutActivity.Args? = intent.getParcelableExtra(CheckoutActivity.EXTRA_ARGS)

        if (args != null) {
            viewModel.getPaymentMethods(args.customer, args.ephemeralKey).observe(viewLifecycleOwner, Observer {
                binding.recycler.adapter = Adapter(it)
            })
        } else {
            binding.recycler.adapter = Adapter(listOf(
                PaymentMethod("amex", 0, false, PaymentMethod.Type.Card,
                    card = PaymentMethod.Card(CardBrand.AmericanExpress, last4 = "1234")),
                PaymentMethod("visa", 0, false, PaymentMethod.Type.Card,
                    card = PaymentMethod.Card(CardBrand.Visa, last4 = "4242")),
                PaymentMethod("mastercard", 0, false, PaymentMethod.Type.Card,
                    card = PaymentMethod.Card(CardBrand.MasterCard, last4 = "6789"))
            ))
        }
    }

    private class Adapter(val methods: List<PaymentMethod>) : RecyclerView.Adapter<Adapter.VH>() {

        private data class VH(val binding: LayoutCheckoutPaymentMethodItemBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bindMethod(method: PaymentMethod) {
                binding.brandIcon.setImageResource(method.card!!.brand.icon)
                binding.cardNumber.setText("····" + method.card!!.last4)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = LayoutCheckoutPaymentMethodItemBinding.inflate(LayoutInflater.from(parent.context))
            return VH(binding)
        }

        override fun getItemCount(): Int {
            return methods.size
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val method = methods[position]
            holder.bindMethod(method)
        }
    }
}
