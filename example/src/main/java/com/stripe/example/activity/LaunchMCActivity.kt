package com.stripe.example.activity

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.stripe.android.view.CheckoutActivity
import com.stripe.example.databinding.ActivityLaunchMcBinding
import com.stripe.example.module.StripeIntentViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LaunchMCActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityLaunchMcBinding.inflate(layoutInflater)
    }

    private val viewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(application)
        )[ViewModel::class.java]
    }

    private lateinit var ephemeralKey: EphemeralKey

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(
            this,
            Observer {
                viewBinding.progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
                viewBinding.launchMc.isEnabled = !it
            }
        )
        viewModel.status.observe(
            this,
            Observer {
                viewBinding.status.text = it
            }
        )
        viewBinding.clear.setOnClickListener {
            viewModel.clearKeys(this)
        }
        viewBinding.launchMc.setOnClickListener {
            // Disabled because we don't need it right now
//            viewModel.createPaymentIntent("us").observe(
//                this,
//                Observer {
//                    it.fold(
//                        onSuccess = {
//                            val secret = it.getString("secret")
//                            CheckoutActivity.start(this, secret, ephemeralKey.key, ephemeralKey.customer)
//                        },
//                        onFailure = {
//                            viewModel.status.postValue(viewModel.status.value + "\nFailed: ${it.message}")
//                        }
//                    )
//                }
//            )
            CheckoutActivity.start(this, "", ephemeralKey.key, ephemeralKey.customer)
        }
        viewModel.fetchEphemeralKey(this).observe(
            this,
            Observer {
                ephemeralKey = it
            }
        )
    }

    internal data class EphemeralKey(val key: String, val customer: String)

    internal class ViewModel(application: Application) : StripeIntentViewModel(application) {

        fun clearKeys(activity: Activity) {
            val prefs = activity.getPreferences(Context.MODE_PRIVATE)
            CoroutineScope(workContext).launch {
                prefs.edit()
                    .clear()
                    .commit()
            }
        }

        fun fetchEphemeralKey(activity: Activity) = liveData {
            val prefs = activity.getPreferences(Context.MODE_PRIVATE)
            val ek = prefs.getString(PREF_EK, null)
            val customer = prefs.getString(PREF_CUSTOMER, null)
            if (ek != null && customer != null) {
                emit(EphemeralKey(ek, customer))
                return@liveData
            }

            inProgress.postValue(true)
            status.postValue("Fetching ephemeral key")
            val responseJson =
                kotlin.runCatching {
                    backendApi
                        .createEphemeralKey(hashMapOf("api_version" to "2020-03-02"))
                        .string()
                }
            responseJson.fold(
                onSuccess = {
                    // TOOD: create separate endpoint that only sends necessary info
                    val json = JSONObject(it)
                    val secret = json.getString("secret")
                    val associatedObjectArray = json.getJSONArray("associated_objects")
                    val typeObject = associatedObjectArray.getJSONObject(0)
                    val objectId = typeObject.getString("id")

                    status.postValue(status.value + "\n\nFetched key $secret for customer $objectId")

                    prefs.edit()
                        .putString(PREF_EK, secret)
                        .putString(PREF_CUSTOMER, objectId)
                        .commit()

                    withContext(Dispatchers.Main) {
                        emit(EphemeralKey(secret, objectId))
                    }
                },
                onFailure = {
                    status.postValue(status.value + "\n\nFetching ephemeral key failed\n${it.message}")
                }
            )
            inProgress.postValue(false)
        }

        private companion object {
            private const val PREF_EK = "pref_ek"
            private const val PREF_PI = "pref_pi"
            private const val PREF_CUSTOMER = "pref_customer"
        }
    }
}
