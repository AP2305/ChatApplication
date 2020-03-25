package com.example.chatapplication.ui

import android.content.Context
import android.util.Log
import androidx.core.provider.FontRequest
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.FontRequestEmojiCompatConfig
import androidx.fragment.app.Fragment
import com.example.chatapplication.R
import com.example.chatapplication.utils.SharedPrefs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

open class BaseFragment : Fragment(){

    lateinit var mContext:Context
    val firebaseAuth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val firebaseStorage = FirebaseStorage.getInstance()
    val authUser = firebaseAuth.currentUser
    val userRef = firestore.collection("users")
    val chatRef = firestore.collection("chats")
    val profileRef = firebaseStorage.getReference("profilePics")
    val imageRef = firebaseStorage.getReference("images")
    val videoRef = firebaseStorage.getReference("videos")
    lateinit var shared: SharedPrefs


    override fun onAttach(context: Context) {

        mContext = context
        shared = SharedPrefs(context)

        super.onAttach(context)
    }


    private val USE_BUNDLED_EMOJI = true

//    fun initEmojiCompat(){
//        val config: EmojiCompat.Config
//        if (USE_BUNDLED_EMOJI) {
//            // Use the bundled font for EmojiCompat
//            config = BundledEmojiCompatConfig(context!!)
//        } else {
//            // Use a downloadable font for EmojiCompat
//            val fontRequest = FontRequest(
//                "com.google.android.gms.fonts",
//                "com.google.android.gms",
//                "Noto Color Emoji Compat",
//                R.array.com_google_android_gms_fonts_certs)
//            config = FontRequestEmojiCompatConfig(context!!, fontRequest)
//                .setReplaceAll(true)
//                .registerInitCallback(object : EmojiCompat.InitCallback() {
//                    override fun onInitialized() {
//                        Log.i("EmojiCompat", "EmojiCompat initialized")
//                    }
//
//                    override fun onFailed(throwable: Throwable?) {
//                        Log.e("EmojiCompat", "EmojiCompat initialization failed", throwable)
//                    }
//                })
//        }
//        EmojiCompat.init(config)
//    }

}