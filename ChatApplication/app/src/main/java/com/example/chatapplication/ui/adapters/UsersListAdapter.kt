package com.example.chatapplication.ui.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chatapplication.R
import com.example.chatapplication.models.User
import com.example.chatapplication.ui.chat.ChatWindowActivity
import kotlinx.android.synthetic.main.layout_user_item.view.*

class UsersListAdapter(private val userList:ArrayList<User>) : RecyclerView.Adapter<UsersListAdapter.ViewHolder>(){

    class ViewHolder(private val usrListLayout: View,val context: Context) : RecyclerView.ViewHolder(usrListLayout){
        fun bind(user: User) {
            usrListLayout.userItemName.text = user.userName
            if(!user.userImageUrl.isNullOrBlank()) {
                Glide.with(context).asBitmap().load(user.userImageUrl)
                    .apply(RequestOptions().circleCrop()).into(usrListLayout.userItemImage)
            }else{
                usrListLayout.userItemImage.setImageDrawable(context.resources.getDrawable(R.drawable.user))
            }
            usrListLayout.setOnClickListener {
                val intent = Intent(context,ChatWindowActivity::class.java)
                intent.putExtra("user",user)
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
        return userList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(userList[position])
    }

}