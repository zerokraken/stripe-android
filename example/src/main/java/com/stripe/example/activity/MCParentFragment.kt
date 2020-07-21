package com.stripe.example.activity

import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.stripe.example.R
import com.stripe.example.databinding.BottomSheetMainBinding

class MCParentFragment : Fragment(R.layout.mc_parent_fragment) {
    val viewModel: MCViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, MCRootFragment())
            .commit()

        val activityViewModel by activityViewModels<MCActivityViewModel>()
        activityViewModel.backPressed.observe(viewLifecycleOwner, Observer {
            val count: Int = childFragmentManager.backStackEntryCount

            if (count > 0) {
                childFragmentManager.popBackStack()
            } else {
                activityViewModel.completed.postValue(Unit)
            }
        })
        viewModel.transition.observe(viewLifecycleOwner, Observer {
            when(it) {
                Transition.PUSH -> {
                    childFragmentManager.beginTransaction()
                        .setCustomAnimations(R.transition.enter_from_right, R.transition.exit_to_left, R.transition.enter_from_left, R.transition.exit_to_right)
                        .replace(R.id.fragment_container, MCPushFragment())
                        .addToBackStack(null)
                        .commit()
                }
            }
        })
    }

}

class MCViewModel(application: Application) : BaseViewModel(application) {
    val transition = MutableLiveData<Transition>()
}

class MCActivityViewModel(application: Application) : BaseViewModel(application) {
    val backPressed = MutableLiveData<Unit>()
    val completed = MutableLiveData<Unit>()
}

enum class Transition {
    PUSH
}

class MCRootFragment : Fragment(R.layout.bottom_sheet_main) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewBinding = BottomSheetMainBinding.bind(view)
        val viewModel by requireParentFragment().viewModels<MCViewModel>()
        viewBinding.push.setOnClickListener {
            viewModel.transition.postValue(Transition.PUSH)
        }
    }
}

class MCPushFragment : Fragment(R.layout.mc_push)