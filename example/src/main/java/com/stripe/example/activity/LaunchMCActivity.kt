package com.stripe.example.activity

import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.stripe.android.view.CheckoutActivity
import com.stripe.example.databinding.ActivityLaunchMcBinding
import com.stripe.example.module.StripeIntentViewModel
import kotlinx.coroutines.Dispatchers
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
        viewBinding.launchMc.setOnClickListener {
            viewModel.createPaymentIntent("us").observe(
                this,
                Observer {
                    it.fold(
                        onSuccess = {
                            val secret = it.getString("secret")
                            CheckoutActivity.start(this, secret, ephemeralKey.key, ephemeralKey.customer)
                        },
                        onFailure = {
                            viewModel.status.postValue(viewModel.status.value + "\nFailed: ${it.message}")
                        }
                    )
                }
            )
        }
        viewModel.fetchEphemeralKey().observe(
            this,
            Observer {
                ephemeralKey = it
            }
        )
    }

    internal data class EphemeralKey(val key: String, val customer: String)

    internal class ViewModel(application: Application) : StripeIntentViewModel(application) {

        fun fetchEphemeralKey() = liveData {
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
    }
}
