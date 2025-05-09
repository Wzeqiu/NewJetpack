package com.wzeqiu.newjetpack

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.Gravity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.common.kt.singleClick
import com.common.ui.BaseActivity
import com.common.ui.media.MediaInfo
import com.common.ui.media.MediaManageActivity
import com.common.ui.webView.WebViewActivity
import com.common.kt.activity.launch
import com.wzeqiu.newjetpack.databinding.ActivityMainBinding
import com.example.newjetpack.utils.PrivacyFriendlyAccessHelper // 新增导入
import android.widget.Toast // 新增导入
import android.net.Uri // 新增导入

class MainActivity : BaseActivity<ActivityMainBinding>() {

    // 使用 PrivacyFriendlyAccessHelper 注册启动器
    private lateinit var pickContactLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest>
    private lateinit var pickDocumentLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var requestLocationPermissionLauncher: ActivityResultLauncher<String>
    private var contactLauncher: ActivityResultLauncher<Intent>  = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult(),
    activityResultRegistry) { result ->
        if (result.data == null) {
            return@registerForActivityResult
        }
            result.data?.data?.let {
            contentResolver.query(it, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    var number = ""
                    var name = ""
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (numberIndex != -1) {
                        number = cursor.getString(numberIndex)
                    }
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                    Log.e("AAAA", "name===$name")
                    Log.e("AAAA", "number===$number")
                } else {
                    // 如果不能获取，可以让手动输入
                    Log.e("AAAA", " 如果不能获取，可以让手动输入")
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化启动器
        pickContactLauncher = PrivacyFriendlyAccessHelper.registerPickContactLauncher(this) {
            uri -> handleContactPicked(uri)
        }
        pickImageLauncher = PrivacyFriendlyAccessHelper.registerPickImageOrVideoLauncher(this) {
            uri -> handleMediaPicked(uri, "Image")
        }
        pickDocumentLauncher = PrivacyFriendlyAccessHelper.registerPickDocumentLauncher(this) {
            uri -> handleMediaPicked(uri, "Document")
        }
        requestLocationPermissionLauncher = PrivacyFriendlyAccessHelper.registerLocationPermissionLauncher(this) {
            isGranted -> handleLocationPermissionResult(isGranted)
        }

        binding.textView.singleClick {
            contactLauncher.launch(
                Intent(
                    Intent.ACTION_PICK,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                )
            )
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

        // 为新按钮设置点击事件
        binding.btnPickContact.singleClick {
            PrivacyFriendlyAccessHelper.launchContactPicker(pickContactLauncher)
        }

        binding.btnPickImage.singleClick {
            PrivacyFriendlyAccessHelper.launchImagePicker(pickImageLauncher) // 或者 launchImageAndVideoPicker
        }

        binding.btnPickDocument.singleClick {
            PrivacyFriendlyAccessHelper.launchDocumentPicker(pickDocumentLauncher, arrayOf("application/pdf", "image/*")) // 示例：PDF和图片
        }

        binding.btnRequestLocation.singleClick {
            PrivacyFriendlyAccessHelper.requestCoarseLocationPermission(
                this@MainActivity,
                requestLocationPermissionLauncher,
                onPermissionGranted = {
                    Toast.makeText(this@MainActivity, "粗略位置权限已授予", Toast.LENGTH_SHORT).show()
                    // 在这里获取位置信息
                    Log.d("LocationDemo", "粗略位置权限已授予，可以获取位置了")
                },
                onPermissionDenied = {
                    Toast.makeText(this@MainActivity, "粗略位置权限被拒绝", Toast.LENGTH_SHORT).show()
                    Log.d("LocationDemo", "粗略位置权限被拒绝")
                }
            )
        }
    }

    /**
     * 处理选择的联系人URI
     * @param contactUri 选择的联系人URI，如果用户未选择则为null
     */
    private fun handleContactPicked(contactUri: Uri?) {
        if (contactUri != null) {
            // 查询联系人信息
            contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val name = if (nameIndex != -1) cursor.getString(nameIndex) else "N/A"
                    val number = if (numberIndex != -1) cursor.getString(numberIndex) else "N/A"
                    val contactId = if (idIndex != -1) cursor.getString(idIndex) else "N/A"
                    Log.d("ContactPicker", "Selected Contact: ID=$contactId, Name=$name, Uri=$contactUri")
                    Toast.makeText(this, "选择的联系人: $name  $number", Toast.LENGTH_LONG).show()

                    // 如果需要电话号码，可以进一步查询 ContactsContract.CommonDataKinds.Phone
                    // 注意：这可能需要 READ_CONTACTS 权限，取决于具体实现和Android版本
                    // 对于金融类应用，通常只获取用户主动选择的联系人的基本信息（如姓名）是更安全的做法
                }
            }
        } else {
            Log.d("ContactPicker", "No contact selected")
            Toast.makeText(this, "未选择联系人", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理选择的媒体（图片/文档）URI
     * @param mediaUri 选择的媒体URI，如果用户未选择则为null
     * @param type 媒体类型 ("Image", "Document")
     */
    private fun handleMediaPicked(mediaUri: Uri?, type: String) {
        if (mediaUri != null) {
            Log.d("${type}Picker", "Selected $type URI: $mediaUri")
            Toast.makeText(this, "选择的 $type: $mediaUri", Toast.LENGTH_LONG).show()
            // 在这里处理URI，例如显示图片或上传文件
            // 对于图片，可以使用 binding.imageView.setImageURI(mediaUri)
            // 对于文件，可以获取输入流 contentResolver.openInputStream(mediaUri)
        } else {
            Log.d("${type}Picker", "No $type selected")
            Toast.makeText(this, "未选择 $type", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理位置权限请求结果
     * @param isGranted 权限是否被授予
     */
    private fun handleLocationPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            Toast.makeText(this, "位置权限已通过启动器授予", Toast.LENGTH_SHORT).show()
            Log.d("LocationDemo", "位置权限已通过启动器授予，可以获取位置了")
            // 再次调用 requestCoarseLocationPermission 中的 onPermissionGranted 逻辑（如果需要）
            // 或者直接在这里执行获取位置的操作
        } else {
            Toast.makeText(this, "位置权限已通过启动器拒绝", Toast.LENGTH_SHORT).show()
            Log.d("LocationDemo", "位置权限已通过启动器拒绝")
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