package com.bedirhandag.harcapaylas

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_grup.*

class GrupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grup)

        grupKey.text = intent.getStringExtra("grupKey")
    }
}