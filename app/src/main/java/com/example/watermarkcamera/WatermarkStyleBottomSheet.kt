package com.example.watermarkcamera

import android.app.Dialog
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class WatermarkStyleBottomSheet : BottomSheetDialogFragment() {

    private lateinit var addressResolver: AddressResolver
    private var currentLocation: Location? = null
    private var onStyleSelected: ((WatermarkStyle) -> Unit)? = null

    companion object {
        private const val ARG_LOCATION = "location"

        fun newInstance(location: Location?, onStyleSelected: (WatermarkStyle) -> Unit): WatermarkStyleBottomSheet {
            return WatermarkStyleBottomSheet().apply {
                this.currentLocation = location
                this.onStyleSelected = onStyleSelected
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.5).toInt()
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_watermark_style, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        addressResolver = AddressResolver(requireContext())

        view.findViewById<View>(R.id.styleAttendance).setOnClickListener {
            handleStyleSelection(WatermarkStyleType.ATTENDANCE)
        }

        view.findViewById<View>(R.id.styleGeneral).setOnClickListener {
            handleStyleSelection(WatermarkStyleType.GENERAL)
        }

        view.findViewById<View>(R.id.styleSimple).setOnClickListener {
            handleStyleSelection(WatermarkStyleType.SIMPLE)
        }

        view.findViewById<View>(R.id.styleTime).setOnClickListener {
            handleStyleSelection(WatermarkStyleType.TIME)
        }

        view.findViewById<View>(R.id.styleCustom).setOnClickListener {
            showCustomInputDialog()
        }
    }

    private fun handleStyleSelection(styleType: WatermarkStyleType) {
        if (currentLocation == null) {
            Toast.makeText(requireContext(), "正在获取位置信息...", Toast.LENGTH_SHORT).show()
            return
        }

        when (styleType) {
            WatermarkStyleType.ATTENDANCE -> showRemarkInputDialog(styleType)
            else -> generateAndApplyWatermark(styleType, null)
        }
    }

    private fun showRemarkInputDialog(styleType: WatermarkStyleType) {
        val input = EditText(requireContext()).apply {
            hint = "请输入备注（可选）"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("备注")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val remark = input.text.toString().ifEmpty { "无" }
                generateAndApplyWatermark(styleType, remark)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showCustomInputDialog() {
        val input = EditText(requireContext()).apply {
            hint = "请输入自定义水印文本"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("自定义水印")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val customText = input.text.toString()
                if (customText.isNotEmpty()) {
                    val style = WatermarkStyle(
                        type = WatermarkStyleType.CUSTOM,
                        text = customText
                    )
                    onStyleSelected?.invoke(style)
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "请输入水印文本", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun generateAndApplyWatermark(styleType: WatermarkStyleType, remark: String?) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val watermarkText = withContext(Dispatchers.IO) {
                    buildWatermarkText(styleType, remark)
                }
                
                val style = WatermarkStyle(
                    type = styleType,
                    text = watermarkText,
                    location = currentLocation
                )
                
                onStyleSelected?.invoke(style)
                dismiss()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "生成水印失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun buildWatermarkText(styleType: WatermarkStyleType, remark: String?): String {
        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        val weekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        
        val time = timeFormat.format(calendar.time)
        val date = dateFormat.format(calendar.time)
        val week = weekFormat.format(calendar.time).replace("星期", "")
        
        val location = currentLocation
        val latitude = location?.latitude?.let { "%.6f".format(it) } ?: "未知"
        val longitude = location?.longitude?.let { "%.6f".format(it) } ?: "未知"
        
        val address = if (location != null) {
            try {
                addressResolver.getAddressFromLocation(location.latitude, location.longitude) 
                    ?: "地址获取失败"
            } catch (e: Exception) {
                "地址获取失败"
            }
        } else {
            "位置未授权"
        }

        return when (styleType) {
            WatermarkStyleType.ATTENDANCE -> {
                """
                |考勤打卡 $time
                |$date 星期$week
                |$address
                |经度:$longitude,纬度:$latitude
                |备注:${remark ?: "无"}
                """.trimMargin()
            }
            
            WatermarkStyleType.GENERAL -> {
                "$time $date $address 经度:$longitude,纬度:$latitude"
            }
            
            WatermarkStyleType.SIMPLE -> {
                "$time $date 星期$week"
            }
            
            WatermarkStyleType.TIME -> {
                "TIME:$time\n$date $address"
            }
            
            else -> ""
        }
    }

    data class WatermarkStyle(
        val type: WatermarkStyleType,
        val text: String,
        val location: Location? = null
    )

    enum class WatermarkStyleType {
        ATTENDANCE,
        GENERAL,
        SIMPLE,
        TIME,
        CUSTOM
    }
}
