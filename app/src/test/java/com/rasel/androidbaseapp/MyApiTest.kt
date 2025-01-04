package com.rasel.androidbaseapp

import com.rasel.androidbaseapp.remote.api.MyApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MyApiTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var myApi: MyApi

    @Before
    fun setup(){
        mockWebServer = MockWebServer()
        myApi = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(MyApi::class.java)
    }

    @Test
    fun testGetProducts() = runTest{
        val mockResponse = MockResponse()
        mockResponse.setBody("[]")
        mockWebServer.enqueue(mockResponse)

        val response = myApi.getPostList()
        mockWebServer.takeRequest()

        Assert.assertEquals(true, response.body()?.isEmpty() ?: false)
    }

    @Test
    fun testGetProducts_returnProducts() = runTest{
        val mockResponse = MockResponse()
        val content = Helper.readFileResource("/response.json")
        mockResponse.setResponseCode(200)
        mockResponse.setBody(content)
        mockWebServer.enqueue(mockResponse)

        val response = myApi.getPostList()
        mockWebServer.takeRequest()

        Assert.assertEquals(false, response.body()?.isEmpty())
        Assert.assertEquals(4, response.body()?.size ?: 0)
    }

    /*@Test
    fun testGetProducts_returnError() = runTest{
        val mockResponse = MockResponse()
        mockResponse.setResponseCode(404)
        mockResponse.setBody("Something went wrong")
        mockWebServer.enqueue(mockResponse)

        val response = myApi.getPostList()
        mockWebServer.takeRequest()

        Assert.assertEquals(false, response.isSuccessful)
        Assert.assertEquals(404, response.code())
    }*/

    /*@Test
    fun testGetProducts_EmptyList() = runTest {
        val mockResponse = MockResponse()
        mockWebServer.enqueue(mockResponse.setResponseCode(404))
        val sut = HomeRepository(myApi)
        val result = sut.getPostList()
        val request = mockWebServer.takeRequest()

        Assert.assertEquals(true, result is Resource.Failure)
        Assert.assertEquals(0, result.data!!.size)
    }*/

    @After
    fun tearDown(){
        mockWebServer.shutdown()
    }
}












