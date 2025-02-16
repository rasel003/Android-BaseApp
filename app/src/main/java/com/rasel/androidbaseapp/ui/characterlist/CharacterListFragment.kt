package com.rasel.androidbaseapp.ui.characterlist

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rasel.androidbaseapp.R
import com.rasel.androidbaseapp.base.BaseFragment
import com.rasel.androidbaseapp.databinding.FragmentCharacterListBinding
import com.rasel.androidbaseapp.domain.models.Character
import com.rasel.androidbaseapp.presentation.viewmodel.BaseViewModel
import com.rasel.androidbaseapp.presentation.viewmodel.CharacterListViewModel
import com.rasel.androidbaseapp.presentation.viewmodel.CharacterUIModel
import com.rasel.androidbaseapp.util.LoadingUtils
import com.rasel.androidbaseapp.util.doOnApplyWindowInsets
import com.rasel.androidbaseapp.util.observe
import com.rasel.androidbaseapp.util.showToast
import com.rasel.androidbaseapp.util.smoothSnapToPosition
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class CharacterListFragment : BaseFragment<FragmentCharacterListBinding, BaseViewModel>(),
    CharacterAdapter.ClickListener {

    override fun getViewBinding(): FragmentCharacterListBinding =
        FragmentCharacterListBinding.inflate(layoutInflater)

    override val viewModel: CharacterListViewModel by viewModels()

    @Inject
    lateinit var characterAdapter: CharacterAdapter


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pad the bottom of the ScrollView so that it scrolls up above the nav bar
        view.doOnApplyWindowInsets { v, insets, padding ->
            v.updatePaddingRelative(
                top = padding.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
//                top = padding.top + insets.systemWindowInsetTop
            )
        }
        if (characterAdapter.itemCount < 1) {
            val isFavorite =
                (findNavController().currentDestination?.label == getString(R.string.menu_favorites))
            viewModel.getCharacters(isFavorite)
        }
        initRecyclerView()
        observe(viewModel.getCharacters(), ::onViewStateChange)

        /*postponeEnterTransition()
        binding.recyclerViewCharacters.doOnPreDraw {
            startPostponedEnterTransition()
        }*/

        binding.btnScrollTo.setOnClickListener {
            val position = Random.nextInt(1, characterAdapter.itemCount - 1)
            binding.btnScrollTo.text = "Position $position"
            binding.recyclerViewCharacters.smoothSnapToPosition(position)

            binding.toggleButton.check(binding.btnDarkMode.id)
        }
        binding.toggleButton.addOnButtonCheckedListener { toggleButton, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener  // Ignore uncheck events

//            if (toggleButton.isPressed) {  // Ensure it's a user action
                when (checkedId) {
                    binding.btnLightMode.id -> {
                        mContext.showToast("Light Selected")
                        Timber.tag("rsl").i("Light Mode Selected")
                    }
                    binding.btnDarkMode.id -> {
                        mContext.showToast("Dark Selected")
                        Timber.tag("rsl").i("Dark Mode Selected")
                    }
                }
//            }
        }
    }

    private fun initRecyclerView() {
        binding.recyclerViewCharacters.apply {
            adapter = characterAdapter
        }

        characterAdapter.setUpListener(this)
        characterAdapter.setItemClickListener { character ->

        }
    }

    private fun onViewStateChange(event: CharacterUIModel) {
        if (event.isRedelivered) return
        when (event) {
            is CharacterUIModel.Error -> handleErrorMessage(event.error)
            is CharacterUIModel.Loading -> handleLoading(true)
            is CharacterUIModel.Success -> {
                handleLoading(false)
                event.data.let {
                    Timber.d("Fetched character list")
                    characterAdapter.list = it
                }
            }

            else -> {}
        }
    }

    override fun handleLoading(isLoading: Boolean) {
//        super.handleLoading(isLoading)

        if (isLoading) {
            LoadingUtils.showDialog(context, true)
            /*binding.recyclerViewCharacters.visibility = View.GONE
            binding.shimmerFrameLayout.visibility = View.VISIBLE
            binding.shimmerFrameLayout.startShimmerAnimation()*/
        } else {
            /*  binding.recyclerViewCharacters.visibility = View.VISIBLE
              binding.shimmerFrameLayout.stopShimmerAnimation()
              binding.shimmerFrameLayout.visibility = View.GONE*/

            LoadingUtils.hideDialog()

        }
    }

    override fun onItemSelected(
        character: Character,
        transitionView: View,
        transitionName: String
    ) {
        val action =
            CharacterListFragmentDirections.actionCharacterListFragmentToCharacterDetailFragment(
                character.id.toLong()
            )
        val extra = FragmentNavigatorExtras(transitionView to transitionName)
        findNavController().navigate(action, extra)
    }
}