package com.example.watermarkcamera

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (AgreementActivity.isAgreementAccepted(this)) {
            val intent = Intent(this, LoadingActivity::class.java)
            startActivity(intent)
        } else {
            val intent = Intent(this, AgreementActivity::class.java)
            startActivity(intent)
        }
        
        finish()
    }
}
