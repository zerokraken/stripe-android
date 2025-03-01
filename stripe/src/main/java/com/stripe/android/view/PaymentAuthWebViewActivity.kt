package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.stripe.android.Logger
import com.stripe.android.R
import com.stripe.android.StripeIntentResult
import com.stripe.android.auth.PaymentAuthWebViewContract
import com.stripe.android.databinding.PaymentAuthWebViewActivityBinding
import com.stripe.android.exception.StripeException
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.stripe3ds2.utils.CustomizeUtils
import com.ults.listeners.SdkChallengeInterface.UL_HANDLE_CHALLENGE_ACTION

class PaymentAuthWebViewActivity : AppCompatActivity() {

    private val viewBinding: PaymentAuthWebViewActivityBinding by lazy {
        PaymentAuthWebViewActivityBinding.inflate(layoutInflater)
    }

    private val _args: PaymentAuthWebViewContract.Args? by lazy {
        PaymentAuthWebViewContract().parseArgs(intent)
    }

    private val logger: Logger by lazy {
        Logger.getInstance(_args?.enableLogging == true)
    }
    private val viewModel: PaymentAuthWebViewActivityViewModel by viewModels {
        PaymentAuthWebViewActivityViewModel.Factory(requireNotNull(_args))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = _args
        if (args == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        logger.debug("PaymentAuthWebViewActivity#onCreate()")

        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent().setAction(UL_HANDLE_CHALLENGE_ACTION))

        setContentView(viewBinding.root)

        setSupportActionBar(viewBinding.toolbar)
        customizeToolbar()

        val clientSecret = args.clientSecret
        setResult(Activity.RESULT_OK, createResultIntent(viewModel.paymentResult))

        if (clientSecret.isBlank()) {
            logger.debug("PaymentAuthWebViewActivity#onCreate() - clientSecret is blank")
            finish()
            return
        }

        logger.debug("PaymentAuthWebViewActivity#onCreate() - PaymentAuthWebView init and loadUrl")

        val isPagedLoaded = MutableLiveData(false)
        isPagedLoaded.observe(this) { shouldHide ->
            if (shouldHide) {
                viewBinding.progressBar.isGone = true
            }
        }

        val webViewClient = PaymentAuthWebViewClient(
            logger,
            isPagedLoaded,
            clientSecret,
            args.returnUrl,
            ::startActivity,
            ::onAuthComplete
        )
        viewBinding.webView.onLoadBlank = {
            webViewClient.hasLoadedBlank = true
        }
        viewBinding.webView.webViewClient = webViewClient
        viewBinding.webView.webChromeClient = PaymentAuthWebChromeClient(this, logger)

        viewBinding.webView.loadUrl(args.url)
    }

    @VisibleForTesting
    internal fun onAuthComplete(error: Throwable?) {
        if (error != null) {
            setResult(
                Activity.RESULT_OK,
                createResultIntent(
                    viewModel.paymentResult
                        .copy(
                            exception = StripeException.create(error),
                            flowOutcome = StripeIntentResult.Outcome.FAILED,
                            shouldCancelSource = true
                        )
                )
            )
        }
        finish()
    }

    override fun onDestroy() {
        viewBinding.webViewContainer.removeAllViews()
        viewBinding.webView.destroy()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        logger.debug("PaymentAuthWebViewActivity#onCreateOptionsMenu()")
        menuInflater.inflate(R.menu.payment_auth_web_view_menu, menu)

        viewModel.buttonText?.let {
            logger.debug("PaymentAuthWebViewActivity#customizeToolbar() - updating close button text")
            menu.findItem(R.id.action_close).title = it
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        logger.debug("PaymentAuthWebViewActivity#onOptionsItemSelected()")
        if (item.itemId == R.id.action_close) {
            cancelIntentSource()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (viewBinding.webView.canGoBack()) {
            viewBinding.webView.goBack()
        } else {
            cancelIntentSource()
        }
    }

    private fun cancelIntentSource() {
        setResult(Activity.RESULT_OK, viewModel.cancellationResult)
        finish()
    }

    private fun customizeToolbar() {
        logger.debug("PaymentAuthWebViewActivity#customizeToolbar()")

        viewModel.toolbarTitle?.let {
            logger.debug("PaymentAuthWebViewActivity#customizeToolbar() - updating toolbar title")
            viewBinding.toolbar.title = CustomizeUtils.buildStyledText(
                this,
                it.text,
                it.toolbarCustomization
            )
        }

        viewModel.toolbarBackgroundColor?.let { backgroundColor ->
            logger.debug("PaymentAuthWebViewActivity#customizeToolbar() - updating toolbar background color")
            @ColorInt val backgroundColorInt = Color.parseColor(backgroundColor)
            viewBinding.toolbar.setBackgroundColor(backgroundColorInt)
            CustomizeUtils.setStatusBarColor(this, backgroundColorInt)
        }
    }

    private fun createResultIntent(
        paymentFlowResult: PaymentFlowResult.Unvalidated
    ) = Intent().putExtras(paymentFlowResult.toBundle())
}
