package com.example.chatapplication.ui.userProfile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chatapplication.R
import com.example.chatapplication.models.User
import com.example.chatapplication.ui.BaseActivity
import com.example.chatapplication.ui.DashboardActivity
import kotlinx.android.synthetic.main.activity_user_profile.*

class UserProfileActivity : BaseActivity() {

    var user : User?= null
    var imageUri: Uri?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

         user = shared.getUser()

        if(user!!.userName!!.isBlank()){
            userRef.document(user!!.userId!!).get().addOnSuccessListener {
                if(it.exists()&&it.contains("name")){
                    user!!.userName = it.getString("name")!!
                    userProfileNameLayout.editText?.setText(user!!.userName)
                }
            }
        }else{
            userProfileNameLayout.editText?.setText(user!!.userName)
        }

        userPhNummberLayout.editText?.setText(user!!.phoneNumber)

        if(user!!.userImageUrl!!.isBlank()){
            userRef.document(user!!.userId!!).get().addOnSuccessListener {
                if(it.exists()&&it.contains("image")){
                    user!!.userImageUrl = it.getString("image")!!
                    Glide.with(this).asBitmap().load(user!!.userImageUrl).apply(RequestOptions().circleCrop()).into(userProfileImage)
                }
            }
        }else{
            Glide.with(this).asBitmap().load(user!!.userImageUrl).apply(RequestOptions().circleCrop()).into(userProfileImage)
        }

        userProfileImage.setOnClickListener {
            pickImage()
        }
        editBtn.setOnClickListener {
            saveData()
        }

    }

    private fun pickImage(){
        if(checkReadPermission()) {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==0 && resultCode == Activity.RESULT_OK && data!=null){
            imageUri = data.data
            Glide.with(this).asBitmap().load(imageUri).apply(RequestOptions().circleCrop()).into(userProfileImage)
        }
    }

    private fun saveData(){
        stopTouch()
        progressLayout.visibility = View.VISIBLE
        blackLayout.visibility = View.VISIBLE
        val ref = profileRef.child(authUser!!.uid)
        if(imageUri!=null) {
            ref.putFile(imageUri!!)
                .addOnProgressListener {
                    val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount).toInt()
                    progresstext.text = "$progress %\nPlease Wait"
                    println("progress : $progress")
                }
                .continueWithTask {
                    if (!it.isSuccessful) {
                        it.exception?.let { e ->
                            e.printStackTrace()
                            throw e
                        }
                    }
                    ref.downloadUrl
                }
                .addOnSuccessListener { task ->
                    Log.e("Image Status", "Image Uploaded")
                    Log.e("Image upload", "Upload Successful")
                    firestore.collection("users")
                        .document(user!!.userId!!)
                        .update("image", task.toString())
                        .addOnSuccessListener {
                            Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show()
                            user!!.userImageUrl = task.toString()
                            shared.putUser(user!!)
                        }
                    startTouch()
                }.addOnFailureListener {
                    if (!user!!.userImageUrl.isNullOrBlank())
                        Glide.with(this).asBitmap().load(user!!.userImageUrl).apply(RequestOptions().circleCrop()).into(
                            userProfileImage
                        )
                    Toast.makeText(this, "Profile can't Updated", Toast.LENGTH_SHORT).show()
                    startTouch()
                }.addOnCompleteListener {
                    progressLayout.visibility = View.GONE
                    blackLayout.visibility = View.VISIBLE
                    if(!user!!.userName!!.equals(userProfileNameLayout.editText!!.text)){
                        firestore.collection("users")
                            .document(user!!.userId!!)
                            .update("name", userProfileNameLayout.editText!!.text.toString())
                            .addOnSuccessListener {
                                Toast.makeText(this, "UserName Updated", Toast.LENGTH_SHORT).show()
                                user!!.userName = userProfileNameLayout.editText!!.text.toString()
                                shared.putUser(user!!)
                            }.addOnCompleteListener {
                                startActivity(Intent(this,
                                    DashboardActivity::class.java))
                                finish()
                            }
                    }else{
                        startActivity(Intent(this,
                            DashboardActivity::class.java))
                        finish()
                    }
                }
        }
        else if(!user!!.userName!!.equals(userProfileNameLayout.editText!!.text)){
            firestore.collection("users")
                .document(user!!.userId!!)
                .update("name", userProfileNameLayout.editText!!.text.toString())
                .addOnSuccessListener {
                    Toast.makeText(this, "UserName Updated", Toast.LENGTH_SHORT).show()
                    user!!.userName = userProfileNameLayout.editText!!.text.toString()
                    shared.putUser(user!!)
                }
        }
    }

}
