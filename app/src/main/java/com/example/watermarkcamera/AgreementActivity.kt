package com.example.watermarkcamera

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AgreementActivity : AppCompatActivity() {

    private lateinit var checkBoxAgree: CheckBox
    private lateinit var btnAgree: Button
    private lateinit var btnDisagree: Button
    private lateinit var tvAgreementContent: TextView

    companion object {
        private const val PREFS_NAME = "app_preferences"
        private const val KEY_AGREEMENT_ACCEPTED = "agreement_accepted"

        fun isAgreementAccepted(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_AGREEMENT_ACCEPTED, false)
        }

        fun setAgreementAccepted(context: Context, accepted: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_AGREEMENT_ACCEPTED, accepted).apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agreement)

        initViews()
        setupListeners()
        loadAgreementContent()
    }

    private fun initViews() {
        checkBoxAgree = findViewById(R.id.checkBoxAgree)
        btnAgree = findViewById(R.id.btnAgree)
        btnDisagree = findViewById(R.id.btnDisagree)
        tvAgreementContent = findViewById(R.id.tvAgreementContent)

        btnAgree.isEnabled = false
    }

    private fun setupListeners() {
        checkBoxAgree.setOnCheckedChangeListener { _, isChecked ->
            btnAgree.isEnabled = isChecked
        }

        btnAgree.setOnClickListener {
            setAgreementAccepted(this, true)
            navigateToLoading()
        }

        btnDisagree.setOnClickListener {
            finish()
        }
    }

    private fun loadAgreementContent() {
        val agreementText = """
            <h2>用户协议与隐私政策</h2>
            
            <h3>一、用户协议</h3>
            <p>欢迎使用水印相机应用。在使用本应用前，请您仔细阅读并充分理解本协议的全部内容。</p>
            
            <p><b>1.1 服务内容</b><br/>
            本应用为您提供拍照、添加水印、位置信息标注等功能服务。</p>
            
            <p><b>1.2 用户义务</b><br/>
            您承诺遵守相关法律法规，不得利用本应用从事违法违规活动。</p>
            
            <p><b>1.3 知识产权</b><br/>
            本应用的所有知识产权归开发者所有，未经许可不得擅自使用。</p>
            
            <h3>二、隐私政策</h3>
            <p>我们非常重视您的隐私保护，本政策说明我们如何收集、使用和保护您的个人信息。</p>
            
            <p><b>2.1 信息收集</b><br/>
            • 位置信息：用于在照片上添加地理位置水印<br/>
            • 相机权限：用于拍摄照片<br/>
            • 存储权限：用于保存照片到本地</p>
            
            <p><b>2.2 信息使用</b><br/>
            我们收集的信息仅用于提供应用功能，不会用于其他商业目的。</p>
            
            <p><b>2.3 信息保护</b><br/>
            您的照片和位置信息仅存储在本地设备，我们不会上传到服务器。</p>
            
            <p><b>2.4 第三方服务</b><br/>
            本应用可能使用第三方地图服务获取地址信息，请参阅相关服务商的隐私政策。</p>
            
            <p><b>2.5 权限说明</b><br/>
            • 相机权限：必需，用于拍照功能<br/>
            • 位置权限：可选，用于获取地理位置信息<br/>
            • 存储权限：必需，用于保存照片</p>
            
            <p style="margin-top: 20px;">如您对本协议有任何疑问，请联系我们。</p>
        """.trimIndent()

        tvAgreementContent.text = Html.fromHtml(agreementText, Html.FROM_HTML_MODE_COMPACT)
        tvAgreementContent.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun navigateToLoading() {
        val intent = Intent(this, LoadingActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
