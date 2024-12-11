package com.wenjie.traffic

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Created by wenjie on 2024/12/11.
 */
class DataListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tvMax: TextView

    private var isLoaded = false

    private var type: Int = ConnectivityManager.TYPE_MOBILE

    private val adapter by lazy { ApplicationAdapter() }

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

        type = arguments?.getInt("type") ?: ConnectivityManager.TYPE_MOBILE

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
                        getNetworkUsage(it)
                    }.filter {
                        sum += it.use
                        it.use > 0
                    }.sortedByDescending {
                        it.use
                    }.toMutableList()
                    adapter.max = sum
                    list
                }
                tvMax.text = "今日总消耗：${adapter.formatSizeFromKB(adapter.max)}"
                swipeRefreshLayout.isRefreshing = false
                adapter.data = list.toMutableList()
                isLoaded = true
            }
        }
    }

    private fun getNetworkUsage(appInfoItem: AppInfoItem): AppInfoItem {
        val networkStatsManager =
            requireContext().getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val usageStats = mutableListOf<Pair<String, Long>>()
        // 移动网络类型
        val networkType = type
        try {
            val bucket = networkStatsManager.queryDetailsForUid(
                networkType, null, 1733875200000, System.currentTimeMillis(), appInfoItem.uid
            )
            while (bucket.hasNextBucket()) {
                val usageBucket = NetworkStats.Bucket()
                bucket.getNextBucket(usageBucket)
                val packageName = requireContext().packageManager.getNameForUid(usageBucket.uid)

                val dataUsage = usageBucket.rxBytes + usageBucket.txBytes
                Log.e(
                    "asd", "${convertMillisecondsToDate(usageBucket.startTimeStamp)}到${
                        convertMillisecondsToDate(usageBucket.endTimeStamp)
                    } packageName= $packageName dataUsage=$dataUsage"
                )
                usageStats.add(Pair(packageName ?: "Unknown", dataUsage))
            }
            appInfoItem.use = usageStats.sumOf {
                it.second / 1024
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

    fun convertMillisecondsToDate(milliseconds: Long): String {
        // 创建时间格式
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        // 将毫秒数转换为日期
        val date = Date(milliseconds)
        return dateFormat.format(date)
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

        fun newInstance(type: Int): DataListFragment {
            return DataListFragment().also {
                val bundle = Bundle()
                bundle.putInt("type", type)
                it.arguments = bundle
            }
        }
    }
}