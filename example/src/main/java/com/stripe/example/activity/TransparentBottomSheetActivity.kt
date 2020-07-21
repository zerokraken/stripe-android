package com.stripe.example.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.stripe.example.databinding.ActivityTransparentBottomSheetBinding

class TransparentBottomSheetActivity : AppCompatActivity() {
    val viewBinding by lazy {
        ActivityTransparentBottomSheetBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.root.setOnClickListener {
            finish()
        }

        val bottomSheet: View = viewBinding.bottomSheet
        val bottomSheetBehaviour = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehaviour.peekHeight = -1
        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehaviour.isHideable = true

        bottomSheetBehaviour.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    finish()
                }
            }

        })

        viewBinding.modal.setOnClickListener {
            startActivity(Intent(this, TransparentBottomSheetActivity::class.java))
        }
    }
}