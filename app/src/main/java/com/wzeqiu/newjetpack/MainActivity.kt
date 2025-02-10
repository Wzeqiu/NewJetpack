package com.wzeqiu.newjetpack

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.withRotation
import androidx.core.graphics.withSave
import androidx.lifecycle.lifecycleScope
import com.common.kt.download.DownloadManager
import com.common.kt.download.DownloadManager.bindLifeDownloadFile
import com.common.kt.download.DownloadManager.downloadFile
import com.common.kt.singleClick
import com.common.kt.viewBinding
import com.wzeqiu.newjetpack.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date


class MainActivity : AppCompatActivity() {
    val viewBinding by viewBinding<ActivityMainBinding>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        var downloadJob: Job? = null
        viewBinding.textView.singleClick {
            if (downloadJob!=null){
                downloadJob?.cancel()
                return@singleClick
            }

             bindLifeDownloadFile("https://gips3.baidu.com/it/u=3886271102,3123389489&fm=3028&app=3028&f=JPEG&fmt=auto?w=1280&h=960",
              File(cacheDir,"${System.currentTimeMillis()}.jpg"),object :DownloadManager.DownloadListener{
                  override fun onDownloadComplete(targetFile: File) {
                      Log.e("AAAAA","onDownloadComplete===$targetFile")
                  }

                  override fun onDownloadCancel() {
                      Log.e("AAAAA","onDownloadCancel")
                  }

                  override fun onDownloadProgress(progress: Int) {
                      Log.e("AAAAA","onDownloadProgress==$progress")
                  }

                  override fun onDownloadStart() {
                      Log.e("AAAAA","onDownloadStart")
                  }
              }
          )
        }

    }


    fun modifyChannelColor(srcBitmap: Bitmap, color: Int): Bitmap {
        // 创建一个和原位图一样大小的位图
        val resultBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), srcBitmap.getConfig())

        // 创建一个画布，在新位图上绘制
        val canvas = Canvas(resultBitmap)

        // 创建一个画笔，用于填充非透明区域
        val paint = Paint()
        paint.setColor(color)

        // 定义渐变的起始和结束颜色
        val startColor = 0xFFE91E63.toInt() // 粉红色
        val endColor = 0xFF2196F3.toInt()   // 蓝色

// 定义渐变的方向（从左上角到右下角）
        val x0 = 0f
        val y0 = 0f
        val x1 = resultBitmap.width.toFloat()
        val y1 = resultBitmap.height.toFloat()
        val colors1= intArrayOf(
            0xFFE91E63.toInt(), // 粉红色
            0xFF2196F3.toInt(), // 蓝色
            0xFFFFEB3B.toInt(), // 黄色
            0xFF4CAF50.toInt()  // 绿色
        )
        val linearGradient = LinearGradient(x0, y0, x1, y1, colors1, null, Shader.TileMode.CLAMP)

//        val centerX = x1 / 2f
//        val centerY = y1 / 2f
//        val radius = Math.min(x1, y1) / 2f
//        val radialGradient = RadialGradient(centerX, centerY, radius, startColor, endColor, Shader.TileMode.CLAMP)


        val centerX = x1 / 2f
        val centerY = y1 / 2f

// 定义渐变的颜色数组
        val colors = intArrayOf(
            0xFFE91E63.toInt(), // 粉红色
            0xFF2196F3.toInt(), // 蓝色
            0xFFFFEB3B.toInt(), // 黄色
            0xFF4CAF50.toInt()  // 绿色
        )

// 创建 SweepGradient 对象
        val sweepGradient = SweepGradient(centerX, centerY, colors, null)
        paint.shader = linearGradient
        // 创建一个PorterDuffXfermode对象，用于指定合成模式
        val porterDuffXfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)


        canvas.withRotation(180f,centerX,centerY) {

            canvas.withSave {

                canvas.drawBitmap(srcBitmap, 0f, 0f, paint)

                // 应用合成模式
                paint.setXfermode(porterDuffXfermode)
                // 绘制一个与原位图大小相同的白色矩形，这将应用SRC_IN模式，将原位图的非透明区域替换为指定颜色
                canvas.drawRect(0f, 0f, srcBitmap.getWidth().toFloat(), srcBitmap.getHeight().toFloat(), paint)

                // 清除合成模式
                paint.setXfermode(null)
            }
        }
        return resultBitmap
    }

}