package com.stripe.android.view

import android.content.Context
import android.os.Parcelable
import android.util.Log
import com.stripe.android.StripeResourceManager
import com.stripe.android.model.StripeJsonUtils
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

internal class BecsDebitBanks(
    context: Context,
    private val shouldIncludeTestBank: Boolean = true
) {
    internal var banks: List<Bank>

    init {
        val resourceManager = StripeResourceManager.getInstance(context)
        banks = createBanksData(
            requireNotNull(
                resourceManager
                    .fetchJson("au_becs_bsb") { result: Result<JSONObject> ->
                        result.fold({
                            Log.d("StripeResourceManager", "updating banks")
                            banks = createBanksData(it)
                            Unit
                        }, {
                            Log.e("StripeResourceManager", "error in BecsDebitBanks $it")
                        })
                    }))
    }

    fun byPrefix(bsb: String): Bank? {
        return banks
            .plus(listOfNotNull(STRIPE_TEST_BANK.takeIf { shouldIncludeTestBank }))
            .firstOrNull {
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
