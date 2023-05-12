package com.codewithkael.parentalmonitoring.utils

enum class DataModelType{
    WrongJsonType, SignIn, SignInSuccess, UserExists, StartWatching,EndCall,WrongPassword,StartLocating,
    FailedToFindUser, Offer, Answer, IceCandidate,UserFoundSuccessfully,SwitchCamera,Location
}

data class DataModel(
    val type:DataModelType?=null,
    val username:String,
    val target:String?=null,
    val data:Any?=null
)
