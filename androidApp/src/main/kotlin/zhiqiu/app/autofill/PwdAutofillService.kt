package zhiqiu.app.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import java.io.File
import zhiqiu.app.MainActivity
import zhiqiu.app.R
import zhiqiu.app.data.AppBootstrap
import zhiqiu.app.data.AppSession
import zhiqiu.app.data.PasswordRepository

/**
 * 最小可用自动填充：保险库已解锁时，把条目作为可选 Dataset 提供。
 * 未解锁时弹出本应用解锁页（系统会再请求一次填充）。
 */
class PwdAutofillService : AutofillService() {
    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback,
    ) {
        ensureBootstrap()
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }
        val fields = parseFields(structure)
        if (fields.usernameId == null && fields.passwordId == null) {
            callback.onSuccess(null)
            return
        }

        if (!AppSession.session.isUnlocked) {
            val authIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_AUTOFILL, true)
            }
            val sender = PendingIntent.getActivity(
                this,
                0,
                authIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ).intentSender
            val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                setTextViewText(android.R.id.text1, getString(R.string.autofill_unlock))
            }
            val ids = listOfNotNull(fields.usernameId, fields.passwordId).toTypedArray()
            val response = FillResponse.Builder()
                .setAuthentication(ids, sender, presentation)
                .build()
            callback.onSuccess(response)
            return
        }

        val entries = runCatching { PasswordRepository().getAllDecrypted() }.getOrDefault(emptyList())
        if (entries.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val packageHint = structure.activityComponent?.packageName.orEmpty().lowercase()
        val hostHint = fields.webDomain?.lowercase().orEmpty()
        val ranked = entries.sortedByDescending { e ->
            val title = e.title.lowercase()
            when {
                hostHint.isNotEmpty() && title.contains(hostHint.substringBefore('.')) -> 3
                packageHint.isNotEmpty() && packageHint.contains(title.filter { it.isLetterOrDigit() }) -> 2
                title.any { it.isLetter() } && packageHint.contains(title.take(4).lowercase()) -> 1
                else -> 0
            }
        }

        val builder = FillResponse.Builder()
        ranked.take(10).forEach { entry ->
            val label = "${entry.title} · ${entry.username}"
            val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                setTextViewText(android.R.id.text1, label)
            }
            val dataset = Dataset.Builder(presentation)
            fields.usernameId?.let {
                dataset.setValue(it, AutofillValue.forText(entry.username), presentation)
            }
            fields.passwordId?.let {
                dataset.setValue(it, AutofillValue.forText(entry.password), presentation)
            }
            builder.addDataset(dataset.build())
        }
        callback.onSuccess(builder.build())
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // 暂不支持从网页/App 回写新建条目
        callback.onSuccess()
    }

    private fun ensureBootstrap() {
        if (AppBootstrap.isReady()) return
        val dir = File(filesDir, "PwdStore").also { it.mkdirs() }
        AppBootstrap.init(dir.absolutePath)
    }

    private data class Fields(
        val usernameId: AutofillId?,
        val passwordId: AutofillId?,
        val webDomain: String?,
    )

    private fun parseFields(structure: AssistStructure): Fields {
        var username: AutofillId? = null
        var password: AutofillId? = null
        var domain: String? = null
        fun walk(node: AssistStructure.ViewNode) {
            val hint = (node.autofillHints?.joinToString(",") ?: "").lowercase()
            val idEntry = node.idEntry?.lowercase().orEmpty()
            val html = node.htmlInfo
            val inputType = node.inputType
            val isPwd = hint.contains("password") ||
                idEntry.contains("password") ||
                idEntry.contains("passwd") ||
                (inputType and android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
                (inputType and android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) != 0
            val isUser = hint.contains("username") ||
                hint.contains("email") ||
                idEntry.contains("user") ||
                idEntry.contains("email") ||
                idEntry.contains("account") ||
                idEntry.contains("login")
            if (isPwd && password == null) password = node.autofillId
            else if (isUser && username == null) username = node.autofillId
            html?.attributes?.forEach { pair ->
                if (pair.first.equals("type", true) && pair.second.equals("password", true)) {
                    password = password ?: node.autofillId
                }
            }
            if (domain == null) {
                val web = node.webDomain
                if (!web.isNullOrBlank()) domain = web
            }
            for (i in 0 until node.childCount) walk(node.getChildAt(i))
        }
        for (i in 0 until structure.windowNodeCount) {
            walk(structure.getWindowNodeAt(i).rootViewNode)
        }
        return Fields(username, password, domain)
    }

    companion object {
        const val EXTRA_AUTOFILL = "pwdstore_autofill"
    }
}
