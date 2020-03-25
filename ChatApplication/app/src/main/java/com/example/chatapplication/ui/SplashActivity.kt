package com.example.chatapplication.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.chatapplication.R
import com.example.chatapplication.ui.login_register.RegisterActivity
import com.example.chatapplication.ui.login_register.UserDetailsActivity

class SplashActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

    }

    override fun onStart() {
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS),0)
        }else{
            getData()
        }
        super.onStart()
    }

    fun getData(){
        if(authUser!=null&& shared.contains()){
            val user = shared.getUser()
            userRef.document(user.userId!!).get().addOnSuccessListener {
                if (it.exists()) {
                    if (it.getString("name").isNullOrBlank()) {
                        startActivity(Intent(this, UserDetailsActivity::class.java))
                        finish()
                    } else {
                        user.userName = it.getString("name").toString()
                        if(!it.getString("image").isNullOrBlank()){
                            user.userImageUrl = it.getString("image")!!
                        }
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    }
                } else {
                    startActivity(Intent(this, UserDetailsActivity::class.java))
                    finish()
                }
            }
        }else{
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            0->{
                if(grantResults[0]!=PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Please grant Permissions to Continue", Toast.LENGTH_SHORT).show()
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS),0)
                }else{
                    getData()
                }
            }
        }

    }
}
