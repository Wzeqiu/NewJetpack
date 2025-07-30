package com.wzeqiu.permission

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.common.kt.singleClick
import com.common.ui.BaseActivity
import com.gyf.immersionbar.ktx.immersionBar
import com.wzeqiu.permission.databinding.ActivityMainBinding

class MainActivity :BaseActivity<ActivityMainBinding>() {
    // 使用 PrivacyFriendlyAccessHelper 注册启动器
    private lateinit var pickContactLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest>
    private lateinit var pickDocumentLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var requestLocationPermissionLauncher: ActivityResultLauncher<String>
    private var contactLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
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

        binding.apply {
            immersionBar { titleBarMarginTop(main) }
        }
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

        binding.btnRequestPermissions.singleClick {
            startActivity(Intent(this@MainActivity, PermissionsActivity::class.java))

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


}