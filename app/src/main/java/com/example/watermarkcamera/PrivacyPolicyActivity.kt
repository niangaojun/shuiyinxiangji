package com.example.watermarkcamera

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PrivacyPolicyActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvContent: TextView
    private lateinit var ivBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)

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
        val content = """隐私政策

更新日期：2024年1月1日
生效日期：2024年1月1日

一、引言

欢迎您使用我们的水印相机应用。我们非常重视您的隐私保护和个人信息安全。本隐私政策将向您说明我们如何收集、使用、存储、共享和保护您的个人信息，以及您所享有的相关权利。

二、我们收集的信息

1. 设备信息
我们会收集您的设备型号、操作系统版本、设备标识符（如IMEI、Android ID）等信息，用于识别您的设备并提供更好的服务。

2. 位置信息
在您授权的情况下，我们会收集您的地理位置信息，用于在照片上添加位置水印。您可以随时在设备设置中关闭位置权限。

3. 相机和存储权限
为了实现拍照和保存照片功能，我们需要获取您的相机和存储权限。这些权限仅在您使用相关功能时才会被调用。

4. 照片信息
您使用本应用拍摄或编辑的照片将存储在您的本地设备中，我们不会上传到服务器。

三、信息的使用

我们收集的信息将用于以下目的：

1. 提供核心功能服务（拍照、添加水印、保存照片）
2. 改进应用性能和用户体验
3. 进行数据分析和统计
4. 保障应用安全和防止欺诈

四、信息的存储

您的照片和相关数据仅存储在您的本地设备中。我们不会将您的照片上传到任何服务器或云端存储。

五、信息的共享

我们不会与任何第三方共享您的个人信息，除非：

1. 获得您的明确同意
2. 法律法规要求
3. 保护我们或他人的合法权益

六、您的权利

您对自己的个人信息享有以下权利：

1. 访问权：您可以随时查看您的个人信息
2. 更正权：您可以要求更正不准确的信息
3. 删除权：您可以要求删除您的个人信息
4. 撤回同意权：您可以随时撤回对权限的授权

七、信息安全

我们采取合理的安全措施保护您的个人信息，包括：

1. 数据加密
2. 访问控制
3. 安全审计

八、未成年人保护

我们不会故意收集未满14周岁未成年人的个人信息。如果您是未成年人的监护人，发现我们收集了未成年人的信息，请联系我们。

九、隐私政策的更新

我们可能会不时更新本隐私政策。更新后的政策将在应用中公布，并在您下次使用时生效。

十、联系我们

如果您对本隐私政策有任何疑问、意见或建议，请通过以下方式联系我们：

邮箱：support@example.com
电话：400-xxx-xxxx

感谢您信任并使用我们的应用！"""

        tvContent.text = content
    }
}
