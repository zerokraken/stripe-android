package com.stripe.example.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.stripe.example.R
import com.stripe.example.databinding.ActivityTransparentBottomSheetBinding

class TransparentBottomSheetActivity : AppCompatActivity() {
    val viewBinding by lazy {
        ActivityTransparentBottomSheetBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val bottomSheet: View = viewBinding.bottomSheet
        val bottomSheeteBehaviour = BottomSheetBehavior.from(bottomSheet)
        bottomSheeteBehaviour.isHideable = false
        bottomSheeteBehaviour.peekHeight = 300
        bottomSheeteBehaviour.state = BottomSheetBehavior.STATE_EXPANDED

//        bottomSheeteBehaviour.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
//            override fun onSlide(bottomSheet: View, slideOffset: Float) {
//            }
//
//            override fun onStateChanged(bottomSheet: View, newState: Int) {
//                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
//                    finish()
//                }
//            }
//
//        })
    }
}