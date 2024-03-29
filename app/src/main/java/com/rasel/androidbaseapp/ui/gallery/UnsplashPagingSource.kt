package com.rasel.androidbaseapp.ui.gallery

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rasel.androidbaseapp.remote.api.MyApi
import com.rasel.androidbaseapp.data.models.UnsplashPhoto

private const val UNSPLASH_STARTING_PAGE_INDEX = 0

class UnsplashPagingSource(
    private val api: MyApi,
    private val query: String
) : PagingSource<Int, UnsplashPhoto>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UnsplashPhoto> {
        val page = params.key ?: UNSPLASH_STARTING_PAGE_INDEX
        return try {
            val response = api.searchPhotos(query, page, params.loadSize)
            val photos = response.results
            LoadResult.Page(
                data = photos,
                prevKey = if (page == UNSPLASH_STARTING_PAGE_INDEX) null else page - 1,
                nextKey = if (page == response.totalPages) null else page + 1
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, UnsplashPhoto>): Int? {
        return 0
    }
}
