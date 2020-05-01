package com.stripe.android.view

import android.content.Context
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import com.stripe.android.StripeResourceManager
import com.stripe.android.model.StripeJsonUtils
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

internal class BecsDebitBanks(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    private val shouldIncludeTestBank: Boolean = true
) {
    private val banks: LiveData<List<Bank>>

    init {
        val resourceManager = StripeResourceManager.getInstance(context)
        val jsonLiveData = MutableLiveData<JSONObject>()
        banks = Transformations.map(jsonLiveData, ::createBanksData)
        banks.observe(lifecycleOwner, Observer { })

        resourceManager
            .fetchJson("au_becs_bsb", jsonLiveData)
        resourceManager.fetchJson("au_becs_bsb") {
            Log.d("StripeResourceManager", "second callback")
        }
    }

    fun byPrefix(bsb: String): Bank? {
        return banks
            .value
            ?.plus(listOfNotNull(STRIPE_TEST_BANK.takeIf { shouldIncludeTestBank }))
            ?.firstOrNull {
                bsb.startsWith(it.prefix)
            }
    }

    @Parcelize
    data class Bank(
        internal val prefix: String,
        internal val code: String,
        internal val name: String
    ) : Parcelable

    private companion object {
        private fun createBanksData(json: JSONObject): List<Bank> {
            return StripeJsonUtils.jsonObjectToMap(json)
                .orEmpty()
                .map { entry ->
                (entry.value as List<*>).let {
                    Bank(
                        prefix = entry.key,
                        code = it.first().toString(),
                        name = it.last().toString()
                    )
                }
            }
        }

        private val STRIPE_TEST_BANK = Bank(
            prefix = "00",
            code = "STRIPE",
            name = "Stripe Test Bank"
        )
    }
}
