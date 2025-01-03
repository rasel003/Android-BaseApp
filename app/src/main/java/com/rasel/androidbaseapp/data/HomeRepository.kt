package com.rasel.androidbaseapp.data

import android.os.Build
import androidx.lifecycle.asFlow
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.rasel.androidbaseapp.cache.entities.Plant
import com.rasel.androidbaseapp.data.source.HomeDataSourceFactory
import com.rasel.androidbaseapp.remote.models.PostItem
import com.rasel.androidbaseapp.remote.models.UnsplashSearchResponse
import com.rasel.androidbaseapp.util.ApiResponse
import com.rasel.androidbaseapp.workers.GetPostWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject

private const val MINIMUM_INTERVAL_DASHBOARD = 2


class HomeRepository @Inject constructor(
    private val dataSourceFactory: HomeDataSourceFactory,
    private val workManager: WorkManager
) {
    fun getPostList(): Flow<ApiResponse<List<PostItem>>> = dataSourceFactory.getRemoteDataSource().getPostList()

    suspend fun getPost(): Flow<Pair<String,String>> {
        val getPostWorkRequest: OneTimeWorkRequest = OneTimeWorkRequest.Builder(GetPostWorker::class.java).build()
        workManager.enqueue(getPostWorkRequest)
        return workManager.getWorkInfoByIdLiveData(getPostWorkRequest.id).asFlow().map {
            if (it != null && it.state == WorkInfo.State.SUCCEEDED) {
                val res = it.outputData.getString("responseString") ?: ""
                Pair("Success",res)
            } else {
                Pair("Failure","")
            }
        }
    }

    suspend fun getCurrentUserAvailableLeave(token: String) {
        val lastSavedAt = dataSourceFactory.getCacheDataSource().getSavedValue()

        if (lastSavedAt == null || isFetchNeeded(lastSavedAt)) {
            dataSourceFactory.getRemoteDataSource().getDashboardData(token)
        }
    }

    private fun getSavedTimeKey(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now().toString()
        } else {
            Date().time.toString()
        }
    }

    //checking if last fetched time exceeded the interval time
    private fun isFetchNeeded(savedAt: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ChronoUnit.MINUTES.between(
                LocalDateTime.parse(savedAt),
                LocalDateTime.now()
            ) >= MINIMUM_INTERVAL_DASHBOARD
        } else {
            val interVal = Date().time - savedAt.toLong()
            interVal >= MINIMUM_INTERVAL_DASHBOARD * 1000
        }
    }

    fun getPlant(plantId: String) = dataSourceFactory.getCacheDataSource().getPlant(plantId)
    fun getPlants(): Flow<List<Plant>> = dataSourceFactory.getCacheDataSource().getPlants()
    suspend fun getDataFromUnSplash2(params: String): Flow<UnsplashSearchResponse> =
        dataSourceFactory.getRemoteDataSource().getDataFromUnSplash2(params)

    fun getPlantsWithGrowZoneNumber(zone: Int): Flow<List<Plant>> = dataSourceFactory.getCacheDataSource().getPlantsWithGrowZoneNumber(zone)
    fun getSearchResultStream(queryString: String) = dataSourceFactory.getRemoteDataSource().getSearchResultStream(queryString)
}