package com.example.watermarkcamera

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class UserAgreementActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvContent: TextView
    private lateinit var ivBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_agreement)

        initViews()
        setupListeners()
        loadContent()
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvTitle)
        tvContent = findViewById(R.id.tvContent)
        ivBack = findViewById(R.id.ivBack)
    }

    private fun setupListeners() {
        ivBack.setOnClickListener {
            finish()
        }
    }

    private fun loadContent() {
        val content = """用户协议

更新日期：2024年1月1日
生效日期：2024年1月1日

一、协议的接受

欢迎使用水印相机应用（以下简称"本应用"）。在使用本应用前，请您仔细阅读并充分理解本协议的全部内容。您使用本应用即表示您已阅读、理解并同意接受本协议的全部内容。

二、服务内容

本应用为您提供以下服务：

1. 拍照功能：使用设备相机拍摄照片
2. 水印添加：在照片上添加时间、地点等水印信息
3. 照片编辑：对照片进行基本的编辑处理
4. 照片保存：将处理后的照片保存到本地设备

三、用户义务

1. 您应当遵守中华人民共和国相关法律法规
2. 您不得利用本应用从事违法违规活动
3. 您不得侵犯他人的合法权益
4. 您应当妥善保管自己的设备和账号信息

四、知识产权

1. 本应用的所有知识产权归开发者所有
2. 未经许可，您不得复制、修改、传播本应用
3. 您使用本应用拍摄的照片的知识产权归您所有

五、免责声明

1. 本应用按"现状"提供，不提供任何明示或暗示的保证
2. 我们不对因使用本应用而产生的任何直接或间接损失承担责任
3. 我们不对第三方服务的可用性和准确性承担责任

六、服务的变更和终止

1. 我们有权随时修改或终止本应用的服务
2. 我们会提前通知您重大变更
3. 服务终止后，您应当停止使用本应用

七、隐私保护

我们重视您的隐私保护，具体内容请参阅《隐私政策》。

八、争议解决

1. 本协议的解释、效力及纠纷的解决适用中华人民共和国法律
2. 因本协议产生的争议，双方应友好协商解决
3. 协商不成的，任何一方可向开发者所在地人民法院提起诉讼

九、协议的修改

我们有权随时修改本协议。修改后的协议将在应用中公布，并在您下次使用时生效。如果您不同意修改后的协议，您应当停止使用本应用。

十、其他

1. 本协议的标题仅为方便阅读，不影响协议的解释
2. 如本协议的任何条款被认定为无效，不影响其他条款的效力
3. 本协议的最终解释权归开发者所有

十一、联系我们

如果您对本协议有任何疑问，请通过以下方式联系我们：

邮箱：support@example.com
电话：400-xxx-xxxx

感谢您使用我们的应用！"""

        tvContent.text = content
    }
}
