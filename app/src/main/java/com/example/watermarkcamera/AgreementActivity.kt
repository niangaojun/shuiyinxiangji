package com.example.watermarkcamera

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AgreementActivity : AppCompatActivity() {

    private lateinit var btnAgree: Button
    private lateinit var btnDisagree: TextView
    private lateinit var tvAgreementContent: TextView
    private lateinit var tvLinkText: TextView

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
        setupClickableLinks()
    }

    private fun initViews() {
        btnAgree = findViewById(R.id.btnAgree)
        btnDisagree = findViewById(R.id.btnDisagree)
        tvAgreementContent = findViewById(R.id.tvAgreementContent)
        tvLinkText = findViewById(R.id.tvLinkText)
    }

    private fun setupListeners() {
        btnAgree.setOnClickListener {
            setAgreementAccepted(this, true)
            navigateToLoading()
        }

        btnDisagree.setOnClickListener {
            finishAffinity()
        }
    }

    private fun loadAgreementContent() {
        val content = """1. 我们会遵循隐私政策收集、使用信息，但不会仅因同意本隐私政策而采用强制捆绑的方式收集信息。

2. 为了识别您的设备账号，方便注册认证，我们会申请获取您的手机号码、IMEI权限。为了您浏览资讯及维护文件，我们会申请存储权限。为了方便您现场拍摄视频内容，我们可能会申请位置权限。为了方便您编辑个人信息，我们可能会申请相机权限。只有经过您的授权才会在实现功能或服务时使用。

3. 您可查看完整版《隐私政策》和《用户协议》。"""

        tvAgreementContent.text = content
    }

    private fun setupClickableLinks() {
        val linkText = "您可查看完整版《隐私政策》和《用户协议》。"
        val spannableString = SpannableString(linkText)

        val privacyStart = linkText.indexOf("《隐私政策》")
        val privacyEnd = privacyStart + "《隐私政策》".length

        val userAgreementStart = linkText.indexOf("《用户协议》")
        val userAgreementEnd = userAgreementStart + "《用户协议》".length

        val privacyClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@AgreementActivity, PrivacyPolicyActivity::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.parseColor("#0000FF")
                ds.isUnderlineText = false
            }
        }

        val userAgreementClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@AgreementActivity, UserAgreementActivity::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.parseColor("#0000FF")
                ds.isUnderlineText = false
            }
        }

        spannableString.setSpan(
            privacyClickableSpan,
            privacyStart,
            privacyEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannableString.setSpan(
            userAgreementClickableSpan,
            userAgreementStart,
            userAgreementEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvLinkText.text = spannableString
        tvLinkText.movementMethod = LinkMovementMethod.getInstance()
        tvLinkText.highlightColor = Color.TRANSPARENT
    }

    private fun navigateToLoading() {
        val intent = Intent(this, LoadingActivity::class.java)
        startActivity(intent)
        finish()
    }
}
