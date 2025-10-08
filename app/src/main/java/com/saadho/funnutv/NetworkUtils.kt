package com.saadho.funnutv

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object NetworkUtils {
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
    
    fun getNetworkType(context: Context): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                // Check if it's 5G, 4G, or slower
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    when (activeNetwork.getSignalStrength()) {
                        in -50..-30 -> NetworkType.CELLULAR_5G
                        in -80..-50 -> NetworkType.CELLULAR_4G
                        else -> NetworkType.CELLULAR_3G
                    }
                } else {
                    NetworkType.CELLULAR_4G // Default to 4G for older versions
                }
            }
            else -> NetworkType.NONE
        }
    }
    
    fun isHighQualityNetwork(context: Context): Boolean {
        return when (getNetworkType(context)) {
            NetworkType.WIFI, NetworkType.ETHERNET, NetworkType.CELLULAR_5G -> true
            else -> false
        }
    }
    
    enum class NetworkType {
        NONE, WIFI, ETHERNET, CELLULAR_5G, CELLULAR_4G, CELLULAR_3G
    }
}
