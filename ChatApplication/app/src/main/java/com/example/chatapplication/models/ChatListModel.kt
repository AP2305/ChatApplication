package com.example.chatapplication.models

data class ChatListModel(val user:User, val chatId: String, val lastMsg: String? ="", val lastSeen: String? ="")