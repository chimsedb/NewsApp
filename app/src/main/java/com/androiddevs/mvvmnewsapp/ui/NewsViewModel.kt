package com.androiddevs.mvvmnewsapp.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androiddevs.mvvmnewsapp.models.Article
import com.androiddevs.mvvmnewsapp.models.NewsResponse
import com.androiddevs.mvvmnewsapp.repository.NewsRepository
import com.androiddevs.mvvmnewsapp.util.Resource
import kotlinx.coroutines.launch
import retrofit2.Response

class NewsViewModel(
    val app: Application,
    val newsRespository: NewsRepository
) : AndroidViewModel(app) {
    val breakingNews: MutableLiveData<Resource<NewsResponse>> = MutableLiveData()
    var breakingNewsPage = 1
    var breakingNewsResponse: NewsResponse? = null

    val searchNews: MutableLiveData<Resource<NewsResponse>> = MutableLiveData()
    var searchNewsPage = 1

    init {
        getBreakingNews("us")
    }

    fun getBreakingNews(countryCode: String) = viewModelScope.launch {
        breakingNews.postValue(Resource.Loading())
        if (isNetworkConnected()) {
            val response = newsRespository.getBreakingNews(countryCode, breakingNewsPage)
            breakingNews.postValue(handleBreakingNewsResponse(response))
        } else {
            breakingNews.postValue(Resource.Error("Network not found!"))
        }
    }

    private fun handleBreakingNewsResponse(response: Response<NewsResponse>): Resource<NewsResponse>? {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->

                breakingNewsPage++
                if (breakingNewsResponse == null) {
                    breakingNewsResponse = resultResponse
                } else {
                    val oldResponse = breakingNewsResponse?.articles
                    val newResponse = resultResponse?.articles
                    oldResponse?.addAll(newResponse)
                }

                return Resource.Success(breakingNewsResponse ?: resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    fun searchNews(q: String) = viewModelScope.launch {
        searchNews.postValue(Resource.Loading())
        if (isNetworkConnected()) {
            val response = newsRespository.searchBreakingNews(q, searchNewsPage)
            searchNews.postValue(handleSearchNews(response))
        } else {
            searchNews.postValue(Resource.Error("Network not found!"))
        }
    }

    private fun handleSearchNews(response: Response<NewsResponse>): Resource<NewsResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                return Resource.Success(resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    fun saveArticle(article: Article) = viewModelScope.launch {
        newsRespository.insertArticle(article)
    }

    fun deleteArticle(article: Article) = viewModelScope.launch {
        newsRespository.deleteArticle(article)
    }

    fun getSaveArticle() = newsRespository.getSaveArticle()

    private fun isNetworkConnected(): Boolean {
        val connectivityManager: ConnectivityManager =
            app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val capabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                if (capabilities != null) {
                    when {
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                            return true
                        }
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                            return true
                        }
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }
}