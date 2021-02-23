package com.itri.bingokotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener {
    companion object{
        private val TAG = MainActivity::class.java.simpleName
        private val RC_SIGN_IN = 1
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
        if (auth.currentUser == null){
            startActivityForResult(
                    AuthUI.getInstance().createSignInIntentBuilder()
                            .setAvailableProviders(
                                    mutableListOf(
                                            AuthUI.IdpConfig.EmailBuilder().build(),
                                            AuthUI.IdpConfig.GoogleBuilder().build(),
                                            AuthUI.IdpConfig.AppleBuilder().build()
                                    )
                            )
                            .setIsSmartLockEnabled(false)
                            .build()
                    , RC_SIGN_IN)
        }else{
            Log.d(TAG, "onAuthStateChanged: ${auth.currentUser!!.providerData}")
        }
    }
}