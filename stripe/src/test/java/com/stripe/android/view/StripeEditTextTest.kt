package com.stripe.android.view

import android.content.Context
import android.content.res.ColorStateList
import androidx.annotation.ColorInt
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.stripe.android.R
import com.stripe.android.testharness.ViewTestUtils
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class StripeEditTextTest {
    private val context: Context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        R.style.StripeDefaultTheme
    )
    private val afterTextChangedListener: StripeEditText.AfterTextChangedListener = mock()
    private val deleteEmptyListener: StripeEditText.DeleteEmptyListener = mock()

    private val editText = StripeEditText(
        context
    ).also {
        it.setDeleteEmptyListener(deleteEmptyListener)
        it.setAfterTextChangedListener(afterTextChangedListener)
    }

    @Test
    fun deleteText_whenZeroLength_callsDeleteListener() {
        ViewTestUtils.sendDeleteKeyEvent(editText)
        verify(deleteEmptyListener).onDeleteEmpty()
        verifyNoMoreInteractions(afterTextChangedListener)
    }

    @Test
    fun addText_callsAppropriateListeners() {
        editText.append("1")
        verifyNoMoreInteractions(deleteEmptyListener)
        verify(afterTextChangedListener)
            .onTextChanged("1")
    }

    @Test
    fun deleteText_whenNonZeroLength_callsAppropriateListeners() {
        editText.append("1")

        ViewTestUtils.sendDeleteKeyEvent(editText)
        verifyNoMoreInteractions(deleteEmptyListener)
        verify(afterTextChangedListener)
            .onTextChanged("")
    }

    @Test
    fun deleteText_whenSelectionAtBeginningButLengthNonZero_doesNotCallListener() {
        editText.append("12")
        verify(afterTextChangedListener)
            .onTextChanged("12")
        editText.setSelection(0)
        ViewTestUtils.sendDeleteKeyEvent(editText)
        verifyNoMoreInteractions(deleteEmptyListener)
        verifyNoMoreInteractions(afterTextChangedListener)
    }

    @Test
    fun deleteText_whenDeletingMultipleItems_onlyCallsListenerOneTime() {
        editText.append("123")
        // Doing this four times because we need to delete all three items, then jump back.

        repeat(4) {
            ViewTestUtils.sendDeleteKeyEvent(editText)
        }

        verify(deleteEmptyListener).onDeleteEmpty()
    }

    @Test
    fun getDefaultErrorColorInt_onDarkTheme_returnsDarkError() {
        editText.defaultColorStateList = ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.primary_text_dark))
        assertThat(editText.defaultErrorColorInt)
            .isEqualTo(ContextCompat.getColor(context, R.color.stripe_error_text_dark_theme))
    }

    @Test
    fun getDefaultErrorColorInt_onLightTheme_returnsLightError() {
        editText.setTextColor(ContextCompat.getColor(context, android.R.color.primary_text_light))
        assertThat(editText.defaultErrorColorInt)
            .isEqualTo(ContextCompat.getColor(context, R.color.stripe_error_text_light_theme))
    }

    @Test
    fun setErrorColor_whenInError_overridesDefault() {
        // By default, the text color in this test activity is a light theme
        @ColorInt val blueError = 0x0000ff
        editText.setErrorColor(blueError)
        editText.shouldShowError = true
        val currentColorInt = editText.textColors.defaultColor
        assertThat(currentColorInt)
            .isEqualTo(blueError)
    }

    @Test
    fun setTextColor() {
        editText.setTextColor(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    android.R.color.holo_red_dark
                )
            )
        )

        // The field state must be toggled to show an error
        editText.shouldShowError = true
        editText.shouldShowError = false

        assertThat(editText.textColors)
            .isEqualTo(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        android.R.color.holo_red_dark
                    )
                )
            )
    }

    @Test
    fun getCachedColorStateList_afterInit_returnsNotNull() {
        assertThat(editText.defaultColorStateList)
            .isNotNull()
    }

    @Test
    fun setShouldShowError_whenErrorColorNotSet_shouldUseDefaultErrorColor() {
        editText.shouldShowError = true
        assertThat(editText.textColors.defaultColor)
            .isEqualTo(ContextCompat.getColor(context, R.color.stripe_error_text_light_theme))
    }

    @Test
    fun shouldShowError_whenChanged_changesTextColor() {
        editText.errorMessage = "There was an error!"

        editText.shouldShowError = true
        assertThat(editText.currentTextColor)
            .isEqualTo(-1369050)

        editText.shouldShowError = false
        assertThat(editText.currentTextColor)
            .isEqualTo(-570425344)
    }
}
