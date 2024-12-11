package com.wenjie.traffic

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager2: ViewPager2
    private lateinit var tab: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        viewPager2 = findViewById(R.id.viewpager2)
        tab = findViewById(R.id.tab)

        viewPager2.adapter = MyPagerAdapter(
            listOf(
                DataListFragment.newInstance(ConnectivityManager.TYPE_WIFI),
                DataListFragment.newInstance(ConnectivityManager.TYPE_MOBILE)
            ), this
        )

        TabLayoutMediator(tab, viewPager2) { tab, position ->
            if (position == 0) tab.text = "Wifi"
            if (position == 1) tab.text = "Mobile"
        }.attach()

        if (!checkOpNoThrow()) {
            startActivityForResult(getPackagePermissionIntent(this), 1)
        }
    }

    /**
     * 获取使用统计权限设置界面意图
     */
    private fun getPackagePermissionIntent(context: Context): Intent {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.setData(Uri.parse("package:" + context.packageName))
        return intent
    }

    private fun checkOpNoThrow(): Boolean {
        val opName = AppOpsManager.OPSTR_GET_USAGE_STATS
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(opName, applicationInfo.uid, packageName)
        } else {
            appOps.checkOpNoThrow(opName, applicationInfo.uid, packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

}