package com.codewithkael.parentalmonitoring.utils

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    fun providesContext(@ApplicationContext context: Context) = context

    @Provides
    fun provideGson():Gson= Gson()

    @Singleton
    @Provides
    fun provideFusedProvider(context: Context) : FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
}