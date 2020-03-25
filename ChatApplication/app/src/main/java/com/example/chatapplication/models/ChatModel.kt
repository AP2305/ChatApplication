package com.example.chatapplication.models

class ChatModel
    (
    val sender:String,
    val msgContent:String,
    val dateTime:String,
    val type:String,
    val chatItemId:String?= null,
    val deletedFor:String?=null
)