package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.databinding.ActivityCheckoutBinding
import kotlinx.android.parcel.Parcelize

class CheckoutActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityCheckoutBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val args: Args? = intent.getParcelableExtra(EXTRA_ARGS)
        if (args != null) {
            Toast.makeText(this, "Found some args", Toast.LENGTH_LONG).show()
        }
    }

    @Parcelize
    private data class Args(
        val clientSecret: String,
        val ephemeralKey: String,
        val customer: String
    ) : Parcelable

    companion object {
        private const val EXTRA_ARGS = "checkout_activity_args"

        fun start(activity: Activity, clientSecret: String, ephemeralKey: String, customer: String) {
            activity.startActivity(Intent(activity, CheckoutActivity::class.java)
                .putExtra(EXTRA_ARGS, Args(clientSecret, ephemeralKey, customer)))
        }
    }
}
