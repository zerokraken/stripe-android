package com.stripe.example.activity

import android.app.Application
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.stripe.example.R
import com.stripe.example.databinding.ActivityTransparentBottomSheetBinding
import com.stripe.example.databinding.BottomSheetMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransparentBottomSheetActivity : AppCompatActivity() {
    val viewBinding by lazy {
        ActivityTransparentBottomSheetBinding.inflate(layoutInflater)
    }
    private val workScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    val viewModel: AMCViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.root.setOnClickListener {
            finish()
        }

        val bottomSheet: View = viewBinding.bottomSheet
        val bottomSheetBehaviour = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehaviour.peekHeight = -1
        bottomSheetBehaviour.isHideable = true
        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN

        bottomSheetBehaviour.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    finish()
                }
            }

        })

        supportFragmentManager
            .beginTransaction()
            .replace(viewBinding.fragmentContainer.id, AMCRootFragment())
            .commitAllowingStateLoss()

        workScope.launch {
            delay(300)
            scope.launch {
                bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
                bottomSheetBehaviour.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    }

                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            finish()
                        }
                    }

                })
            }
        }
        viewModel.transition.observe(this, Observer {
            when(it) {
                Transition.PUSH -> {
                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.transition.enter_from_right, R.transition.exit_to_left, R.transition.enter_from_left, R.transition.exit_to_right)
                        .replace(R.id.fragment_container, AMCPushFragment())
                        .addToBackStack(null)
                        .commit()
                }
            }
        })
    }

    override fun finish() {
        overridePendingTransition(0, 0)
        super.finish()
    }
}

class AMCViewModel(application: Application) : BaseViewModel(application) {
    val transition = MutableLiveData<Transition>()
}

class AMCRootFragment : Fragment(R.layout.bottom_sheet_main) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewBinding = BottomSheetMainBinding.bind(view)
        val viewModel by activityViewModels<AMCViewModel>()
        viewBinding.push.setOnClickListener {
            viewModel.transition.postValue(Transition.PUSH)
        }
    }
}

class AMCPushFragment : Fragment(R.layout.mc_push)