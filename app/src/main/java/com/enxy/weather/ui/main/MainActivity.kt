package com.enxy.weather.ui.main

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import com.enxy.weather.R
import com.enxy.weather.ui.search.SearchFragment
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setSupportActionBar(bottomAppBar)
        bottomAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_search_action -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.mainContainer, SearchFragment.newInstance())
                        .addToBackStack(SearchFragment.TAG)
                        .commit()
                    true
                }
                else -> false
            }
        }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainContainer, MainFragment.newInstance())
                .commitNow()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
}
