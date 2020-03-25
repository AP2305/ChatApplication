package com.example.chatapplication.ui.chat

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat

class CustomEditText : androidx.appcompat.widget.AppCompatEditText {

    private var commitListener: CommitListener? =null

    constructor(context: Context) : super(context)

    constructor(context: Context,attrs: AttributeSet) : super(context,attrs)

    constructor(context: Context,attrs:AttributeSet,defStyleAttr:Int):super(context, attrs, defStyleAttr)

    override fun onCreateInputConnection(editorInfo:EditorInfo): InputConnection {
        val ic: InputConnection = super.onCreateInputConnection(editorInfo)
        EditorInfoCompat.setContentMimeTypes(editorInfo, arrayOf("image/*"))

        val callback = InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, opts ->
                val lacksPermission = (flags and  InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && lacksPermission) {
                    try {
                        inputContentInfo.requestPermission()
                    } catch (e: Exception) {
                        return@OnCommitContentListener false // return false if failed
                    }
                    Log.e("gif",inputContentInfo.contentUri.toString())
                }
                if(inputContentInfo.contentUri.toString().contains(".png")) {
                    commitListener?.onCommitContent(inputContentInfo.contentUri,"sticker")
                }else{
                    commitListener?.onCommitContent(inputContentInfo.contentUri,"gif")
                }
                true
            }
        return InputConnectionCompat.createWrapper(ic, editorInfo, callback)
    }

    fun setCommitListener(listener:CommitListener){
        this.commitListener = listener
    }

    interface CommitListener{
        fun onCommitContent(uri: Uri,type:String)
    }

}