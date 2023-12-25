package com.rasel.androidbaseapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import com.rasel.androidbaseapp.domain.interactor.CharacterBookmarkUseCase
import com.rasel.androidbaseapp.domain.interactor.CharacterUnBookmarkUseCase
import com.rasel.androidbaseapp.domain.interactor.GetCharacterByIdUseCase
import com.rasel.androidbaseapp.presentation.utils.CoroutineContextProvider
import com.rasel.androidbaseapp.presentation.utils.ExceptionHandler
import com.rasel.androidbaseapp.presentation.utils.UiAwareLiveData
import com.rasel.androidbaseapp.presentation.utils.UiAwareModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import javax.inject.Inject
import com.rasel.androidbaseapp.domain.models.Character

private const val TAG = "CharacterDetailVM"

sealed class CharacterDetailUIModel : UiAwareModel() {
    object Loading : CharacterDetailUIModel()
    data class Error(var error: String = "") : CharacterDetailUIModel()
    data class Success(val data: Character) : CharacterDetailUIModel()
    data class BookMarkStatus(val bookmark: Bookmark, val status: Boolean) :
        CharacterDetailUIModel()
}

enum class Bookmark {
    BOOKMARK,
    UN_BOOKMARK
}

@HiltViewModel
class CharacterDetailViewModel @Inject constructor(
    contextProvider: CoroutineContextProvider,
    private val characterByIdUseCase: GetCharacterByIdUseCase,
    private val bookmarkUserCase: CharacterBookmarkUseCase,
    private val unBookmarkUserCase: CharacterUnBookmarkUseCase
) : BaseViewModel(contextProvider) {

    private val _character = UiAwareLiveData<CharacterDetailUIModel>()
    private var character: LiveData<CharacterDetailUIModel> = _character

    fun getCharacter(): LiveData<CharacterDetailUIModel> {
        return character
    }

    override val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        val message = ExceptionHandler.parse(exception)
        _character.postValue(CharacterDetailUIModel.Error(exception.message ?: "Error"))
    }

    fun getCharacterDetail(characterId: Long) {
        _character.postValue(CharacterDetailUIModel.Loading)
        launchCoroutineIO {
            loadCharacter(characterId)
        }
    }

    private suspend fun loadCharacter(characterId: Long) {
        characterByIdUseCase(characterId).collect {
            _character.postValue(CharacterDetailUIModel.Success(it))
        }
    }

    fun setBookmarkCharacter(characterId: Long) {
        launchCoroutineIO {
            bookmarkUserCase(characterId).collect {
                if (it == 1)
                    _character.postValue(
                        CharacterDetailUIModel.BookMarkStatus(
                            Bookmark.BOOKMARK,
                            true
                        )
                    )
                else
                    _character.postValue(
                        CharacterDetailUIModel.BookMarkStatus(
                            Bookmark.BOOKMARK,
                            false
                        )
                    )
            }
        }
    }

    fun setUnBookmarkCharacter(characterId: Long) {
        launchCoroutineIO {
            unBookmarkUserCase(characterId).collect {
                if (it == 1)
                    _character.postValue(
                        CharacterDetailUIModel.BookMarkStatus(
                            Bookmark.UN_BOOKMARK,
                            true
                        )
                    )
                else
                    _character.postValue(
                        CharacterDetailUIModel.BookMarkStatus(
                            Bookmark.UN_BOOKMARK,
                            false
                        )
                    )
            }
        }
    }
}
