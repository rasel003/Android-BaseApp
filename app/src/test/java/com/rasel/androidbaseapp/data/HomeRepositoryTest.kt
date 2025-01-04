package com.rasel.androidbaseapp.data

import androidx.work.WorkManager
import com.rasel.androidbaseapp.data.repository.HomeDataSource
import com.rasel.androidbaseapp.data.source.HomeDataSourceFactory
import com.rasel.androidbaseapp.fakes.FakeValueFactory
import com.rasel.androidbaseapp.remote.models.PostItem
import com.rasel.androidbaseapp.util.ApiResponse
import com.rasel.androidbaseapp.utils.DataBaseTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class HomeRepositoryTest : DataBaseTest() {

    @Mock
    lateinit var dataSourceFactory: HomeDataSourceFactory

    @Mock
    lateinit var workManager: WorkManager

    @Mock
    lateinit var dataSource: HomeDataSource

    lateinit var sut: HomeRepository

    @Before
    fun setUp() {
        sut = HomeRepository(
            dataSourceFactory,
            workManager = workManager
        )
    }

    @Test
    fun testGetProducts_EmptyList() = runTest {
        Mockito.`when`(dataSourceFactory.getRemoteDataSource()).thenReturn(dataSource)
        Mockito.`when`(dataSource.getPostList()).thenReturn(flow { emit(ApiResponse.Success(emptyList())) })

        val result = sut.getPostList().first()
        Assert.assertEquals(true, result is ApiResponse.Success<*>)
        Assert.assertEquals(0, (result as ApiResponse.Success).data.size)
    }

    @Test
    fun testGetProducts_expectedProductList() = runTest {
        val productList = listOf(
            PostItem(1, "Prod 1", FakeValueFactory.randomString(), FakeValueFactory.randomInt()),
            PostItem(
                2,
                FakeValueFactory.randomString(),
                FakeValueFactory.randomString(),
                FakeValueFactory.randomInt()
            )
        )
        Mockito.`when`(dataSourceFactory.getRemoteDataSource()).thenReturn(dataSource)
        Mockito.`when`(dataSource.getPostList()).thenReturn(flow { emit(ApiResponse.Success(productList)) })

        val result = sut.getPostList().first()
        Assert.assertEquals(true, result is ApiResponse.Success<*>)
        Assert.assertEquals(2, (result as ApiResponse.Success).data.size)
        Assert.assertEquals("Prod 1", result.data[0].title)
    }

      @Test
      fun testGetProducts_expectedError() = runTest {
          Mockito.`when`(dataSourceFactory.getRemoteDataSource()).thenReturn(dataSource)
          Mockito.`when`(dataSource.getPostList()).thenReturn(flow { emit(ApiResponse.Failure("Connection Exception" , 400)) })

          val result = sut.getPostList().first()
          Assert.assertEquals(true, result is ApiResponse.Failure)
          Assert.assertEquals("Connection Exception", (result as ApiResponse.Failure).errorMessage)
      }
}