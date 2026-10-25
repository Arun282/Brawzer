package com.arun.browser

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.net.URLEncoder

class MainActivity : Activity() {
    private data class BrowserTab(val id: Int, val view: WebView, var title: String = "New tab", var url: String = "about:blank")
    private val tabs = mutableListOf<BrowserTab>()
    private var activeTabId = 0
    private var nextTabId = 1
    private var incognito = false

    private lateinit var address: EditText
    private lateinit var progress: ProgressBar
    private lateinit var status: TextView
    private lateinit var webContainer: LinearLayout
    private lateinit var tabStrip: LinearLayout
    private lateinit var bookmarkButton: Button

    private val purple = Color.rgb(102, 82, 235)
    private val dark = Color.rgb(20, 22, 35)
    private val card = Color.rgb(43, 45, 62)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = dark
        window.navigationBarColor = dark
        buildUi()
        createTab("https://www.google.com")
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(7), dp(8), dp(7))
            setBackgroundColor(dark)
        }
        val brand = TextView(this).apply {
            text = "A"
            textSize = 21f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(purple)
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(40)).apply { marginEnd = dp(7) }
        }
        header.addView(brand)
        address = EditText(this).apply {
            hint = "Search or enter website"
            singleLine = true
            textSize = 13f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(dp(10), 0, dp(6), 0)
            setBackgroundColor(card)
            imeOptions = EditorInfo.IME_ACTION_GO
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f)
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER)) {
                    loadFromAddress()
                    true
                } else false
            }
        }
        header.addView(address)
        header.addView(smallButton("Go") { loadFromAddress() })
        root.addView(header)

        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(2))
            setBackgroundColor(dark)
        }
        addTool(tools, "＋ Tab") { createTab("https://www.google.com") }
        addTool(tools, "☆ Save") { saveBookmark() }
        bookmarkButton = Button(this).apply {
            text = "Bookmarks"
            textSize = 10f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(dark)
            layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f)
            setOnClickListener { showBookmarks() }
        }
        tools.addView(bookmarkButton)
        addTool(tools, if (incognito) "Incognito ✓" else "Incognito") { toggleIncognito() }
        addTool(tools, "Downloads") { showDownloads() }
        root.addView(tools)

        val tabScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Color.rgb(29, 31, 47))
        }
        tabStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(5), dp(4), dp(5), dp(4))
        }
        tabScroll.addView(tabStrip)
        root.addView(tabScroll)

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = android.content.res.ColorStateList.valueOf(purple)
            visibility = View.GONE
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(3)))
        webContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        root.addView(webContainer)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(dark)
            setPadding(dp(3), dp(4), dp(3), dp(4))
        }
        addTool(controls, "‹ Back") { activeTab()?.let { if (it.view.canGoBack()) it.view.goBack() } }
        addTool(controls, "Forward ›") { activeTab()?.let { if (it.view.canGoForward()) it.view.goForward() } }
        addTool(controls, "↻ Reload") { activeTab()?.view?.reload() }
        addTool(controls, "Home") { activeTab()?.view?.loadUrl("https://www.google.com") }
        addTool(controls, "✕ Tab") { closeActiveTab() }
        root.addView(controls)
        status = TextView(this).apply {
            text = "Arun Browser"
            textSize = 10f
            setTextColor(Color.LTGRAY)
            setBackgroundColor(dark)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(3), dp(10), dp(6))
        }
        root.addView(status)
        setContentView(root)
    }

    private fun smallButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 10f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(purple)
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(42)).apply { marginStart = dp(5) }
            setOnClickListener { action() }
        }

    private fun addTool(parent: LinearLayout, label: String, action: () -> Unit) {
        parent.addView(Button(this).apply {
            text = label
            textSize = 9.5f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(dark)
            layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f)
            setOnClickListener { action() }
        })
    }

    private fun createTab(url: String) {
        val id = nextTabId++
        val view = WebView(this)
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.settings.loadsImagesAutomatically = true
        view.settings.javaScriptCanOpenWindowsAutomatically = false
        view.settings.setSupportMultipleWindows(false)
        view.settings.builtInZoomControls = true
        view.settings.displayZoomControls = false
        view.settings.allowFileAccess = false
        view.settings.allowContentAccess = true
        if (incognito) {
            view.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            view.clearCache(true)
            view.clearHistory()
            CookieManager.getInstance().setAcceptCookie(false)
        } else {
            view.settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            CookieManager.getInstance().setAcceptCookie(true)
        }
        val tab = BrowserTab(id, view)
        tabs.add(tab)
        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme?.lowercase()
                if (scheme == "http" || scheme == "https") return false
                Toast.makeText(this@MainActivity, "This link type isn't supported.", Toast.LENGTH_SHORT).show()
                return true
            }
            override fun onPageStarted(v: WebView, url: String, favicon: android.graphics.Bitmap?) {
                tab.url = url
                if (activeTabId == tab.id) {
                    address.setText(url)
                    progress.visibility = View.VISIBLE
                    status.text = "Loading…"
                }
                renderTabs()
            }
            override fun onPageFinished(v: WebView, url: String) {
                tab.url = url
                tab.title = v.title?.take(35)?.ifBlank { url } ?: url
                if (activeTabId == tab.id) {
                    address.setText(url)
                    progress.visibility = View.GONE
                    status.text = tab.title
                }
                renderTabs()
            }
        }
        view.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(v: WebView, newProgress: Int) {
                if (activeTabId == tab.id) {
                    progress.progress = newProgress
                    progress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
                }
            }
        }
        view.setDownloadListener(DownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, _ ->
            startDownload(downloadUrl, userAgent, contentDisposition, mimeType)
        })
        webContainer.addView(view, LinearLayout.LayoutParams(-1, -1))
        selectTab(id)
        view.loadUrl(url)
    }

    private fun renderTabs() {
        tabStrip.removeAllViews()
        tabs.forEach { tab ->
            val selected = tab.id == activeTabId
            val tabButton = Button(this).apply {
                text = (if (selected) "● " else "") + tab.title.take(18) + "  ×"
                textSize = 10f
                isAllCaps = false
                setTextColor(Color.WHITE)
                setBackgroundColor(if (selected) purple else card)
                layoutParams = LinearLayout.LayoutParams(dp(140), dp(36)).apply { marginEnd = dp(5) }
                setOnClickListener {
                    if (tab.id == activeTabId) closeTab(tab.id) else selectTab(tab.id)
                }
                setOnLongClickListener {
                    selectTab(tab.id)
                    true
                }
            }
            tabStrip.addView(tabButton)
        }
    }

    private fun selectTab(id: Int) {
        val tab = tabs.firstOrNull { it.id == id } ?: return
        activeTabId = id
        tabs.forEach { item -> item.view.visibility = if (item.id == id) View.VISIBLE else View.GONE }
        address.setText(if (tab.url == "about:blank") "" else tab.url)
        status.text = tab.title
        renderTabs()
    }

    private fun activeTab(): BrowserTab? = tabs.firstOrNull { it.id == activeTabId }
    private fun closeActiveTab() = closeTab(activeTabId)

    private fun closeTab(id: Int) {
        val tab = tabs.firstOrNull { it.id == id } ?: return
        if (tabs.size == 1) {
            tab.view.loadUrl("https://www.google.com")
            tab.title = "Google"
            tab.url = "https://www.google.com"
            renderTabs()
            return
        }
        val index = tabs.indexOf(tab)
        webContainer.removeView(tab.view)
        tab.view.stopLoading()
        tab.view.destroy()
        tabs.remove(tab)
        if (activeTabId == id) {
            val next = tabs.getOrNull((index - 1).coerceAtLeast(0)) ?: tabs.first()
            selectTab(next.id)
        } else renderTabs()
    }

    private fun loadFromAddress() {
        val raw = address.text.toString().trim()
        if (raw.isEmpty()) return
        val url = when {
            raw.startsWith("https://", true) || raw.startsWith("http://", true) -> raw
            raw.matches(Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(:[0-9]+)?(/.*)?$")) -> "https://$raw"
            else -> "https://www.google.com/search?q=" + URLEncoder.encode(raw, "UTF-8")
        }
        val keyboard = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        keyboard.hideSoftInputFromWindow(address.windowToken, 0)
        activeTab()?.view?.loadUrl(url) ?: createTab(url)
    }

    private fun saveBookmark() {
        val tab = activeTab() ?: return
        val url = tab.view.url ?: return
        val prefs = getSharedPreferences("arun_browser", MODE_PRIVATE)
        val values = prefs.getStringSet("bookmarks", emptySet())?.toMutableSet() ?: mutableSetOf()
        values.add(url)
        prefs.edit().putStringSet("bookmarks", values).apply()
        Toast.makeText(this, "Bookmark saved", Toast.LENGTH_SHORT).show()
    }

    private fun showBookmarks() {
        val values = getSharedPreferences("arun_browser", MODE_PRIVATE)
            .getStringSet("bookmarks", emptySet())?.toList().orEmpty()
        if (values.isEmpty()) {
            Toast.makeText(this, "No bookmarks saved yet", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = values.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("Bookmarks")
            .setItems(labels) { _, which ->
                val url = labels[which]
                activeTab()?.view?.loadUrl(url) ?: createTab(url)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun toggleIncognito() {
        incognito = !incognito
        Toast.makeText(
            this,
            if (incognito) "Incognito enabled for new tabs. Existing tabs are unchanged." else "Incognito disabled for new tabs.",
            Toast.LENGTH_LONG
        ).show()
        createTab("https://www.google.com")
    }

    private fun startDownload(url: String, userAgent: String, disposition: String?, mimeType: String?) {
        try {
            val filename = URLUtil.guessFileName(url, disposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url))
                .setMimeType(mimeType)
                .addRequestHeader("User-Agent", userAgent)
                .setTitle(filename)
                .setDescription("Downloading with Arun Browser")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val id = manager.enqueue(request)
            val prefs = getSharedPreferences("arun_browser", MODE_PRIVATE)
            val old = prefs.getStringSet("downloads", emptySet())?.toMutableSet() ?: mutableSetOf()
            old.add("$id|$filename|$url")
            prefs.edit().putStringSet("downloads", old).apply()
            Toast.makeText(this, "Download started: $filename", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't start download: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDownloads() {
        val values = getSharedPreferences("arun_browser", MODE_PRIVATE)
            .getStringSet("downloads", emptySet())?.toList().orEmpty()
        if (values.isEmpty()) {
            Toast.makeText(this, "No downloads started in this app yet", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = values.map { it.split("|").getOrElse(1) { it } }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("Download history")
            .setItems(labels) { _, which ->
                val parts = values[which].split("|", limit = 3)
                if (parts.size >= 2) {
                    val id = parts[0].toLongOrNull()
                    if (id != null) {
                        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        try {
                            startActivity(manager.getUriForDownloadedFile(id)?.let {
                                android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(it, contentResolver.getType(it))
                                    .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            } ?: android.content.Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                        } catch (_: Exception) {
                            startActivity(android.content.Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                        }
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("incognito", incognito)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val tab = activeTab()
        if (tab?.view?.canGoBack() == true) tab.view.goBack()
        else if (tabs.size > 1) closeActiveTab()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        tabs.forEach { it.view.stopLoading(); it.view.destroy() }
        tabs.clear()
        super.onDestroy()
    }
}