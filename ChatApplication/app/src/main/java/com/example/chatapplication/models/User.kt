package com.example.chatapplication.models

import android.os.Parcel
import android.os.Parcelable

class User(var userId:String?="",var userName:String?="",var userImageUrl:String?="",val phoneNumber:String?="") :Parcelable{
    constructor(parcel: Parcel) : this(parcel.readString(), parcel.readString(), parcel.readString(), parcel.readString())

    override fun writeToParcel(p0: Parcel?, p1: Int) {
        p0?.writeString(userId)
        p0?.writeString(userName)
        p0?.writeString(userImageUrl)
        p0?.writeString(phoneNumber)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }
        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }


}