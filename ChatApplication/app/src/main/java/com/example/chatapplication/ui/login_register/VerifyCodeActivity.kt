package com.example.chatapplication.ui.login_register

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.chatapplication.R
import com.example.chatapplication.models.User
import com.example.chatapplication.ui.BaseActivity
import com.example.chatapplication.ui.DashboardActivity
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.android.synthetic.main.activity_verify_code.*
import java.util.concurrent.TimeUnit

class VerifyCodeActivity : BaseActivity() {

    var verificationId = ""
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    var verificationInProgress = false
    var phoneNo = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_code)

        phoneNo = intent.getStringExtra("phoneNumber")
        register()

        verifyBtn.setOnClickListener {
            verifyCode(verificationId,verifyLayout.editText?.text.toString())
        }

    }


    private fun register(){
        setCallbacks()
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNo, 60, TimeUnit.SECONDS, this, callbacks)
        verificationInProgress = true
    }

    private fun setCallbacks(){
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                verificationInProgress = false
                Log.d("verification completed", "onVerificationCompleted:$credential")

                if(credential.smsCode != null){
                    verifyLayout.editText?.setText(credential.smsCode)
                }
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                verificationInProgress = false
                Log.e("verification failed", "onVerificationFailed:",exception)

                if(exception is FirebaseAuthInvalidCredentialsException){
                    Toast.makeText(this@VerifyCodeActivity,"Invalid Phone Number", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@VerifyCodeActivity,RegisterActivity::class.java)
                    intent.putExtra("verificationInProgress",false)
                    startActivity(intent)
                }else if(exception is FirebaseTooManyRequestsException){
                    Toast.makeText(this@VerifyCodeActivity,"Too many Requests", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@VerifyCodeActivity,RegisterActivity::class.java)
                    intent.putExtra("verificationInProgress",false)
                    startActivity(intent)
                }
                startTouch()
            }

            override fun onCodeSent(verifyId: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = verifyId
                resendToken = token
            }
        }
    }

    private fun verifyCode(verifyId:String,code:String){
        stopTouch()
        try {
            val credential = PhoneAuthProvider.getCredential(verifyId,code)
            signInWithPhoneAuthCredential(credential)
        }catch (e:Exception){
            Log.e("phoneAuthCredential","Wrong Code")
            e.printStackTrace()
            startTouch()
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener{
                if(it.isSuccessful){
                    Toast.makeText(this,"Logged IN", Toast.LENGTH_SHORT).show()
                    val userid = it.result?.user?.uid
                    userRef.document(userid!!).get().addOnSuccessListener {snapshot->
                        if(snapshot.contains("name")){
                            val name = snapshot.getString("name")
                            var image = ""
                            if(snapshot.contains("image")){
                                image = snapshot.getString("image")!!
                            }
                            shared.putUser(User(userid,name ,image,phoneNo))
                            startActivity(Intent(this, DashboardActivity::class.java))
                        }else{
                            startActivity(Intent(this, UserDetailsActivity::class.java))
                        }
                    }.addOnFailureListener{
                        shared.putUser(User(userid,"","",phoneNo))
                        startActivity(Intent(this, UserDetailsActivity::class.java))
                    }
                }else{
                    Log.e("signin","Failed",it.exception)
                    if(it is FirebaseAuthInvalidCredentialsException) {
                        verifyLayout.error = "Invalid Code"
                    }
                    startTouch()
                }
            }
    }

    override fun onBackPressed() {
        val intent = Intent(this,RegisterActivity::class.java)
        intent.putExtra("verificationInProgress",false)
        startActivity(intent)
        finish()
    }

}
