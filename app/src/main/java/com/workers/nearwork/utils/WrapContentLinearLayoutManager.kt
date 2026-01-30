package com.workers.nearwork.utils // or your package name

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// This class prevents the "Inconsistency detected" crash
class WrapContentLinearLayoutManager(context: Context) : LinearLayoutManager(context) {

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            Log.e("LayoutManager", "RecyclerView crash caught: ${e.message}")
        }
    }
}