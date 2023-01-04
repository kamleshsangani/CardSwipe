package com.cards.coverflow

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.view.View

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onRecyclerViewClick(view: View?) {
        val intent = Intent(this, RecyclerViewActivity::class.java)
        startActivity(intent)
    }
}