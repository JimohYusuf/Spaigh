package com.example.spaigh

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.spaigh.network.DbConstants
import com.google.android.material.navigation.NavigationView

var serverConnection = "server address"
val handler = Handler()

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var drawer: DrawerLayout

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Use created toolbar instead of default action bar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        drawer = findViewById(R.id.drawer_layout)

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        //Use android ready-made navigation drawer toggle icon
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        //Open Connect page automatically at app launch only
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ConnectFragment()).commit()
            navigationView.setCheckedItem(R.id.nav_connect)
        }

    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun onResume() {
        super.onResume()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun onPause() {
        super.onPause()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    /*Take action based on selected option in navigation drawer menu*/
    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.nav_connect -> supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ConnectFragment()).commit()
            R.id.nav_settings -> supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingsFragment()).commit()
            R.id.nav_livestream -> {
                Toast.makeText(this, "Live streaming starting", Toast.LENGTH_SHORT).show()
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DbConstants.STREAM_ADDRESS))
                    startActivity(intent)
                } catch (ex: Exception) {
                    println(ex.message)
                }
            }
            R.id.nav_website -> {
                Toast.makeText(this, "Opening website", Toast.LENGTH_LONG).show()
                try {
                    val browserIntent =
                        Intent(Intent.ACTION_VIEW, Uri.parse(DbConstants.WEBSITE_ADDRESS))
                    startActivity(browserIntent)
                } catch (ex: Exception) {
                    println(ex.message)
                }
            }
            R.id.nav_about -> {
                Toast.makeText(this, "Go to documentation", Toast.LENGTH_LONG).show()
            }
        }

        drawer.closeDrawer(GravityCompat.START)
        return true
    }
}