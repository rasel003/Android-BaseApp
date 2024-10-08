package com.rasel.androidbaseapp.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.ListenableWorker.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.rasel.androidbaseapp.cache.dao.PlantDao
import com.rasel.androidbaseapp.cache.entities.Plant
import com.rasel.androidbaseapp.data.HomeRepository
import com.rasel.androidbaseapp.presentation.utils.CoroutineContextProvider
import com.rasel.androidbaseapp.presentation.utils.ExceptionHandler
import com.rasel.androidbaseapp.presentation.utils.UiAwareModel
import com.rasel.androidbaseapp.util.PLANT_DATA_FILENAME
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PlantUIModel : UiAwareModel() {
    object Loading : CharacterUIModel()
    data class Error(var error: String = "") : CharacterUIModel()
    data class Success(val data: List<Plant>) : CharacterUIModel()
}

/**
 * The ViewModel for [PlantListFragment].
 */
@HiltViewModel
class PlantListViewModel @Inject internal constructor(
    contextProvider: CoroutineContextProvider,
    plantRepository: HomeRepository,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
    private val plantDao: PlantDao
) : BaseViewModel(contextProvider) {

    private val growZone: MutableStateFlow<Int> = MutableStateFlow(
        savedStateHandle[GROW_ZONE_SAVED_STATE_KEY] ?: NO_GROW_ZONE
    )

    override val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        val message = ExceptionHandler.parse(exception)
    }

    val plants: LiveData<List<Plant>> = growZone.flatMapLatest { zone ->
        if (zone == NO_GROW_ZONE) {
            plantRepository.getPlants()
        } else {
            plantRepository.getPlantsWithGrowZoneNumber(zone)
        }
    }.asLiveData()

    init {

        /**
         * When `growZone` changes, store the new value in `savedStateHandle`.
         *
         * There are a few ways to write this; all of these are equivalent. (This info is from
         * https://github.com/android/sunflower/pull/671#pullrequestreview-548900174)
         *
         * 1) A verbose version:
         *
         *    viewModelScope.launch {
         *        growZone.onEach { newGrowZone ->
         *            savedStateHandle.set(GROW_ZONE_SAVED_STATE_KEY, newGrowZone)
         *        }
         *    }.collect()
         *
         * 2) A simpler version of 1). Since we're calling `collect`, we can consume
         *    the elements in the `collect`'s lambda block instead of using the `onEach` operator.
         *    This is the version that's used in the live code below.
         *
         * 3) We can avoid creating a new coroutine using the `launchIn` terminal operator. In this
         *    case, `onEach` is needed because `launchIn` doesn't take a lambda to consume the new
         *    element in the Flow; it takes a `CoroutineScope` that's used to create a coroutine
         *    internally.
         *
         *    growZone.onEach { newGrowZone ->
         *        savedStateHandle.set(GROW_ZONE_SAVED_STATE_KEY, newGrowZone)
         *    }.launchIn(viewModelScope)
         */
        viewModelScope.launch {
            growZone.collect { newGrowZone ->
                savedStateHandle[GROW_ZONE_SAVED_STATE_KEY] = newGrowZone
            }
        }
        /*viewModelScope.launch(Dispatchers.IO) {
            context.assets.open(PLANT_DATA_FILENAME).use { inputStream ->
                JsonReader(inputStream.reader()).use { jsonReader ->
                    val plantType = object : TypeToken<List<Plant>>() {}.type
                    val plantList: List<Plant> = Gson().fromJson(jsonReader, plantType)

//                    val database = AppDatabase.invoke(applicationContext)
                    plantDao.insertAll(plantList)

                    Result.success()
                }
            }
        }*/
    }

    fun setGrowZoneNumber(num: Int) {
        growZone.value = num
    }

    fun clearGrowZoneNumber() {
        growZone.value = NO_GROW_ZONE
    }

    fun isFiltered() = growZone.value != NO_GROW_ZONE

    companion object {
        private const val NO_GROW_ZONE = -1
        private const val GROW_ZONE_SAVED_STATE_KEY = "GROW_ZONE_SAVED_STATE_KEY"
    }
}
