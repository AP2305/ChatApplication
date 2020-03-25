package com.example.chatapplication.ui.login_register

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chatapplication.R
import com.example.chatapplication.models.User
import com.example.chatapplication.ui.BaseActivity
import com.example.chatapplication.ui.DashboardActivity
import kotlinx.android.synthetic.main.activity_user_details.*

class UserDetailsActivity : BaseActivity() {

    var imageUri: Uri?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_details)

        userImage.setOnClickListener{
            pickImage()
        }

        nextBtn.setOnClickListener {
            if (unameLayout.editText?.text.isNullOrBlank()) {
                unameLayout.error = "Please Enter UserName"
            } else {
                progressLayout.visibility = View.VISIBLE
                blackLayout.visibility = View.VISIBLE
                saveUserData()
            }
        }
    }

    private fun saveUserData(){
        stopTouch()
        if(imageUri!=null) {
            val ref = profileRef.child(authUser!!.uid)
            ref.putFile(imageUri!!)
                .addOnProgressListener {
                    val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount).toInt()
                    progresstext.text = "$progress %\nPlease Wait"
                    println("progress : $progress")
                }
                .continueWithTask {
                    if (!it.isSuccessful) {
                        it.exception?.let {e->
                            e.printStackTrace()
                            throw e
                        }
                    }
                    ref.downloadUrl
                }
                .addOnSuccessListener { task ->
                    Log.e("Image Status", "Image Uploaded")
                    val map = HashMap<String, String>()
                    map["name"] = unameLayout.editText?.text.toString()
                    map["image"] = task.toString()
                    Log.e("Image upload", "Upload Successful")
                    firestore.collection("users")
                        .document(authUser.uid)
                        .set(map)
                        .addOnCompleteListener {
                            Log.e("Data upload", "Upload Successful")
                            shared.putUser(User(authUser.uid,unameLayout.editText?.text.toString(),task.toString(),authUser.phoneNumber))
                            val intent = Intent(this, DashboardActivity::class.java)
                            startActivity(intent)
                        }
                }.addOnFailureListener { e ->
                    Log.e("Image upload", "Upload Unsuccessful")
                    e.printStackTrace()
                }
        }else{
            val map = HashMap<String, String>()
            map["name"] = unameLayout.editText?.text.toString()
            Log.e("Image upload", "Upload Successful")
            firestore.collection("users")
                .document(authUser?.uid.toString())
                .set(map)
                .addOnCompleteListener {
                    Log.e("Data upload", "Upload Successful")
                    shared.putUser(User(authUser?.uid,unameLayout.editText?.text.toString(),"",authUser?.phoneNumber))
                    val intent = Intent(this, DashboardActivity::class.java)
                    startActivity(intent)
                }
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
            Glide.with(this).asBitmap().load(imageUri).apply(RequestOptions().circleCrop()).into(userImage)
        }
    }
}
