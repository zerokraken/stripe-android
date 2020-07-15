package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.list.listItems
import com.afollestad.materialdialogs.list.updateListItems
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stripe.example.databinding.ActivityBottomSheetBinding
import com.stripe.example.databinding.FragmentBottomsheetBinding


class BottomSheetActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityBottomSheetBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

//        val bottomSheet: View = viewBinding.bottomSheet
//        val bottomSheeteBehaviour = BottomSheetBehavior.from(bottomSheet)
//        bottomSheeteBehaviour.isHideable = true
//        bottomSheeteBehaviour.peekHeight = 300
//        bottomSheeteBehaviour.state = BottomSheetBehavior.STATE_HIDDEN

        viewBinding.launchModal.setOnClickListener {
//            when (bottomSheeteBehaviour.state) {
//                BottomSheetBehavior.STATE_EXPANDED -> {
//                    bottomSheeteBehaviour.setState(BottomSheetBehavior.STATE_COLLAPSED)
//                }
//                BottomSheetBehavior.STATE_COLLAPSED -> {
//                    bottomSheeteBehaviour.setState(BottomSheetBehavior.STATE_HIDDEN)
//                }
//                BottomSheetBehavior.STATE_HIDDEN -> {
//                    bottomSheeteBehaviour.setState(BottomSheetBehavior.STATE_EXPANDED)
//                }
//            }
            startActivity(Intent(this, TransparentBottomSheetActivity::class.java))
        }
        viewBinding.launchFragment.setOnClickListener {
            supportFragmentManager.findFragmentByTag("bottom_sheet")?.let {
                supportFragmentManager.beginTransaction().remove(it).commit()
                return@setOnClickListener
            }
            val fragment = MyBottomSheetDialogFragment()
            supportFragmentManager.beginTransaction().add(fragment, "bottom_sheet").commit()
        }

        viewBinding.material.setOnClickListener {
            MaterialDialog(this, BottomSheet()).show {
                title(text = "This is a title")
                noAutoDismiss()
                listItems(items = listOf("one", "two", "three", "four")) { dialog, index, text ->
                    dialog.updateListItems(items = listOf("a", "b", "c"))
                }
            }
        }
    }

    class MyBottomSheetDialogFragment : BottomSheetDialogFragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val viewBinding = FragmentBottomsheetBinding.inflate(inflater)
            return viewBinding.root
        }
    }
}