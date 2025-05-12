package com.wzeqiu.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * 隐私友好型数据访问辅助类
 */
object PrivacyFriendlyAccessHelper {

    // --- 通讯录访问 --- 

    /**
     * 注册一个用于选择联系人的 ActivityResultLauncher。
     * @param activity AppCompatActivity 实例
     * @param onContactPicked 回调函数，参数为选择的联系人 URI，如果用户未选择则为 null
     * @return ActivityResultLauncher<Intent> 用于启动联系人选择器
     */
    fun registerPickContactLauncher(
        activity: AppCompatActivity,
        onContactPicked: (Uri?) -> Unit
    ): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                onContactPicked(it.data?.data)
            } else {
                onContactPicked(null)
            }
        }
    }

    /**
     * 注册一个用于选择联系人的 ActivityResultLauncher (Fragment 版本)。
     * @param fragment Fragment 实例
     * @param onContactPicked 回调函数，参数为选择的联系人 URI，如果用户未选择则为 null
     * @return ActivityResultLauncher<Intent> 用于启动联系人选择器
     */
    fun registerPickContactLauncher(
        fragment: Fragment,
        onContactPicked: (Uri?) -> Unit
    ): ActivityResultLauncher<Intent> {
        return fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                onContactPicked(it.data?.data)
            } else {
                onContactPicked(null)
            }
        }
    }

    /**
     * 启动联系人选择器。
     * @param launcher 通过 registerPickContactLauncher注册的启动器
     */
    fun launchContactPicker(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        launcher.launch(intent)
    }

    // --- 图片/视频选择 (照片选择器) ---

    /**
     * 注册一个用于选择单个图片/视频的 ActivityResultLauncher (照片选择器)。
     * @param activity AppCompatActivity 实例
     * @param onMediaPicked 回调函数，参数为选择的媒体 URI，如果用户未选择则为 null
     * @return ActivityResultLauncher<PickVisualMediaRequest> 用于启动照片选择器
     */
    fun registerPickImageOrVideoLauncher(
        activity: AppCompatActivity,
        onMediaPicked: (Uri?) -> Unit
    ): ActivityResultLauncher<PickVisualMediaRequest> {
        return activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            onMediaPicked(uri)
        }
    }

    /**
     * 注册一个用于选择单个图片/视频的 ActivityResultLauncher (照片选择器) (Fragment 版本)。
     * @param fragment Fragment 实例
     * @param onMediaPicked 回调函数，参数为选择的媒体 URI，如果用户未选择则为 null
     * @return ActivityResultLauncher<PickVisualMediaRequest> 用于启动照片选择器
     */
    fun registerPickImageOrVideoLauncher(
        fragment: Fragment,
        onMediaPicked: (Uri?) -> Unit
    ): ActivityResultLauncher<PickVisualMediaRequest> {
        return fragment.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            onMediaPicked(uri)
        }
    }

    /**
     * 启动照片选择器选择单个图片。
     * @param launcher 通过 registerPickImageOrVideoLauncher 注册的启动器
     */
    fun launchImagePicker(launcher: ActivityResultLauncher<PickVisualMediaRequest>) {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    /**
     * 启动照片选择器选择单个视频。
     * @param launcher 通过 registerPickImageOrVideoLauncher 注册的启动器
     */
    fun launchVideoPicker(launcher: ActivityResultLauncher<PickVisualMediaRequest>) {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }

    /**
     * 启动照片选择器选择单个图片或视频。
     * @param launcher 通过 registerPickImageOrVideoLauncher 注册的启动器
     */
    fun launchImageAndVideoPicker(launcher: ActivityResultLauncher<PickVisualMediaRequest>) {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }

    // --- 通用文件选择 (Storage Access Framework) ---

    /**
     * 注册一个用于选择单个通用文件的 ActivityResultLauncher (SAF)。
     * @param activity AppCompatActivity 实例
     * @param onFilePicked 回调函数，参数为选择的文件 URI，如果用户未选择则为 null
     * @return ActivityResultLauncher<Array<String>> 用于启动文件选择器
     */
    fun registerPickDocumentLauncher(
        activity: AppCompatActivity,
        onFilePicked: (Uri?) -> Unit
    ): ActivityResultLauncher<Array<String>> {
        return activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            onFilePicked(uri)
        }
    }

    /**
     * 注册一个用于选择单个通用文件的 ActivityResultLauncher (SAF) (Fragment 版本)。
     * @param fragment Fragment 实例
     * @param onFilePicked 回调函数，参数为选择的文件 URI，如果用户未选择则为 null
     * @return ActivityResultLauncher<Array<String>> 用于启动文件选择器
     */
    fun registerPickDocumentLauncher(
        fragment: Fragment,
        onFilePicked: (Uri?) -> Unit
    ): ActivityResultLauncher<Array<String>> {
        return fragment.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            onFilePicked(uri)
        }
    }

    /**
     *   * 启动 SAF 文件选择器选择单个文件。
     *      * @param launcher 通过 registerPickDocumentLauncher 注册的启动器
     *      * @param mimeTypes 允许选择的文件 MIME 类型数组，例如 arrayOf("image\*", "application//pdf")
     */

    fun launchDocumentPicker(
        launcher: ActivityResultLauncher<Array<String>>,
        mimeTypes: Array<String> = arrayOf("*")
    ) {
        launcher.launch(mimeTypes)
    }

// --- 位置信息 ---

    /**
     * 注册一个用于请求位置权限的 ActivityResultLauncher。
     * @param activity AppCompatActivity 实例
     * @param onPermissionResult 回调函数，参数为权限是否被授予 (Boolean)
     * @return ActivityResultLauncher<String> 用于请求权限
     */
    fun registerLocationPermissionLauncher(
        activity: AppCompatActivity,
        onPermissionResult: (Boolean) -> Unit
    ): ActivityResultLauncher<String> {
        return activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            onPermissionResult(isGranted)
        }
    }

    /**
     * 注册一个用于请求位置权限的 ActivityResultLauncher (Fragment 版本)。
     * @param fragment Fragment 实例
     * @param onPermissionResult 回调函数，参数为权限是否被授予 (Boolean)
     * @return ActivityResultLauncher<String> 用于请求权限
     */
    fun registerLocationPermissionLauncher(
        fragment: Fragment,
        onPermissionResult: (Boolean) -> Unit
    ): ActivityResultLauncher<String> {
        return fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            onPermissionResult(isGranted)
        }
    }

    /**
     * 检查并请求粗略位置权限。
     * @param context Context 实例
     * @param launcher 通过 registerLocationPermissionLauncher 注册的启动器
     * @param onPermissionGranted 当权限已被授予或成功请求后回调
     * @param onPermissionDenied 当权限被拒绝时回调 (可选)
     */
    fun requestCoarseLocationPermission(
        context: Context,
        launcher: ActivityResultLauncher<String>,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: (() -> Unit)? = null
    ) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                onPermissionGranted()
            }
            // TODO: 可以考虑添加一个 shouldShowRequestPermissionRationale 的处理逻辑
            else -> {
                launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    }

    /**
     * 检查并请求精确位置权限。
     * 注意：金融类App应优先使用粗略位置。仅在核心功能绝对必要时才请求精确位置，并向用户清晰说明原因。
     * @param context Context 实例
     * @param launcher 通过 registerLocationPermissionLauncher 注册的启动器 (通常是同一个，只是请求的权限字符串不同)
     * @param onPermissionGranted 当权限已被授予或成功请求后回调
     * @param onPermissionDenied 当权限被拒绝时回调 (可选)
     */
    fun requestFineLocationPermission(
        context: Context,
        launcher: ActivityResultLauncher<String>,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: (() -> Unit)? = null
    ) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                onPermissionGranted()
            }
            // TODO: 可以考虑添加一个 shouldShowRequestPermissionRationale 的处理逻辑
            else -> {
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

// 注意：获取具体位置数据的逻辑 (如使用 FusedLocationProviderClient) 应在权限被授予后执行，
// 通常在 Activity/Fragment 或专门的 LocationService 中实现。
// 此辅助类主要关注权限请求和通过 Intent 启动选择器。
}