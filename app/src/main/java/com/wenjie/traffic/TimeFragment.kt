package com.wenjie.traffic

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by wenjie on 2024/12/11.
 */
class TimeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tvMax: TextView

    private var isLoaded = false

    private val adapter by lazy { ApplicationAdapter(2) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recycler_view)
        swipeRefreshLayout = view.findViewById(R.id.swipe)
        tvMax = view.findViewById(R.id.tv_max)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        getAppUseDataList()
        swipeRefreshLayout.setOnRefreshListener {
            getAppUseDataList()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isLoaded) {
            getAppUseDataList()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getAppUseDataList() {
        if (checkOpNoThrow()) {
            lifecycleScope.launch {
                swipeRefreshLayout.isRefreshing = true
                val list = withContext(Dispatchers.IO) {
                    var sum: Long = 0.toLong()
                    val list = getInstalledApps(requireContext().packageManager).asSequence().map {
                        getUsage(it)
                    }.filter {
                        sum += it.time
                        it.time > 0
                    }.sortedByDescending {
                        it.time
                    }.toMutableList()
                    adapter.max = sum
                    list
                }
                tvMax.text = "今日总使用：${adapter.formatTime(adapter.max)}"
                swipeRefreshLayout.isRefreshing = false
                adapter.data = list.toMutableList()
                isLoaded = true
            }
        }
    }

    private fun getUsage(appInfoItem: AppInfoItem): AppInfoItem {
        val usageStatsManager =
            requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            1733875200000,
            System.currentTimeMillis()
        )
        appInfoItem.time = stats.sumOf {
            it.totalTimeInForeground
        }
        return appInfoItem
    }

    private fun checkOpNoThrow(): Boolean {
        val opName = AppOpsManager.OPSTR_GET_USAGE_STATS
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                opName, requireContext().applicationInfo.uid, requireContext().packageName
            )
        } else {
            appOps.checkOpNoThrow(
                opName, requireContext().applicationInfo.uid, requireContext().packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }


    private fun getInstalledApps(packageManager: PackageManager): List<AppInfoItem> {
        val installedApps = mutableListOf<AppInfoItem>()
        // 获取已安装的应用程序列表
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in packages) {
            // 检查是否声明了网络权限
            val hasNetworkPermission = packageManager.checkPermission(
                Manifest.permission.INTERNET, appInfo.packageName
            ) == PackageManager.PERMISSION_GRANTED
            if (hasNetworkPermission) {
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val icon = packageManager.getApplicationIcon(appInfo)
                val uid = appInfo.uid
                installedApps.add(
                    AppInfoItem(
                        name = appName,
                        icon = icon,
                        uid = uid,
                    )
                )
            }
        }
        return installedApps
    }

    companion object {

        fun newInstance(): TimeFragment {
            return TimeFragment().also {
                val bundle = Bundle()
                it.arguments = bundle
            }
        }
    }
}