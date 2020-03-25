package com.example.chatapplication.ui.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chatapplication.R
import com.example.chatapplication.models.ChatListModel
import com.example.chatapplication.models.User
import com.example.chatapplication.ui.chat.ChatWindowActivity
import kotlinx.android.synthetic.main.layout_user_item.view.*
import java.text.SimpleDateFormat


class ChatsListAdapter(private val chatListArr: ArrayList<ChatListModel>) : RecyclerView.Adapter<ChatsListAdapter.ViewHolder>(){

    class ViewHolder(private val usrListLayout: View, val context: Context) : RecyclerView.ViewHolder(usrListLayout){
        fun bind(chatItm:ChatListModel) {
            val user = chatItm.user
            usrListLayout.userItemName.text = user.userName
            if(!user.userImageUrl.isNullOrBlank()) {
                Glide.with(context).asBitmap().load(user.userImageUrl)
                    .apply(RequestOptions().circleCrop()).into(usrListLayout.userItemImage)
            }else{
                usrListLayout.userItemImage.setImageDrawable(context.resources.getDrawable(R.drawable.user))
            }
            usrListLayout.lastMessage.visibility = View.VISIBLE
            usrListLayout.lastMessage.text = chatItm.lastMsg
            usrListLayout.lastSeenTime.visibility = View.VISIBLE
            usrListLayout.lastSeenTime.text = SimpleDateFormat("HH:mm").format(chatItm.lastSeen?.toLong())
            usrListLayout.setOnClickListener {
                val intent = Intent(context, ChatWindowActivity::class.java)
                intent.putExtra("user",user)
                intent.putExtra("chatId",chatItm.chatId)
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.layout_user_item,parent,false)
        return ViewHolder(itemView,parent.context)
    }

    override fun getItemCount(): Int {
        return chatListArr.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(chatListArr[position])
    }

}