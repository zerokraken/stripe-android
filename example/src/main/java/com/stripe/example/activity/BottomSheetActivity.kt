package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.setPeekHeight
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.list.listItems
import com.afollestad.materialdialogs.list.updateListItems
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stripe.example.R
import com.stripe.example.databinding.ActivityBottomSheetBinding
import com.stripe.example.databinding.BottomSheetMainBinding
import com.stripe.example.databinding.McRootBinding


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
//            supportFragmentManager.findFragmentByTag("bottom_sheet")?.let {
//                supportFragmentManager.beginTransaction().remove(it).commit()
//                return@setOnClickListener
//            }
            val fragment = MyBottomSheetDialogFragment()
            fragment.dialog?.setOnShowListener { dialog ->
                (dialog as BottomSheetDialog).behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
            supportFragmentManager.beginTransaction().add(fragment, "bottom_sheet").commit()
        }

        viewBinding.material.setOnClickListener {
            val mainDialog = MaterialDialog(this, BottomSheet()).show {
                title(text = "This is a title")
                customView(R.layout.bottom_sheet_main)
                noAutoDismiss()
                cornerRadius(16f)
                setPeekHeight(1200)
                icon(R.drawable.ic_brandicon__giropay)
//                cancelable(false)
//                cancelOnTouchOutside(false)
                listItems(items = listOf("one", "two", "three", "four", "five", "six")) { dialog, index, text ->
                    dialog.updateListItems(items = emptyList())
                }
            }

//            mainDialog.setOnKeyListener { dialog, keyCode, event ->
//                keyCode == KeyEvent.KEYCODE_BACK
//            }

            val binding = BottomSheetMainBinding.bind(mainDialog.getCustomView())
            binding.modal.setOnClickListener {
                MaterialDialog(this, BottomSheet()).show {
                    title(text="This is a second dialog")
                    listItems(items=listOf("Some", "List", "Items", "Here"))
                }
            }
//            binding.cancelable.setOnCheckedChangeListener { button, checked ->
//                mainDialog.cancelable(checked)
//                mainDialog.cancelOnTouchOutside(checked)
//            }
        }
        viewBinding.materialFragments.setOnClickListener {
            val dialog = MaterialDialog(this, BottomSheet()).show {
                title(text="How about some fragments")
                cornerRadius(12f)
                icon(R.drawable.stripe_ic_visa)
                customView(R.layout.mc_root)
            }

            val viewModel by viewModels<MCActivityViewModel>()
            dialog.setOnKeyListener { dialog, keyCode, event ->
                when(keyCode) {
                    KeyEvent.KEYCODE_BACK -> {
                        if (event.action == KeyEvent.ACTION_UP) {
                            viewModel.backPressed.postValue(Unit)
                        }
                        true
                    }
                    else -> false
                }
            }
            viewModel.completed.observe(this, Observer {
                dialog.dismiss()
            })
        }
    }

    class MCPushFragment : Fragment(R.layout.mc_push)


    class MyBottomSheetDialogFragment : BottomSheetDialogFragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val viewBinding = BottomSheetMainBinding.inflate(inflater)
            return viewBinding.root
        }

        fun behavior(): BottomSheetBehavior<FrameLayout> {
            return (dialog as BottomSheetDialog).behavior
        }
    }
}