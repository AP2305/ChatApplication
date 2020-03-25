package com.example.chatapplication.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.example.chatapplication.R
import com.example.chatapplication.ui.adapters.DashboardFragmentAdapter
import com.example.chatapplication.ui.chatList.ChatListFragment
import com.example.chatapplication.ui.userProfile.UserProfileFragment
import com.example.chatapplication.ui.usersList.UsersListFragment
import com.example.chatapplication.ui.login_register.RegisterActivity
import kotlinx.android.synthetic.main.activity_dashboard.*

class DashboardActivity : BaseActivity(){

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_list,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.logout->{
                firebaseAuth.signOut()
                shared.removeUser()
                startActivity(Intent(this,RegisterActivity::class.java))
                finish()
            }
        }

        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val adapter = DashboardFragmentAdapter(supportFragmentManager)
        adapter.addFrag(UserProfileFragment(),"Profile")
        adapter.addFrag(ChatListFragment(),"Chat")
        adapter.addFrag(UsersListFragment(),"Users")

        dashViewPager.adapter = adapter
        dashViewPager.offscreenPageLimit = 3
        dashTabLayout.setupWithViewPager(dashViewPager)

        dashTabLayout.getTabAt(0)!!.icon = resources.getDrawable(R.drawable.profile)
        dashTabLayout.getTabAt(1)!!.icon = resources.getDrawable(R.drawable.chat)
        dashTabLayout.getTabAt(2)!!.icon = resources.getDrawable(R.drawable.users_icon)

        dashViewPager.currentItem = 1


    }

    override fun onBackPressed() {
        exitDialog()
    }
}
