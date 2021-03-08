package com.itri.bingokotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_bingo.*
import kotlinx.android.synthetic.main.single_button.*
import kotlinx.android.synthetic.main.single_button.view.*

class BingoActivity : AppCompatActivity() {
    private lateinit var adapter: FirebaseRecyclerAdapter<Boolean, ButtonHolder>

    companion object{
        private val TAG = BingoActivity::class.java.simpleName
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bingo)
        val roomId = intent.getStringExtra("ROOM_ID")
        val isCreator = intent.getBooleanExtra("IS_CREATOR", false)
        Log.d(TAG, "BingoActivity: roomId ${roomId}, isCreator ${isCreator}")

        //button ball setting
        var buttons = mutableListOf<NumberButton>()
        for (i in 1..25){
            FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId!!)
                .child("numbers")
                .child(i.toString())
                .setValue(false)
            val button = NumberButton(this)
            button.number = i
            buttons.add(button)
        }
        buttons.shuffle()

        //bingo ball recycler setting
        roomId?.run {
            recycler.setHasFixedSize(true)
            recycler.layoutManager  = GridLayoutManager(this@BingoActivity, 5)
            val query = FirebaseDatabase.getInstance().getReference("rooms")
                .child(this)
                .child("numbers")
                .orderByKey()
            val options = FirebaseRecyclerOptions.Builder<Boolean>()
                .setQuery(query, Boolean::class.java)
                .build()
            adapter = object : FirebaseRecyclerAdapter<Boolean, ButtonHolder>(options) {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonHolder {
                    val view = layoutInflater.inflate(R.layout.single_button, parent, false)
                    return ButtonHolder(view)
                }

                override fun onBindViewHolder(holder: ButtonHolder, position: Int, model: Boolean) {
                    holder.button.text = buttons.get(position).number.toString()
                    holder.button.picked = !model
                }

            }
            recycler.adapter = adapter
            Log.d(TAG, "onCreate: adapter create")
        }
    }
    class ButtonHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
         var button : NumberButton = itemView.button
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }
}