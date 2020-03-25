package com.example.chatapplication.ui.userProfile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chatapplication.R
import com.example.chatapplication.models.User
import com.example.chatapplication.ui.BaseFragment
import kotlinx.android.synthetic.main.activity_user_profile.view.*

class UserProfileFragment : BaseFragment() {

    private var user: User?=null
    private lateinit var mView: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mView = inflater.inflate(R.layout.fragment_user_profile, container, false)

        user = shared.getUser()

        if(user!!.userName!!.isBlank()){
            userRef.document(user!!.userId!!).get().addOnSuccessListener {
                if(it.exists()&&it.contains("name")){
                    user!!.userName = it.getString("name")!!
                    mView.userProfileNameLayout.editText?.setText(user!!.userName)
                }
            }
        }else{
            mView.userProfileNameLayout.editText?.setText(user!!.userName)
        }

        mView.userPhNummberLayout.editText?.setText(user!!.phoneNumber)

        if(user!!.userImageUrl!!.isBlank()){
            userRef.document(user!!.userId!!).get().addOnSuccessListener {
                if(it.exists()&&it.contains("image")){
                    user!!.userImageUrl = it.getString("image")!!
                    Glide.with(this).asBitmap().load(user!!.userImageUrl).apply(RequestOptions().circleCrop()).into(mView.userProfileImage)
                }
            }
        }else{
            Glide.with(this).asBitmap().load(user!!.userImageUrl).apply(RequestOptions().circleCrop()).into(mView.userProfileImage)
        }

        mView.editBtn.setOnClickListener {
            startActivity(Intent(mContext,UserProfileActivity::class.java))
        }
        return mView
    }

}
