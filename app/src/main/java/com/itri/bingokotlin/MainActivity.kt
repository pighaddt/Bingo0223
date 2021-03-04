package com.itri.bingokotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener, View.OnClickListener {
    private var member: Member? = null

    companion object{
        private val TAG = MainActivity::class.java.simpleName
        private val RC_SIGN_IN = 1
        val avatarId = listOf(R.drawable.avatar_0, R.drawable.avatar_1, R.drawable.avatar_2,
                R.drawable.avatar_3, R.drawable.avatar_4, R.drawable.avatar_5)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        group_avatars.visibility = View.GONE
        nickname.setOnClickListener {
            FirebaseAuth.getInstance().currentUser?.run {
                showNicknameDialog(this.displayName!!, this.uid)
            }
        }

        avatar.setOnClickListener {
            group_avatars.visibility =
                    if (group_avatars.visibility == View.GONE) View.VISIBLE else View.GONE
        }
        //avatar setting on click
        avatar_0.setOnClickListener(this)
        avatar_1.setOnClickListener(this)
        avatar_2.setOnClickListener(this)
        avatar_3.setOnClickListener(this)
        avatar_4.setOnClickListener(this)
        avatar_5.setOnClickListener(this)
        fab.setOnClickListener {
            val roomEdit = EditText(this)
            roomEdit.gravity = Gravity.CENTER
            roomEdit.setText("Wellcome")
            AlertDialog.Builder(this)
                    .setTitle("Game Room")
                    .setMessage("Your game room title??")
                    .setView(roomEdit)
                    .setPositiveButton("OK"){_ , _ ->
                        var room = GameRoom(roomEdit.text.toString(), member!!)
                        FirebaseDatabase.getInstance().getReference("rooms")
                                .push().setValue(room)
                    }.show()
        }
    }

    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener(this)
    }

    override fun onStop() {
        super.onStop()
        FirebaseAuth.getInstance().removeAuthStateListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_sign_out -> AuthUI.getInstance().signOut(this)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        // auth User
        auth.currentUser?.also {
            it.displayName?.run {
                FirebaseDatabase.getInstance().getReference("users")
                        .child(it.uid)
                        .child("displayName")
                        .setValue(this)
                        .addOnCompleteListener { Log.d(TAG, "addOnCompleteListener: ") }
            }
            // uid setting
            FirebaseDatabase.getInstance().getReference("users")
                    .child(it.uid)
                    .child("uid")
                    .setValue(it.uid)
            //addValueEventListener vs SingleValueEvent
            FirebaseDatabase.getInstance().getReference("users")
                .child(it.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        member = snapshot.getValue(Member::class.java)
                        member?.nickname?.also { nick ->
                            nickname.text = nick.toString()
                        } ?: showNicknameDialog(it)
                        member?.also { member ->
                            avatar.setImageResource(avatarId[member.avatarId])
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                })
//            nicknameSingleValueEvent(it)
        } ?: signIn()
    }

    private fun nicknameSingleValueEvent(it: FirebaseUser) {
        FirebaseDatabase.getInstance().getReference("users")
            .child(it.uid)
            .child("nickname")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.value?.also { nick ->
                        nickname.text = nick.toString()
                    } ?: showNicknameDialog(it)
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun showNicknameDialog(displayName :String, uid : String){
        val editText = EditText(this)
        editText.gravity = Gravity.CENTER
        editText.setText(displayName)
        AlertDialog.Builder(this)
                .setTitle("Nickname")
                .setMessage("Enter your nickname")
                .setView(editText)
                .setPositiveButton("OK"){_ , _ ->
                    FirebaseDatabase.getInstance().getReference("users")
                            .child(uid)
                            .child("nickname")
                            .setValue(editText.text.toString())
                }.show()
    }

    private fun showNicknameDialog(user: FirebaseUser) {
        val displayName  = user.displayName
        val uid = user.uid
        showNicknameDialog(displayName!!, uid)
    }

    private fun signIn() {
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setAvailableProviders(
                                mutableListOf(
                                        AuthUI.IdpConfig.EmailBuilder().build(),
                                        AuthUI.IdpConfig.GoogleBuilder().build(),
                                        AuthUI.IdpConfig.AppleBuilder().build()
                                )
                        )
                        .setIsSmartLockEnabled(true)
                        .build(), RC_SIGN_IN)
    }

    override fun onClick(v: View?) {
        val avatarId = when(v!!.id){
            R.id.avatar_0 -> 0
            R.id.avatar_1 -> 1
            R.id.avatar_2 -> 2
            R.id.avatar_3 -> 3
            R.id.avatar_4 -> 4
            R.id.avatar_5 -> 5
            else -> 0
        }
        FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                .child("avatarId")
                .setValue(avatarId)
        group_avatars.visibility = View.GONE

    }
}