package com.stripe.example.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.stripe.android.Checkout
import com.stripe.example.R

class LaunchMCActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch_m_c)
        findViewById<Button>(R.id.launch_mc).setOnClickListener {
            Checkout("", "", "").confirm(this) {
            }
        }
    }
}
