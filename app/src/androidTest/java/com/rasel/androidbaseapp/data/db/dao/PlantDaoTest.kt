package com.rasel.androidbaseapp.data.db.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.rasel.androidbaseapp.cache.dao.PlantDao
import com.rasel.androidbaseapp.cache.database.AppDatabase
import com.rasel.androidbaseapp.cache.entities.Plant
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.*
import javax.inject.Inject

@HiltAndroidTest
class PlantDaoTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val hiltAndroidRule = HiltAndroidRule(this)

    @Inject
    lateinit var appDatabase: AppDatabase
    private lateinit var plantDao: PlantDao

    @Before
    fun setUp() {
        hiltAndroidRule.inject()
        plantDao = appDatabase.getPlantDao()
    }

    @Test
    fun insertProduct_returnsSingleProduct() = runTest {
        val product = Plant(
            "333", "", "dfd", 256, 12, "Test Product",
            commentsCount = "3",
            bookmarked = false
        )
        plantDao.insertAll(listOf(product))
        val result = plantDao.getPlantList()
        Assert.assertEquals(1, result.size)
    }


    @After
    fun closeDatabase() {
        appDatabase.close()
    }
}