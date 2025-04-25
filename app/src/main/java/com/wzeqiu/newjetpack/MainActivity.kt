package com.wzeqiu.newjetpack

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.activity.result.contract.ActivityResultContracts
import com.common.kt.singleClick
import com.common.ui.BaseActivity
import com.common.ui.media.MediaInfo
import com.common.ui.media.MediaManageActivity
import com.common.ui.webView.WebViewActivity
import com.common.kt.activity.launch
import com.wzeqiu.newjetpack.databinding.ActivityMainBinding


class MainActivity : BaseActivity<ActivityMainBinding>() {
    private val videoSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val info = it.data?.getParcelableExtra<MediaInfo>(MediaManageActivity.RESULT_DATA)
            info?.let { }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.textView.singleClick {
            TextDialog()
                .setWidthRatio(1f)
                .setBackgroundAlpha(0.5f)
                .setCancelableOnBackPress(false)
                .setCancelableOnTouchOutside(false)
                .setGravity(Gravity.CENTER)
                .show(supportFragmentManager, "textdialog")
//            val spring = SpringForce(360f)
//                .setDampingRatio(SpringForce.DAMPING_RATIO_HIGH_BOUNCY)
//                .setStiffness(SpringForce.STIFFNESS_VERY_LOW)
//            val anim = SpringAnimation(binding.iv, DynamicAnimation.ROTATION)
//            anim.setStartValue(0f)
//            anim.spring = spring
//            anim.start()
        }

        // 添加按钮点击事件，打开引导页示例
        binding.btnGuideDemo.singleClick {
            launch<GuidePageDemoActivity> {
                if (it.resultCode == RESULT_OK) {
                    Log.e("AAAA", "DATA===${it.data?.getStringExtra("data")}")
                }
            }
        }
        // 添加按钮点击事件ShapeDemo
        binding.btnShapeDemo.singleClick {
            startActivity(Intent(this@MainActivity, ShapeDemoActivity::class.java))
        }
        // 添加按钮点击事件TablayoutDemo
        binding.btnTablayoutDemo.singleClick {
            startActivity(Intent(this@MainActivity, TabLayoutDemoActivity::class.java))
        }
        // 添加按钮点击事件TablayoutDemo
        binding.btnWebViewDemo.singleClick {

            startActivity(Intent(this@MainActivity, WebViewActivity::class.java))
        }
    }


//    fun modifyChannelColor(srcBitmap: Bitmap, color: Int): Bitmap {
//        // 创建一个和原位图一样大小的位图
//        val resultBitmap =
//            Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), srcBitmap.getConfig())
//
//        // 创建一个画布，在新位图上绘制
//        val canvas = Canvas(resultBitmap)
//
//        // 创建一个画笔，用于填充非透明区域
//        val paint = Paint()
//        paint.setColor(color)
//
//        // 定义渐变的起始和结束颜色
//        val startColor = 0xFFE91E63.toInt() // 粉红色
//        val endColor = 0xFF2196F3.toInt()   // 蓝色
//
//// 定义渐变的方向（从左上角到右下角）
//        val x0 = 0f
//        val y0 = 0f
//        val x1 = resultBitmap.width.toFloat()
//        val y1 = resultBitmap.height.toFloat()
//        val colors1 = intArrayOf(
//            0xFFE91E63.toInt(), // 粉红色
//            0xFF2196F3.toInt(), // 蓝色
//            0xFFFFEB3B.toInt(), // 黄色
//            0xFF4CAF50.toInt()  // 绿色
//        )
//        val linearGradient = LinearGradient(x0, y0, x1, y1, colors1, null, Shader.TileMode.CLAMP)
//
////        val centerX = x1 / 2f
////        val centerY = y1 / 2f
////        val radius = Math.min(x1, y1) / 2f
////        val radialGradient = RadialGradient(centerX, centerY, radius, startColor, endColor, Shader.TileMode.CLAMP)
//
//
//        val centerX = x1 / 2f
//        val centerY = y1 / 2f
//
//// 定义渐变的颜色数组
//        val colors = intArrayOf(
//            0xFFE91E63.toInt(), // 粉红色
//            0xFF2196F3.toInt(), // 蓝色
//            0xFFFFEB3B.toInt(), // 黄色
//            0xFF4CAF50.toInt()  // 绿色
//        )
//
//// 创建 SweepGradient 对象
//        val sweepGradient = SweepGradient(centerX, centerY, colors, null)
//        paint.shader = linearGradient
//        // 创建一个PorterDuffXfermode对象，用于指定合成模式
//        val porterDuffXfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
//
//
//        canvas.withRotation(180f, centerX, centerY) {
//
//            canvas.withSave {
//
//                canvas.drawBitmap(srcBitmap, 0f, 0f, paint)
//
//                // 应用合成模式
//                paint.setXfermode(porterDuffXfermode)
//                // 绘制一个与原位图大小相同的白色矩形，这将应用SRC_IN模式，将原位图的非透明区域替换为指定颜色
//                canvas.drawRect(
//                    0f,
//                    0f,
//                    srcBitmap.getWidth().toFloat(),
//                    srcBitmap.getHeight().toFloat(),
//                    paint
//                )
//
//                // 清除合成模式
//                paint.setXfermode(null)
//            }
//        }
//        return resultBitmap
//    }

}