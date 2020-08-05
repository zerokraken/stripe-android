package com.stripe.android.view

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.util.AttributeSet
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.ApiRequest
import com.stripe.android.CardUtils
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.StripeApiRepository
import com.stripe.android.StripeTextUtils
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardMetadata
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class CardNumberViewModel @JvmOverloads internal constructor(
    application: Application,
    workDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {
    private val publishableKey = PaymentConfiguration.getInstance(application).publishableKey
    private val stripeRepository = StripeApiRepository(
        application,
        publishableKey,
        workDispatcher = workDispatcher
    )

    private val responseHistory = mutableMapOf<String, CardMetadata>()
}

/**
 * A [StripeEditText] that handles spacing out the digits of a credit card.
 */
class CardNumberEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : StripeEditText(context, attrs, defStyleAttr) {

    private val publishableKey = PaymentConfiguration.getInstance(context).publishableKey
    private val stripeRepository = StripeApiRepository(
        context,
        publishableKey,
        workDispatcher = Dispatchers.IO
    )

    private val responseHistory = mutableMapOf<String, CardMetadata>()

    private val viewModel by lazy {
        ViewModelProvider(
            context as ViewModelStoreOwner,
            ViewModelProvider.AndroidViewModelFactory((context as Activity).application)
        ).get(CardNumberViewModel::class.java)
    }

    @VisibleForTesting
    var cardBrand: CardBrand = CardBrand.Unknown
        internal set(value) {
            val prevBrand = field
            field = value
            if (value != prevBrand) {
                brandChangeCallback(cardBrand)
                updateLengthFilter()
            }
        }

    @JvmSynthetic
    internal var brandChangeCallback: (CardBrand) -> Unit = {}
        set(callback) {
            field = callback

            // Immediately display the brand if known, in case this method is invoked when
            // partial data already exists.
            callback(cardBrand)
        }

    // invoked when a valid card has been entered
    @JvmSynthetic
    internal var completionCallback: () -> Unit = {}

    val lengthMax: Int
        get() {
            return cardBrand.getMaxLengthWithSpacesForCardNumber(fieldText)
        }

    private var ignoreChanges = false

    /**
     * Check whether or not the card number is valid
     */
    var isCardNumberValid: Boolean = false
        private set

    /**
     * A normalized form of the card number. If the entered card number is "4242 4242 4242 4242",
     * this will be "4242424242424242". If the entered card number is invalid, this is `null`.
     */
    val cardNumber: String?
        get() = if (isCardNumberValid) {
            StripeTextUtils.removeSpacesAndHyphens(fieldText)
        } else {
            null
        }

    init {
        setErrorMessage(resources.getString(R.string.invalid_card_number))
        listenForTextChanges()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_NUMBER)
        }
    }

    override val accessibilityText: String?
        get() {
            return resources.getString(R.string.acc_label_card_number_node, text)
        }

    @JvmSynthetic
    internal fun updateLengthFilter() {
        filters = arrayOf<InputFilter>(InputFilter.LengthFilter(lengthMax))
    }

    /**
     * Updates the selection index based on the current (pre-edit) index, and
     * the size change of the number being input.
     *
     * @param newLength the post-edit length of the string
     * @param editActionStart the position in the string at which the edit action starts
     * @param editActionAddition the number of new characters going into the string (zero for
     * delete)
     * @return an index within the string at which to put the cursor
     */
    @JvmSynthetic
    internal fun updateSelectionIndex(
        newLength: Int,
        editActionStart: Int,
        editActionAddition: Int
    ): Int {
        var gapsJumped = 0
        val gapSet = cardBrand.getSpacePositionsForCardNumber(fieldText)

        var skipBack = false
        gapSet.forEach { gap ->
            if (editActionStart <= gap && editActionStart + editActionAddition > gap) {
                gapsJumped++
            }

            // editActionAddition can only be 0 if we are deleting,
            // so we need to check whether or not to skip backwards one space
            if (editActionAddition == 0 && editActionStart == gap + 1) {
                skipBack = true
            }
        }

        var newPosition: Int = editActionStart + editActionAddition + gapsJumped
        if (skipBack && newPosition > 0) {
            newPosition--
        }

        return if (newPosition <= newLength) {
            newPosition
        } else {
            newLength
        }
    }

    private fun listenForTextChanges() {
        addTextChangedListener(object : StripeTextWatcher() {
            private var latestChangeStart: Int = 0
            private var latestInsertionSize: Int = 0

            private var newCursorPosition: Int? = null
            private var formattedNumber: String? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (!ignoreChanges) {
                    latestChangeStart = start
                    latestInsertionSize = after
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (ignoreChanges) {
                    return
                }

                val inputText = s?.toString().orEmpty()
//                if (start < 4) {
//                    updateCardBrandFromNumber(inputText)
//                }
                updateCardBrandFromNumber(inputText)

                if (start > 16) {
                    // no need to do formatting if we're past all of the spaces.
                    return
                }

                val spacelessNumber = StripeTextUtils.removeSpacesAndHyphens(inputText)
                    ?: return

                val formattedNumber = cardBrand.formatNumber(spacelessNumber)
                this.newCursorPosition = updateSelectionIndex(formattedNumber.length,
                    latestChangeStart, latestInsertionSize)
                this.formattedNumber = formattedNumber
            }

            override fun afterTextChanged(s: Editable?) {
                if (ignoreChanges) {
                    return
                }

                ignoreChanges = true
                if (!isLastKeyDelete && formattedNumber != null) {
                    setText(formattedNumber)
                    newCursorPosition?.let {
                        setSelection(it.coerceIn(0, fieldText.length))
                    }
                }
                formattedNumber = null
                newCursorPosition = null

                ignoreChanges = false

                if (fieldText.length == lengthMax) {
                    val wasCardNumberValid = isCardNumberValid
                    isCardNumberValid = CardUtils.isValidCardNumber(fieldText)
                    shouldShowError = !isCardNumberValid
                    if (!wasCardNumberValid && isCardNumberValid) {
                        completionCallback()
                    }
                } else {
                    isCardNumberValid = CardUtils.isValidCardNumber(fieldText)
                    // Don't show errors if we aren't full-length.
                    shouldShowError = false
                }
            }
        })
    }

    val workScope = CoroutineScope(Dispatchers.IO)

    @JvmSynthetic
    internal fun updateCardBrandFromNumber(partialNumber: String) {
        val cardNumber = StripeTextUtils.removeSpacesAndHyphens(partialNumber)
        if (cardNumber.isNullOrBlank() || cardNumber.length < 6) {
            cardBrand = CardBrand.Unknown
            return
        }
        val prefix = cardNumber.substring(0, 6)
        val previousMatch = responseHistory[prefix]
        if (previousMatch != null) {
            val matchingBrands = previousMatch.accountRanges.filter {
                it.matches(cardNumber)
            }
                .map { it.brand }
                .toSet()
            cardBrand = if (matchingBrands.size == 1) {
                CardBrand.fromCode(matchingBrands.first())
            } else {
                CardBrand.Unknown
            }
        } else {
            cardBrand = CardBrand.Loading

            workScope.launch {
                runCatching {
                    stripeRepository.getCardMetadata(
                        prefix,
                        ApiRequest.Options(publishableKey)
                    )
                }.onSuccess { cardMetadata ->
                    withContext(Dispatchers.Main) {
                        responseHistory[prefix] = cardMetadata
                        val matchingBrands = cardMetadata.accountRanges.filter {
                            it.matches(cardNumber)
                        }
                            .map { it.brand }
                            .toSet()
                        cardBrand = if (matchingBrands.size == 1) {
                            CardBrand.fromCode(matchingBrands.first())
                        } else {
                            CardBrand.Unknown
                        }
                    }
                }
            }
        }
    }
}
