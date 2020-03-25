package com.example.chatapplication.ui.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.chatapplication.R
import com.example.chatapplication.models.ChatModel
import com.example.chatapplication.models.User
import com.example.chatapplication.ui.chat.ChatWindowActivity
import com.example.chatapplication.ui.chat.FullScreenImageActivity
import com.example.chatapplication.ui.chat.VideoPlayer
import com.google.firebase.firestore.FirebaseFirestore
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.layout_chat_item.view.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class ChatAdapter(
    private val context: Context,
    val chats: ArrayList<ChatModel>,
    val user: User,
    val oppUser: User,
    val chatId: String
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    val itemViews = ArrayList<ChatAdapter.ViewHolder>()
    private val selectedViews = ArrayList<ChatAdapter.ViewHolder>()
    val firestore = FirebaseFirestore.getInstance()
    val chatRef = firestore.collection("chats").document(chatId).collection("messages")
    var selectableFlag = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.layout_chat_item,parent,false)
        return ViewHolder(context, itemView)
    }

    override fun getItemCount(): Int {
        return chats.size
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        itemViews.add(holder)
        if(selectableFlag){
            holder.itemView.select.visibility = View.VISIBLE
        }
        holder.itemView.setOnLongClickListener {
            selectableMode()
            if(it.select.isChecked){
                it.select.isChecked = false
                selectedViews.remove(holder)
            }else{
                it.select.isChecked = true
                selectedViews.add(holder)
            }
            true
        }
        return holder.bind(chats[position],user,oppUser)
    }

    private fun selectableMode(){
        (context as ChatWindowActivity).selectMode()
        selectableFlag = true
        for(itm in itemViews){
            itm.itemView.select.visibility = View.VISIBLE
            itm.itemView.setOnClickListener {
                if(it.select.isChecked){
                    it.select.isChecked = false
                    selectedViews.remove(itm)
                }else{
                    it.select.isChecked = true
                    selectedViews.add(itm)
                }
            }
        }
    }
    fun endSelectableMode(){
        (context as ChatWindowActivity).endSelectMode()
        selectableFlag = false
        for(itm in itemViews){
            itm.itemView.isSelected = false
            itm.itemView.select.visibility = View.GONE
            itm.itemView.setOnClickListener(null)
        }
        selectedViews.clear()
    }

    fun delete(){
        val batch = firestore.batch()
        for(v in selectedViews){
            val id = v.itemView.chatItemId.text.toString()
            chatRef.document(id).collection("deletedFor").addSnapshotListener { querySnapshot, e ->
                if (querySnapshot != null) {
                    if(querySnapshot.isEmpty){
                        chatRef
                            .document(id)
                            .update("deletedFor", user.userId)
                            .addOnSuccessListener { notifyDataSetChanged() }
                            .addOnFailureListener {e-> e.printStackTrace()}
                    }else{
                        var str = ""
                        for(q in querySnapshot){
                            str+=q.get("0").toString()
                        }
                        str+="_"+user.userId!!
                        chatRef
                            .document(id)
                            .update("deletedFor",str)
                            .addOnSuccessListener { Log.e("delete","$id Updated") }
                            .addOnFailureListener {e-> e.printStackTrace()}
                    }
                }
            }
        }
        batch.commit()
        endSelectableMode()
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getItemViewType(position: Int): Int = position

    class ViewHolder(
        private val context: Context,
        private val chatView: View
    ) : RecyclerView.ViewHolder(chatView){

        fun bind(chatModel: ChatModel,user:User,oppUser:User) {
            if(chatModel.sender == user.userId) {
                chatView.sender.text = "YOU"
                chatView.chatLayout.gravity = GravityCompat.END
                chatView.chatItemLayout.background = context.resources.getDrawable(R.drawable.chat_item_right)
                chatView.message.setTextColor(Color.WHITE)
                chatView.sender.setTextColor(Color.WHITE)
            }else{
                chatView.sender.text = chatModel.sender
                chatView.chatItemLayout.background = context.resources.getDrawable(R.drawable.chat_item_left)
            }
            if (chatModel.type == "image") {
                chatView.blurImage.visibility = View.VISIBLE
                chatView.image.setImageDrawable(context.resources.getDrawable(R.drawable.img_placeholder))
                val options = RequestOptions.overrideOf(250,250)
                options.transform(BlurTransformation(25, 3))
                Glide.with(context)
                    .asBitmap()
                    .load(chatModel.msgContent)
                    .apply(options)
                    .into(chatView.image)
                chatView.message.visibility = View.GONE
                val uri = imageExistsAtLocation(chatModel.msgContent)
                if (uri != null) {
                    chatView.downloadBtn.visibility = View.GONE
                    Glide.with(context)
                        .asBitmap()
                        .load(uri)
                        .into(chatView.image)
                    chatView.image.setOnClickListener {
                        val intent = Intent(context, FullScreenImageActivity::class.java)
                        intent.putExtra("image", uri.toString())
                        context.startActivity(intent)
                    }
                } else {
                    chatView.downloadBtn.setOnClickListener {
                        Glide.with(context).asBitmap()
                            .load(chatModel.msgContent)
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onLoadCleared(placeholder: Drawable?) {}
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap>?
                                ) {
                                    chatView.downloadBtn.visibility = View.GONE
                                    val urii = saveImage(resource, getFileName(chatModel.msgContent))
                                    if (urii != null) {
                                        Glide.with(context)
                                            .asBitmap()
                                            .load(urii)
                                            .into(chatView.image)

                                        chatView.image.setOnClickListener {
                                            val intent = Intent(
                                                context,
                                                FullScreenImageActivity::class.java
                                            )
                                            intent.putExtra("image", urii.toString())
                                            context.startActivity(intent)
                                        }
                                    }
                                }
                            })
                    }
                }
            } else if (chatModel.type.equals("sticker")) {
                chatView.sticker.visibility = View.VISIBLE
                chatView.message.visibility = View.GONE
                chatView.chatItemLayout.background = null
                Glide.with(context)
                    .asBitmap()
                    .load(chatModel.msgContent)
                    .into(chatView.sticker)
                chatView.downloadBtn.visibility = View.GONE
            } else if (chatModel.type.equals("gif")) {
                chatView.sticker.visibility = View.VISIBLE
                chatView.message.visibility = View.GONE
                Glide.with(context)
                    .asGif()
                    .load(chatModel.msgContent)
                    .into(chatView.sticker)
                chatView.downloadBtn.visibility = View.GONE
            }
            else if (chatModel.type.equals("video")){
                chatView.blurImage.visibility = View.VISIBLE
                chatView.image.setImageDrawable(context.resources.getDrawable(R.drawable.img_placeholder))
                val options = RequestOptions.overrideOf(250,250)
                options.transform(BlurTransformation(25, 3))
                Glide.with(context)
                    .asBitmap()
                    .load(chatModel.msgContent)
                    .apply(options)
                    .into(chatView.image)
                chatView.message.visibility = View.GONE
                val uri = videoExistsAtLocation(chatModel.msgContent)
                if (uri != null) {
                    chatView.downloadBtn.visibility = View.GONE
                    Glide.with(context)
                        .asBitmap()
                        .load(uri)
                        .into(chatView.image)
                    chatView.image.setOnClickListener {
                        val intent = Intent(context, VideoPlayer::class.java)
                        intent.putExtra("video", uri.toString())
                        context.startActivity(intent)
                    }
                } else {
                    chatView.downloadBtn.setOnClickListener {
                        Glide.with(context).asBitmap()
                            .load(chatModel.msgContent)
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onLoadCleared(placeholder: Drawable?) {}
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap>?
                                ) {
                                    chatView.downloadBtn.visibility = View.GONE
                                    val urii = saveVideo(resource, getVFileName(chatModel.msgContent))
                                    if (urii != null) {
                                        Glide.with(context)
                                            .asBitmap()
                                            .load(urii)
                                            .into(chatView.image)

                                        chatView.image.setOnClickListener {
                                            val intent = Intent(
                                                context,
                                                VideoPlayer::class.java
                                            )
                                            intent.putExtra("video", urii.toString())
                                            context.startActivity(intent)
                                        }
                                    }
                                }
                            })
                    }
                }
            }else {
                chatView.message.text = chatModel.msgContent
            }
            chatView.timee.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
            chatView.timee.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(chatModel.dateTime.toLong())
            chatView.chatItemId.text = chatModel.chatItemId
        }

        private fun videoExistsAtLocation(url: String): Uri? {
            val fileName = getVFileName(url)
            val storageDir = File(context.getExternalFilesDir(null).toString()+"/images")
            val filePath = File(storageDir,fileName)
            return if(filePath.exists()){
                Uri.fromFile(filePath)
            }else{
                null
            }
        }

        private fun imageExistsAtLocation(url: String): Uri? {
            val fileName = getFileName(url)+".jpeg"
            val storageDir = File(context.getExternalFilesDir(null).toString()+"/images")
            val filePath = File(storageDir,fileName)
            return if(filePath.exists()){
                Uri.fromFile(filePath)
            }else{
                null
            }
        }

        fun saveImage(bmap:Bitmap,name:String): Uri? {
            val savedImgPath: String
            val imageFileName = "$name.jpeg"
            val storageDir = File(context.getExternalFilesDir(null).toString()+"/images")
            var flag = true
            if(!storageDir.exists()){
                flag = storageDir.mkdirs()
            }
            if(flag){
                val fileName = File(storageDir,imageFileName)
                savedImgPath = fileName.absolutePath
                try {
                    val fout = FileOutputStream(savedImgPath)
                    bmap.compress(Bitmap.CompressFormat.JPEG,100,fout)
                    fout.close()
                }catch (e:Exception){
                    e.printStackTrace()
                }
                val uri = Uri.fromFile(fileName)
                return uri
            }else{
                return null
            }
        }

        private fun saveVideo(resource: Bitmap, fileName: String) {

        }

        private fun getFileName(url:String): String {
                val start = url.lastIndexOf("img")
                val end = url.indexOf("?")
                return url.substring(start, end)
        }

        private fun getVFileName(url:String):String{
            val start = url.lastIndexOf("/")
            val end = url.length-1
            return url.substring(start, end)
        }
    }
}