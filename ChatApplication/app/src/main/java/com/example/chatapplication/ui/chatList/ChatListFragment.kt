package com.example.chatapplication.ui.chatList

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapplication.R
import com.example.chatapplication.models.ChatListModel
import com.example.chatapplication.models.User
import com.example.chatapplication.ui.BaseFragment
import com.example.chatapplication.ui.adapters.ChatsListAdapter
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_chat_list.view.*

class ChatListFragment : BaseFragment() {

    private val chatListArr = ArrayList<ChatListModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val user = shared.getUser()

        val mView = inflater.inflate(R.layout.fragment_chat_list, container, false)

        mView.chatsList.layoutManager = LinearLayoutManager(activity)
        val adapter = ChatsListAdapter(chatListArr)
        mView.chatsList.adapter = adapter

        chatRef.orderBy("updatedTime").addSnapshotListener { querySnapshot, e ->

            if(e!=null){
                e.printStackTrace()
                return@addSnapshotListener
            }

            if (querySnapshot != null) {
                chatListArr.clear()
                for(snap in querySnapshot){
                    if(snap.id.contains(user.userId!!)) {
                        var userstr = snap.id.replace(user.userId!!, "")
                        userstr = userstr.replace("_", "")
                        ////
                        userRef.document(userstr).addSnapshotListener { doc, exc ->
                            if (exc != null) {
                                exc.printStackTrace()
                            } else {
                                ////
                                chatRef.document(snap.id)
                                    .get().addOnSuccessListener { documentSnapshot ->
                                        if (documentSnapshot != null) {
                                            ////
                                            chatRef.document(snap.id).collection("messages")
                                                .document(documentSnapshot.getString("lastmsg")!!)
                                                .get()
                                                .addOnSuccessListener {
                                                    val id = doc?.id
                                                    val name = doc?.getString("name")
                                                    val imgUrl = doc?.getString("image")
                                                    val phoneNum =
                                                    if(!it.getString("type").equals("text")){
                                                        chatListArr.add(
                                                            ChatListModel(
                                                                User(id, name, imgUrl),
                                                                snap.id,
                                                                it.getString("type"),
                                                                snap.getString("updatedTime")
                                                            )
                                                        )
                                                    }
                                                    else{
                                                    chatListArr.add(
                                                        ChatListModel(
                                                            User(id, name, imgUrl),
                                                            snap.id,
                                                            it.getString("msgContent"),
                                                            snap.getString("updatedTime")
                                                        )
                                                    )
                                                    }
                                                    adapter.notifyDataSetChanged()
                                                }.addOnFailureListener {
                                                    it.printStackTrace()
                                                    Log.e("retrive msg", "Failure")
                                                }
                                        }
                                    }.addOnFailureListener {
                                        Log.e("snap id", "lastmsg id not found")
                                    }
                            }
                        }
                    }
                }
            }

        }

        return mView
    }
}
