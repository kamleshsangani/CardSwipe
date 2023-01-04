package com.cards.coverflow

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.cards.coverflow.R

class CoverFlowAdapter : RecyclerView.Adapter<CoverFlowAdapter.ViewHolder> {
    private val mContext: Context
    private val mColors = intArrayOf(
        Color.BLUE, Color.BLACK, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.RED, Color.YELLOW
    )
    private var clickCb: onItemClick? = null

    constructor(c: Context) {
        mContext = c
    }

    constructor(c: Context, cb: onItemClick?) {
        mContext = c
        clickCb = cb
    }

    fun setClickListener(cb: onItemClick?) {
        clickCb = cb
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = R.layout.layout_item
        val v = LayoutInflater.from(mContext).inflate(layout, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position1: Int) {
        val position = holder.bindingAdapterPosition
        holder.layout.setBackgroundColor(mColors[position])
        holder.itemView.setOnClickListener {
            if (clickCb != null) {
                clickCb!!.clickItem(position)
            }
        }
        holder.text.text = position.toString()
    }

    override fun getItemCount(): Int {
        return mColors.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //var img: ImageView
        var layout: ConstraintLayout
        var text: TextView

        init {
            //img = itemView.findViewById(R.id.img)
            layout = itemView.findViewById(R.id.outer_layout)
            text = itemView.findViewById(R.id.labelTextView)
        }
    }

    interface onItemClick {
        fun clickItem(pos: Int)
    }
}