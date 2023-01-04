package com.cards.coverflow

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.OrientationHelper
import com.cards.coverflow.CoverFlowLayoutManger.OnSelected

class RecyclerViewActivity : AppCompatActivity() {
    private var coverFlow: RecyclerCoverFlow? = null
    private var mCoverFlowPosition = 2
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recyclerview)
        initList()
    }

    private fun initList() {
        coverFlow = findViewById<View>(R.id.cover_flow) as RecyclerCoverFlow
        coverFlow!!.setOrientation(OrientationHelper.HORIZONTAL)
        coverFlow!!.adapter = CoverFlowAdapter(this)
        coverFlow!!.setOnItemSelectedListener(object : OnSelected {
            override fun onItemSelected(position: Int) {
                mCoverFlowPosition = position
            }
            override fun onItemSelectStart() {}
            override fun onItemSelectEnd() {}
        })
        coverFlow!!.scrollToPosition(mCoverFlowPosition)
    }
}