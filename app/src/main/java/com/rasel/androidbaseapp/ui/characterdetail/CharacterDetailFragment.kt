package com.rasel.androidbaseapp.ui.characterdetail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.rasel.androidbaseapp.base.BaseFragment
import com.bumptech.glide.RequestManager
import com.rasel.androidbaseapp.R
import com.rasel.androidbaseapp.databinding.FragmentCharacterDetailBinding
import com.rasel.androidbaseapp.presentation.viewmodel.BaseViewModel
import com.rasel.androidbaseapp.presentation.viewmodel.Bookmark
import com.rasel.androidbaseapp.presentation.viewmodel.CharacterDetailUIModel
import com.rasel.androidbaseapp.presentation.viewmodel.CharacterDetailViewModel
import com.rasel.androidbaseapp.util.observe
import com.rasel.androidbaseapp.util.showSnackBar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CharacterDetailFragment : BaseFragment<FragmentCharacterDetailBinding, BaseViewModel>() {

    override fun getViewBinding(): FragmentCharacterDetailBinding = FragmentCharacterDetailBinding.inflate(layoutInflater)

    override val viewModel: CharacterDetailViewModel by viewModels()

    private val args: CharacterDetailFragmentArgs by navArgs()

    @Inject
    lateinit var glide: RequestManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe(viewModel.getCharacter(), ::onViewStateChange)
        viewModel.getCharacterDetail(args.characterId)
        setUiChangeListeners()
    }

    private fun setUiChangeListeners() {
        binding.checkBoxBookmark.setOnCheckedChangeListener { view, isChecked ->
            if (!binding.checkBoxBookmark.isPressed) {
                return@setOnCheckedChangeListener
            }
            if (isChecked)
                viewModel.setBookmarkCharacter(view.tag.toString().toLong())
            else
                viewModel.setUnBookmarkCharacter(view.tag.toString().toLong())
        }
    }

    private fun onViewStateChange(result: CharacterDetailUIModel) {
        if (result.isRedelivered) return
        when (result) {
            is CharacterDetailUIModel.BookMarkStatus -> {
                when (result.bookmark) {
                    Bookmark.BOOKMARK ->
                        if (result.status) {
                            showSnackBar(binding.rootView, getString(R.string.bookmark_success))
                        } else {
                            handleErrorMessage(getString(R.string.bookmark_error))
                        }
                    Bookmark.UN_BOOKMARK ->
                        if (result.status) {
                            showSnackBar(
                                binding.rootView,
                                getString(R.string.un_bookmark_success)
                            )
                        } else {
                            handleErrorMessage(getString(R.string.bookmark_error))
                        }
                }
            }
            is CharacterDetailUIModel.Error -> handleErrorMessage(result.error)
            CharacterDetailUIModel.Loading -> handleLoading(true)
            is CharacterDetailUIModel.Success -> {
                handleLoading(false)
                result.data.let { character ->
                    binding.apply {
                        binding.textViewCharacterName.text = character.name
                        glide.load(character.image).into(imageViewCharacter)
                        checkBoxBookmark.tag = character.id
                        checkBoxBookmark.isChecked = character.isBookMarked
                        textViewSpecies.text = character.species
                        textViewGender.text = character.gender
                        textViewGenderLocation.text = character.characterLocation?.name
                        textViewStatus.text = character.status
                    }
                }
            }
        }
    }
}
