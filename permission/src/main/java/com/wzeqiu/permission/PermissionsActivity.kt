package com.wzeqiu.permission

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.common.kt.singleClick
import com.common.ui.BaseActivity
import com.gyf.immersionbar.ktx.immersionBar
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.wzeqiu.permission.databinding.ActivityPermissionsBinding

class PermissionsActivity : BaseActivity<ActivityPermissionsBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.apply {
            immersionBar { titleBarMarginTop(main) }
        }
        binding.button.singleClick {

            XXPermissions.with(this@PermissionsActivity)
                .permission(PermissionLists.getWriteExternalStoragePermission())
                .request { permissions, allGranted ->
                    if (allGranted) {
                        Log.e("AAAA", "权限申请成功")
                    }

                }

        }
    }
}