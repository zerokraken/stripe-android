package com.stripe.example.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.example.R
import com.stripe.example.databinding.LauncherActivityBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewBinding = LauncherActivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val linearLayoutManager = LinearLayoutManager(this)
            .apply {
                orientation = LinearLayoutManager.VERTICAL
            }

        viewBinding.examples.run {
            setHasFixedSize(true)
            layoutManager = linearLayoutManager
            adapter = ExamplesAdapter(this@LauncherActivity)
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            delay(2000)
            startPaymentSheetLauncherFragment(
                this@LauncherActivity,
                "pi_1ISjP8CRMbs6FrXfAdnoXUZX_secret_PvjS1Jf4K3qev4G8i9O7QsVqJ"
            )
        }
    }

    private fun startPaymentSheetLauncherFragment(
        activity: FragmentActivity,
        clientSecret: String
    ) {
        val fragment = InvisibleFragment().also {
            it.arguments = bundleOf(
                "client_secret" to clientSecret
            )
        }
        activity.supportFragmentManager.beginTransaction()
            .add(fragment, "payment_sheet_launch_fragment")
            .commit()
    }

    private class ExamplesAdapter constructor(
        private val activity: Activity
    ) : RecyclerView.Adapter<ExamplesAdapter.ExamplesViewHolder>() {
        private val items = listOf(
            Item(
                activity.getString(R.string.payment_auth_example),
                PaymentAuthActivity::class.java
            ),
            Item(
                activity.getString(R.string.create_card_token),
                CreateCardTokenActivity::class.java
            ),
            Item(
                activity.getString(R.string.create_card_payment_method),
                CreateCardPaymentMethodActivity::class.java
            ),
            Item(
                activity.getString(R.string.create_card_source),
                CreateCardSourceActivity::class.java
            ),
            Item(
                activity.getString(R.string.launch_customer_session),
                CustomerSessionActivity::class.java
            ),
            Item(
                activity.getString(R.string.launch_payment_session),
                PaymentSessionActivity::class.java
            ),
            Item(
                activity.getString(R.string.launch_payment_session_from_fragment),
                FragmentExamplesActivity::class.java
            ),
            Item(
                activity.getString(R.string.launch_pay_with_google),
                PayWithGoogleActivity::class.java
            ),
            Item(
                activity.getString(R.string.launch_confirm_pm_sepa_debit),
                ConfirmSepaDebitActivity::class.java
            ),
            Item(
                activity.getString(R.string.fpx_payment_example),
                FpxPaymentActivity::class.java
            ),
            Item(
                activity.getString(R.string.klarna_source_example),
                KlarnaSourceActivity::class.java
            ),
            Item(
                activity.getString(R.string.becs_debit_example),
                BecsDebitPaymentMethodActivity::class.java
            ),
            Item(
                activity.getString(R.string.bacs_debit_example),
                BacsDebitPaymentMethodActivity::class.java
            ),
            Item(
                activity.getString(R.string.sofort_example),
                SofortPaymentMethodActivity::class.java
            ),
            Item(
                activity.getString(R.string.upi_example),
                UpiPaymentActivity::class.java
            ),
            Item(
                activity.getString(R.string.netbanking_example),
                NetbankingPaymentActivity::class.java
            ),
            Item(
                activity.getString(R.string.card_brands),
                CardBrandsActivity::class.java
            ),
            Item(
                activity.getString(R.string.simple_payment_method_example),
                SimplePaymentMethodConfirmationActivity::class.java
            ),
            Item(
                activity.getString(R.string.connect_example),
                ConnectExampleActivity::class.java
            )
        )

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ExamplesViewHolder {
            val root = activity.layoutInflater
                .inflate(R.layout.launcher_item, viewGroup, false)
            return ExamplesViewHolder(root)
        }

        override fun onBindViewHolder(examplesViewHolder: ExamplesViewHolder, i: Int) {
            val itemView = examplesViewHolder.itemView
            (itemView as TextView).text = items[i].text
            itemView.setOnClickListener {
                activity.startActivity(Intent(activity, items[i].activityClass))
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        private data class Item constructor(val text: String, val activityClass: Class<*>)

        private class ExamplesViewHolder constructor(
            itemView: View
        ) : RecyclerView.ViewHolder(itemView)
    }

    internal class InvisibleFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            return FrameLayout(requireActivity()).also {
                it.isGone = true
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val paymentSheet = PaymentSheet(this) { result ->
                result
                Toast.makeText(requireActivity(), result.toString(), Toast.LENGTH_SHORT)
                    .show()
            }

            paymentSheet.present(
                paymentIntentClientSecret = arguments?.getString("client_secret").orEmpty()
            )
        }
    }
}
