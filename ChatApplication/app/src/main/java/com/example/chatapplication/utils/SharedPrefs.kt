package com.example.chatapplication.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.chatapplication.models.User
import com.google.gson.Gson

class SharedPrefs(val context: Context){

    private var PRIVATE_MODE = Context.MODE_PRIVATE
    private val PREF_NAME = "chat-app"
    private var sharedPref: SharedPreferences ?= null

    fun contains():Boolean{
        getPrefs()
        if(sharedPref!!.contains("user"))
            return true
        return false
    }

    fun putUser(user: User){
        getPrefs()
        val userr = Gson().toJson(user)
        sharedPref?.edit{
            putString("user",userr)
            commit()
        }
    }

    fun getUser():User{
        getPrefs()
        val userstr = sharedPref!!.getString("user","")
        val usr = Gson().fromJson(userstr,User::class.java)
        return usr
    }

    fun removeUser(){
        getPrefs()
        sharedPref?.edit()?.remove("user")?.apply()
    }

    fun getPrefs(){
        if(sharedPref == null){
            sharedPref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        }
    }
}