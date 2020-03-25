package com.example.chatapplication.ui.usersList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapplication.R
import com.example.chatapplication.models.User
import com.example.chatapplication.ui.BaseFragment
import com.example.chatapplication.ui.adapters.UsersListAdapter
import kotlinx.android.synthetic.main.fragment_users_list.view.*

class UsersListFragment : BaseFragment() {

    private lateinit var mView : View
    private var snapList = ArrayList<User>()
    private var adapter:UsersListAdapter?=null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?{

        val user = shared.getUser()

        mView = inflater.inflate(R.layout.fragment_users_list, container, false)

        adapter = UsersListAdapter(snapList)
        mView.usersList.layoutManager = LinearLayoutManager(activity)
        mView.usersList.adapter = adapter

        userRef.orderBy("name").addSnapshotListener{snapshot,e->
            if (e != null) {
                e.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                snapList.clear()
                for(doc in snapshot){
                    if(doc.id.equals(user.userId))
                        continue
                    val name = doc.getString("name")
                    val imgUrl = doc.getString("image")
                    snapList.add(User(doc.id,name,imgUrl,""))
                }
                adapter!!.notifyDataSetChanged()
            }
        }

        return mView
    }
}
