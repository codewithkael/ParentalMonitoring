package com.codewithkael.parentalmonitoring

import android.util.Log
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)

    }

    @Test
    fun `test if merge works`(){
        val strList = listOf("masoud","ali","mohammad","reza","amir")
        val strObservable = Observable.fromIterable(strList)
        val intObservable = Observable.range(1,5)

        val zip = Observable.zip(strObservable,intObservable){ a,b->
            return@zip a+b
        }

        val observer = object : Observer<Any> {
            override fun onSubscribe(d: Disposable) {
            }

            override fun onError(e: Throwable) {
                Log.d(TAG, "main: onError $e")
            }

            override fun onComplete() {
                Log.d(TAG, "main: onComplete")

            }

            override fun onNext(t: Any) {
                Log.d(TAG, "main: onNext $t")
                assertTrue(t is String)
            }

        }

        zip.subscribeOn(Schedulers.io())
            .subscribe(observer)

    }
}