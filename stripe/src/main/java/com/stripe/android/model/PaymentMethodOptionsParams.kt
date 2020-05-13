package com.stripe.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

sealed class PaymentMethodOptionsParams(
    val type: PaymentMethod.Type
) : StripeParamsModel, Parcelable {

    @Parcelize
    data class Card(
        var cvc: String? = null
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.Card) {
        override fun toParamMap(): Map<String, Any> {
            return mapOf(type.code to
                cvc?.let {
                    mapOf(PARAM_CVC to it)
                }.orEmpty()
            )
        }

        private companion object {
            private const val PARAM_CVC = "cvc"
        }
    }

    @Parcelize
    data class Alipay(
        val appBundleId: String,
        val appVersionKey: String
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.Alipay) {
        override fun toParamMap(): Map<String, Any> {
            return mapOf(type.code to
                mapOf(
                    "app_bundle_id" to appBundleId,
                    "app_version_key" to appVersionKey
                )
            )
        }
    }
}
