# 水印相机

一个使用Kotlin开发的Android水印相机应用，支持在拍照时自动添加时间、地址、经纬度和海拔水印。采用现代化UI设计，界面美观流畅。

## 功能特点

### 核心功能
- 实时相机预览，支持双指缩放
- 点击对焦功能，显示对焦动画
- 拍照后自动添加水印（支持底部、顶部、左侧、右侧位置）
- 闪光灯控制（关闭/开启/自动）
- 照片历史记录浏览
- 支持从相册选择图片添加水印
- 支持Android 8.0 (API 24) 到 Android 15 (API 35)

### 水印内容
- 当前日期时间
- 地理位置（经纬度）
- 海拔高度（可选）
- 具体地址信息（可选）
- 自定义文字

### UI设计特点
- 现代化深色主题界面
- 毛玻璃效果工具栏
- 流畅的动画过渡效果
- 拍照按钮呼吸动画提示
- 精致的按钮设计和交互反馈

## 技术栈

- Kotlin 2.0.0
- Android Gradle Plugin 8.13.0
- Android CameraX API 1.4.1
- Google Play Services Location 21.3.0
- Material Design 3
- Kotlin Coroutines
- ViewBinding
- ProGuard/R8 代码优化

## 项目结构

```
├── app/
│   ├── src/main/
│   │   ├── java/com/example/watermarkcamera/
│   │   │   ├── MainActivity.kt              # 主界面和相机控制
│   │   │   ├── CameraManager.kt             # 相机管理器
│   │   │   ├── WatermarkProcessor.kt        # 水印处理器
│   │   │   ├── LocationManager.kt           # 位置管理器
│   │   │   ├── ElevationManager.kt          # 海拔信息获取
│   │   │   ├── AddressResolver.kt           # 地址解析器
│   │   │   ├── ConfigManager.kt             # 配置管理器
│   │   │   ├── PermissionManager.kt         # 权限管理器
│   │   │   ├── ImageCompressor.kt           # 图片压缩器
│   │   │   ├── FocusRingView.kt             # 自定义对焦框视图
│   │   │   ├── SettingsActivity.kt          # 设置页面
│   │   │   ├── PhotoHistoryActivity.kt      # 照片历史
│   │   │   ├── WatermarkPreviewActivity.kt  # 水印预览
│   │   │   └── FullscreenImageActivity.kt   # 全屏图片查看
│   │   ├── res/
│   │   │   ├── layout/              # 布局文件
│   │   │   ├── drawable/            # 图像资源
│   │   │   ├── anim/                # 动画资源
│   │   │   ├── values/              # 字符串、颜色、样式
│   │   │   └── xml/                 # 配置文件
│   │   └── AndroidManifest.xml      # 应用清单
├── build.gradle.kts                 # 项目级构建脚本
├── settings.gradle.kts              # 项目设置
└── gradle/libs.versions.toml        # 依赖版本管理
```

## 权限说明

该应用需要以下权限：

- **相机权限** (`CAMERA`)：用于拍照
- **位置权限** (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`)：用于获取当前位置信息添加到水印
- **存储权限**：
  - Android 13+：`READ_MEDIA_IMAGES`
  - Android 12及以下：`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`

## 使用说明

### 快速开始

1. 使用Android Studio打开项目
2. 确保已安装Android SDK 24-35
3. 同步Gradle依赖
4. 在支持Android 8.0+的设备上运行应用
5. 授予必要的权限
6. 点击拍照按钮拍摄照片
7. 拍摄的照片将自动保存到相册并显示水印

### 手势操作

- **双指缩放**：放大/缩小相机画面
- **单击预览**：对焦到点击位置，显示对焦动画

### 界面元素

- **顶部工具栏**：设置按钮、闪光灯按钮
- **底部控制区**：图库按钮、拍照按钮、最近照片预览
- **对焦框**：点击预览区域显示，带有发光效果

### 设置选项

- 水印位置（底部/顶部/左侧/右侧）
- 文字大小
- 文字颜色
- 背景透明度
- 显示/隐藏位置信息
- 显示/隐藏日期时间
- 显示/隐藏地址信息
- 获取/不获取海拔高度
- 图片质量
- 自定义水印文字

## 优化特性

### 性能优化
- **代码优化**：启用ProGuard/R8代码压缩和资源缩减
- **性能优化**：使用协程进行异步处理，避免主线程阻塞
- **内存优化**：使用ViewBinding安全模式，及时释放资源
- **缓存优化**：地址和海拔信息使用LRU缓存
- **图片优化**：智能缩略图加载，减少内存占用

### UI优化
- 现代化深色主题设计
- 流畅的入场动画和交互反馈
- 拍照按钮呼吸动画提示
- 对焦框发光效果
- 照片预览边框高亮

## 注意事项

- 位置信息可能需要开启GPS并等待几秒钟才能获取准确位置
- 海拔信息需要网络连接，通过Open Elevation API获取
- 地址解析使用Android系统Geocoder，需要网络连接
- 在Android 13+设备上，应用遵循分区存储机制
- Release版本会自动移除日志输出

## 版本历史

### v3.0 (当前版本)
- 全面升级到Android SDK 35
- 全新现代化UI设计，深色主题
- 添加毛玻璃效果工具栏
- 优化动画效果，添加按钮呼吸动画
- 修复预览图像超出边框问题
- 修复地址信息显示格式问题
- 优化代码结构，修复内存泄漏
- 添加照片历史功能
- 支持从相册选择图片添加水印
- 添加闪光灯控制
- 优化性能，启用代码压缩

### v2.0
- 添加设置页面
- 支持自定义水印选项
- 添加海拔信息获取
- 添加地址信息显示

### v1.0
- 初始版本
- 基本相机功能
- 水印添加功能

## 开源协议

本项目采用 MIT 协议开源。
