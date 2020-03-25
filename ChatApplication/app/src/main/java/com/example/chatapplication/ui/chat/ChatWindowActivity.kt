package com.example.chatapplication.ui.chat

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.emoji.text.EmojiCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chatapplication.R
import com.example.chatapplication.models.ChatModel
import com.example.chatapplication.models.User
import com.example.chatapplication.ui.BaseActivity
import com.example.chatapplication.ui.adapters.ChatAdapter
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_chat_window.*
import java.io.File


class ChatWindowActivity : BaseActivity(), View.OnClickListener {

    private lateinit var currentUser: User
    private lateinit var user: User
    var chatArr = ArrayList<ChatModel>()
    var chatId: String? = null
    lateinit var adapter: ChatAdapter
    val IMAGE_FROM_GALLERY = 0
    val IMAGE_FROM_CAMERA = 2
    val VIDEO_FROM_GALLERY = 3
    var photoUri:Uri?=null
    var photoPath:String?=null
    var s3Client : AmazonS3Client?=null
    var transferUtility:TransferUtility?=null
    private val bucket = "chatt-app"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_window)
        setSupportActionBar(toolbar)

       this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        initEmojiCompat()

        user = intent.getParcelableExtra("user")!!

        currentUser = shared.getUser()
        if (intent.hasExtra("chatId")) {
            chatId = intent.getStringExtra("chatId")
        }
        if (chatId.isNullOrBlank()) {
            chatId = if (user.userId!! < currentUser.userId!!) {
                user.userId + "_" + currentUser.userId
            } else {
                currentUser.userId + "_" + user.userId
            }
        }

        userChatUName.text = user.userName

        if (!user.userImageUrl.isNullOrBlank()) {
            Glide.with(this).asBitmap().load(user.userImageUrl)
                .apply(RequestOptions().circleCrop()).into(userChatImg)
        }

        loadChat()

        adapter = ChatAdapter(this, chatArr, currentUser, user,chatId.toString())
        val llm = LinearLayoutManager(this)
        llm.stackFromEnd = true
        messagesView.layoutManager = llm
        messagesView.adapter = adapter

        message.setCommitListener(object :CustomEditText.CommitListener{
            override fun onCommitContent(uri: Uri,type:String) {
                saveSendSticker(uri,type)
            }
        })

        toolbar.setNavigationIcon(R.drawable.img_back_icon)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        sendBtn.setOnClickListener (this)
        attachBtn.setOnClickListener (this)
        emojiBtn.setOnClickListener(this)

        s3ClientProvider()
        setTransferUtility()

    }

    private fun saveSendSticker(uri:Uri,type:String) {
        val ref = stickerRef.child(uri.path!!)
        ref.putFile(uri)
            .addOnProgressListener {
                val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount).toInt()
                println("progress : $progress")
                Log.e("Sticker Uploading..","==>$progress")
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
            .addOnSuccessListener {
                Log.e("imageUrl", it.toString())
                chatSend(it.toString(),type)
                Log.e("image upload","Success $it")
            }
            .addOnFailureListener { it.printStackTrace()
                Log.e("image upload","Failed")}
    }

    private fun chatSend(msg: String, type: String) {
        if (msg.isNullOrBlank()) {
            return
        }
        val dateTime = System.currentTimeMillis().toString()
        val cht = ChatModel(currentUser.userId!!, msg, dateTime, type)
        message.setText("")
        message.clearFocus()
        val hm = HashMap<String, String>()
        hm["id"] = chatId!!
        hm["updatedTime"] = System.currentTimeMillis().toString()
        chatArr.clear()
        val id = chatRef.document(chatId!!).collection("messages").add(cht)
            .addOnSuccessListener {
                Log.e("message", "Sent : $msg")
                hm["lastmsg"] = it.id
                chatRef.document(chatId!!).set(hm)
                    .addOnSuccessListener { Log.e("LastSeen", "Updated") }
                    .addOnFailureListener { e ->
                        Log.e(
                            "LastSeen",
                            "NotUpdated : ${e.printStackTrace()}"
                        )
                    }
            }
            .addOnFailureListener { Log.e("message", "NotSent : ${it.printStackTrace()}") }
    }

    private fun loadChat() {

        chatRef.document(chatId!!).collection("messages")
            .orderBy("dateTime", Query.Direction.ASCENDING)
            .addSnapshotListener { querySnapshot, e ->
                if (e != null) {
                    e.printStackTrace()
                    return@addSnapshotListener
                }
                if (querySnapshot != null) {
                    chatArr.clear()
                    for (snapshot in querySnapshot) {
                        val deleted = snapshot.getString("deletedFor")
                        if (deleted != null) {
                            if (!deleted.contains(currentUser.userId!!)){
                                val chatModel = ChatModel(
                                    snapshot.getString("sender")!!,
                                    snapshot.getString("msgContent")!!,
                                    snapshot.getString("dateTime")!!,
                                    snapshot.getString("type")!!,
                                    snapshot.id)
                                chatArr.add(chatModel)
                            }
                        }else{
                            val chatModel = ChatModel(
                                snapshot.getString("sender")!!,
                                snapshot.getString("msgContent")!!,
                                snapshot.getString("dateTime")!!,
                                snapshot.getString("type")!!,
                                snapshot.id)
                            chatArr.add(chatModel)
                        }

                    }
                    }
                messagesView.scrollToPosition(adapter.itemCount-1)
                }
            }

    private fun pickImage() {
        checkReadPermission()
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_FROM_GALLERY)
    }
    private fun pickVideo() {
        checkReadPermission()
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "video/*"
        startActivityForResult(intent, VIDEO_FROM_GALLERY)
    }

    private fun saveSendImage(imageUri: Uri, name: String){
        val notifyManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel("newChannel",
                "newChannelName",
                NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "YOUR_NOTIFICATION_CHANNEL_DISCRIPTION"
            notifyManager.createNotificationChannel(channel)
            NotificationCompat.Builder(this,"newChannel")
        }else{
            NotificationCompat.Builder(this)
        }

        builder.setContentTitle("Image Uploading")
            .setContentText("Upload in Progress")
            .setSmallIcon(R.drawable.img_download_icon)
            .setProgress(100,0,false)
        notifyManager.notify(1,builder.build())
        val ref = imageRef.child(chatId!!).child(name)
        ref.putFile(imageUri)
            .addOnProgressListener {
                val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount).toInt()
                println("progress : $progress")
                builder.setProgress(100,progress,false)
                notifyManager.notify(1,builder.build())
                Toast.makeText(this,"Image Uploading..\n$progress",Toast.LENGTH_LONG).show()
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
            .addOnSuccessListener {
                builder.setContentText("Download completed")
                    .setProgress(0,0,false)
                notifyManager.notify(1, builder.build())
                Log.e("imageUrl", it.toString())
                chatSend(it.toString(),"image")
                Log.e("image upload","Success $it")
            }
            .addOnFailureListener { it.printStackTrace()
            Log.e("image upload","Failed")}
    }

    private fun captureImage(){
        val takePic = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if(takePic.resolveActivity(packageManager)!=null) {
            val photoName = "img_${System.currentTimeMillis()}"
            val photoFile = saveImage(photoName)
            if(photoFile!=null){
                photoUri = FileProvider.getUriForFile(this,"com.example.chatapplication.fileprovider",photoFile)
                takePic.putExtra(MediaStore.EXTRA_OUTPUT,photoUri)
            }
            startActivityForResult(takePic, IMAGE_FROM_CAMERA)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==IMAGE_FROM_GALLERY && resultCode == Activity.RESULT_OK && data!=null){
            val randomName = "img_${System.currentTimeMillis()}"
            saveSendImage(data.data!!, randomName)
        }
        if(requestCode==IMAGE_FROM_CAMERA &&  resultCode == Activity.RESULT_OK) {
            if (photoPath != null) {
                photoUri?.let {
                    saveSendImage(it, getFileName(photoPath!!))
                }
            }else {
                    Log.e("photoUpload", "FAILED")
                }
        }
        if(requestCode==VIDEO_FROM_GALLERY &&  resultCode == Activity.RESULT_OK) {
            val videoUri = data?.data
            uploadVideo(videoUri)
        }
    }


    private fun s3ClientProvider(){
        val cognitoCachngCredsProvider = CognitoCachingCredentialsProvider(
            applicationContext,
            resources.getString(R.string.identityPoolID),
            Regions.US_EAST_1
        )
        createAmazonS3Client(cognitoCachngCredsProvider)
    }

    private fun createAmazonS3Client(cognitoCachngCredsProvider: CognitoCachingCredentialsProvider) {
        s3Client = AmazonS3Client(cognitoCachngCredsProvider)
    }

    private fun setTransferUtility(){
        transferUtility = TransferUtility.builder()
            .context(applicationContext)
            .awsConfiguration(AWSMobileClient.getInstance().configuration)
            .s3Client(s3Client)
            .build()
    }

    private fun getFileName(filePath: File): String {
        return filePath.name
    }

    private fun getRealPathFromURI(uri: Uri?): String? {
        val cursor: Cursor? = contentResolver.query(uri!!, null, null, null, null)
        cursor?.moveToFirst()
        val index: Int? = cursor?.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
        return cursor?.getString(index!!)
    }


    private fun uploadVideo(videoUri: Uri?) {
        val imgPath = File(getRealPathFromURI(videoUri))
        val fileName = getFileName(imgPath)

        val notifyManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel("newChannel",
                "newChannelName",
                NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "YOUR_NOTIFICATION_CHANNEL_DISCRIPTION"
            notifyManager.createNotificationChannel(channel)
            NotificationCompat.Builder(this,"newChannel")
        }else{
            NotificationCompat.Builder(this)
        }

        builder.setContentTitle("Video Uploading")
            .setContentText("Upload in Progress")
            .setSmallIcon(R.drawable.img_download_icon)
            .setProgress(100,0,false)
        notifyManager.notify(1,builder.build())
        val observer =  transferUtility!!.upload(bucket, fileName, imgPath, CannedAccessControlList.PublicRead)!!
        observer.setTransferListener(object: TransferListener {
            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                val percentage = (bytesCurrent / bytesTotal * 100).toInt()
                Toast.makeText(applicationContext, "Progress in %$percentage", Toast.LENGTH_SHORT).show()
                builder.setProgress(100,percentage,false)
                notifyManager.notify(1,builder.build())
            }
            override fun onStateChanged(id: Int, state: TransferState) {
                Toast.makeText(applicationContext, "State Change$state", Toast.LENGTH_LONG).show()
                if(state == TransferState.COMPLETED){
                    val fileUrl = "https://$bucket.s3.amazonaws.com/$fileName"
                    chatSend(fileUrl,"video")
                    builder.setContentText("Upload completed")
                        .setProgress(0,0,false)
                    notifyManager.notify(1, builder.build())
                }else if(state == TransferState.CANCELED || state == TransferState.FAILED){
                    builder.setContentText("Upload Failed")
                        .setProgress(0,0,false)
                    notifyManager.notify(1, builder.build())
                }
            }
            override fun onError(id: Int, ex: Exception) {
                Log.e("error","Error")
                ex.printStackTrace()
                builder.setContentText("Upload ERROR")
                    .setProgress(0,0,false)
                notifyManager.notify(1, builder.build())
            }
        })
    }

    private fun saveImage(name:String): File? {
        val savedImgPath: String
        val storageDir = File(getExternalFilesDir(null).toString()+"/images")
        var flag = true
        if(!storageDir.exists()){
            flag = storageDir.mkdirs()
        }
            return if(flag){
                File.createTempFile(
                    name,
                    ".jpeg",
                    storageDir
                ).apply {
                    photoPath = absolutePath
                }
            }else{
                null
            }
    }

    override fun onClick(p0: View?) {
        when(p0){
            attachBtn->{
                val popup = PopupMenu(this,attachBtn)
                popup.menuInflater.inflate(R.menu.attach_menu,popup.menu)
                popup.setOnMenuItemClickListener {
                    when(it.itemId){
                        R.id.camera->{
                            if(checkCameraPermission()) {
                                captureImage()
                                Log.e("attach media", "camera")
                            }
                            true
                        }
                        R.id.gallery->{
                            Log.e("attach media","gallery")
                            pickImage()
                            true
                        }
                        R.id.video->{
                            Log.e("attach media","video")
                            pickVideo()
                            true
                        }
                        else ->
                            true
                    }
                }
                popup.show()
            }
            sendBtn->{
                val msg = EmojiCompat.get().process(message.text.toString())
                chatSend(msg as String, "text")}

        }
    }

    fun selectMode(){
        deleteBtn.visibility = View.VISIBLE
        deleteBtn.setOnClickListener {
            adapter.delete()
            endSelectMode()
            adapter.endSelectableMode()
        }
    }

    fun endSelectMode(){
        deleteBtn.visibility = View.GONE
    }

    override fun onBackPressed() {

        if(deleteBtn.visibility == View.VISIBLE){
            endSelectMode()
            adapter.endSelectableMode()
        }else {
            super.onBackPressed()
        }
    }
    private fun getFileName(url:String): String {
        val start = url.lastIndexOf("img")
        val end = url.indexOf(".jpeg")
        return url.substring(start, end)
    }
}
