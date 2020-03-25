package com.example.chatapplication.ui.login_register

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapplication.R
import com.example.chatapplication.ui.BaseActivity
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : BaseActivity() {

    private var verificationInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        if(intent.hasExtra("verificationInProgress")) {
            verificationInProgress = false
        }

        registerBtn.setOnClickListener {
            register()
        }
    }


    private fun register(){
        if(validation()){
            var intent = Intent(this,VerifyCodeActivity::class.java)
            intent.putExtra("phoneNumber",countryCode.selectedCountryCodeWithPlus.toString() + mobileNoLayout.editText?.text.toString())
            verificationInProgress = true
            startActivity(intent)
        }
    }

    private fun validation():Boolean{
        if(mobileNoLayout.editText?.text.isNullOrBlank()) {
            mobileNoLayout.error = "Invalid PhoneNumber"
            return false
        }else if(!Patterns.PHONE.matcher(mobileNoLayout.editText?.text).matches()){
            Toast.makeText(this, "Invalid Phone Number", Toast.LENGTH_SHORT).show()
            return false
        }
        else{
            return true
        }
    }


    override fun onSaveInstanceState(savedInstanceState:Bundle){
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBoolean("verification",verificationInProgress)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        verificationInProgress = savedInstanceState.getBoolean("verification")
    }

    override fun onStart() {
        super.onStart()
        if(verificationInProgress){
            register()
        }
    }

    override fun onBackPressed() {
        exitDialog()
    }
}
