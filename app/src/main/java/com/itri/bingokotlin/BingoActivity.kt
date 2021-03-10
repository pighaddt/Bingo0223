package com.itri.bingokotlin

import android.nfc.tech.NfcBarcode
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_bingo.*
import kotlinx.android.synthetic.main.single_button.*
import kotlinx.android.synthetic.main.single_button.view.*

class BingoActivity : AppCompatActivity() {
    private var isCreator: Boolean = false
    private var roomId: String? = null
    private lateinit var adapter: FirebaseRecyclerAdapter<Boolean, ButtonHolder>
    private var stateListener : ValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val status : Long = snapshot.value as Long
            when(status.toInt()){
                STATE_CREATED -> {
                    bingo_info.text = "waiting for challenger..."
                }
                STATE_JOINED -> {
                    bingo_info.text = "creator pick ball first"
                    if (isCreator){
                        FirebaseDatabase.getInstance().getReference("rooms")
                                .child(roomId!!)
                                .child("status")
                                .setValue(STATE_CREATER_TURN)
                    }
                }
                STATE_CREATER_TURN ->{
                    if (isCreator){
                        bingo_info.text  = "choose one ball "
                    } else{
                        bingo_info.text  = "creator is choosing a ball "
                    }
                }
                STATE_JOINER_TURN ->{
                    if (!isCreator){
                        bingo_info.text  = "choose one ball "
                    } else{
                        bingo_info.text  = "joiner is choosing a ball "
                    }
                }
                STATE_CREATOR_BINGO -> {
                    bingo_info.text  = "creator bingo!!!"
                }
                STATE_JOINER_BINGO -> {
                    bingo_info.text = "Joiner bingo!!!"
                }

            }
        }

        override fun onCancelled(error: DatabaseError) {

        }
    }
    companion object{
        private val TAG = BingoActivity::class.java.simpleName
        private val STATE_INIT = 0
        private val STATE_CREATED = 1
        private val STATE_JOINED = 2
        private val STATE_CREATER_TURN = 3
        private val STATE_JOINER_TURN = 4
        private val STATE_CREATOR_BINGO = 5
        private val STATE_JOINER_BINGO = 6
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bingo)
        roomId = intent.getStringExtra("ROOM_ID")
        isCreator = intent.getBooleanExtra("IS_CREATOR", false)
        //room id not null and status init
        roomId?.run {
            if (isCreator){
                FirebaseDatabase.getInstance().getReference("rooms")
                        .child(this)
                        .child("status")
                        .setValue(STATE_CREATED)
                // firebase number init
                for (i in 1..25){
                    FirebaseDatabase.getInstance().getReference("rooms")
                            .child(this)
                            .child("numbers")
                            .child(i.toString())
                            .setValue(false)
                }
            }else{
                FirebaseDatabase.getInstance().getReference("rooms")
                        .child(this)
                        .child("status")
                        .setValue(STATE_JOINED)
            }
        }


        //button ball setting && HashMap<number , position>
        var buttons = mutableListOf<NumberButton>()
        var numberMap = HashMap<Int, Int>()
        for (i in 1..25){
            val button = NumberButton(this)
            button.number = i
            buttons.add(button)
        }
        buttons.shuffle()
        for (i in 0..24){
            val number = buttons.get(i).number
            numberMap.put(number, i)
            Log.d(TAG, "numberMap: $number , $i")
        }


        //bingo ball recycler setting
        roomId?.run {
            val roomId = this
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
                override fun onChildChanged(type: ChangeEventType, snapshot: DataSnapshot, newIndex: Int, oldIndex: Int) {
                    super.onChildChanged(type, snapshot, newIndex, oldIndex)
                    if (type == ChangeEventType.CHANGED){
                        val number = snapshot.key?.toInt()
                        val pos = numberMap.get(number)
                        val picked = snapshot.value as Boolean
                        buttons.get(pos!!).picked = picked
                        val holder : ButtonHolder = recycler.findViewHolderForAdapterPosition(pos!!) as ButtonHolder
                        holder.button.isEnabled = !picked
                        //bingo count
                        var bingo = 0
                        var sum = 0
                        var nums = IntArray(25)
                        for (i in 0..24){
                            nums[i] = if (buttons[i].picked) 1 else 0
                        }
                        for (i in 0..4){
                            //row bingo counter
                            sum = 0
                            for(j in 0..4){
                                sum += nums[i*5 + j]
                            }
                            if (sum == 5)
                                bingo+=1
                            //column bingo counter
                            sum = 0
                            for(j in 0..4){
                                sum += nums[j*5 + i]
                            }
                            if (sum == 5)
                                bingo+=1
                            Log.d(TAG, "bingo: $bingo")
                        }
                    }
                }

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonHolder {
                    val view = layoutInflater.inflate(R.layout.single_button, parent, false)
                    return ButtonHolder(view)
                }

                override fun onBindViewHolder(holder: ButtonHolder, position: Int, model: Boolean) {
                    //init recycler ball
                    holder.button.text = buttons.get(position).number.toString()
                    holder.button.number = buttons.get(position).number
                    holder.button.isEnabled = !buttons.get(position).picked
                    holder.button.setOnClickListener {

                        val button = it as NumberButton
                        Log.d(TAG, "button.number: ${button.number}, roomId $roomId")
                        FirebaseDatabase.getInstance().getReference("rooms")
                                .child(roomId)
                                .child("numbers")
                                .child(button.number.toString())
                                .setValue(true)
                        if (isCreator){
                            FirebaseDatabase.getInstance().getReference("rooms")
                                    .child(roomId)
                                    .child("status")
                                    .setValue(STATE_JOINER_TURN)
                        }else{
                            FirebaseDatabase.getInstance().getReference("rooms")
                                    .child(roomId)
                                    .child("status")
                                    .setValue(STATE_CREATER_TURN)
                        }
                    }
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
        roomId?.run {
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(this)
                    .child("status")
                    .addValueEventListener(stateListener)
        }

    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
        roomId?.run {
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(this)
                    .child("status")
                    .removeEventListener(stateListener)
        }
    }
}