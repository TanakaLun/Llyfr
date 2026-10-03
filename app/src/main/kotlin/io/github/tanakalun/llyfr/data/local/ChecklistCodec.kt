package io.github.tanakalun.llyfr.data.local

import io.github.tanakalun.llyfr.data.crypto.NoteCipher
import io.github.tanakalun.llyfr.data.model.CheckItem
import org.json.JSONArray
import org.json.JSONObject

/** checklist 列的 JSON 编解码（每条目 text 与笔记正文同体系加密）。 */
class ChecklistCodec(private val cipher: NoteCipher) {

    fun encode(items: List<CheckItem>, encryptionEnabled: Boolean): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("text", cipher.encrypt(item.text, encryptionEnabled))
                    .put("checked", item.checked),
            )
        }
        return arr.toString()
    }

    /** @return Pair(解析出的条目, 是否存在不可解密的条目) */
    fun decode(json: String): Pair<List<CheckItem>, Boolean> {
        if (json.isBlank() || json == "[]") return emptyList<CheckItem>() to false
        var anyUnreadable = false
        val result = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList<CheckItem>() to true
        val items = (0 until result.length()).mapNotNull { i ->
            val obj = result.optJSONObject(i) ?: return@mapNotNull null
            val stored = obj.optString("text", "")
            val text = if (!cipher.isEncrypted(stored)) {
                stored
            } else {
                try {
                    cipher.decrypt(stored)
                } catch (_: Exception) {
                    anyUnreadable = true
                    ""
                }
            }
            CheckItem(
                id = obj.optString("id", ""),
                text = text,
                checked = obj.optBoolean("checked", false),
            )
        }
        return items to anyUnreadable
    }

    /** 迁移用：把旧版 JSON 中的每条 text 解密后重加密为 v1。失败字段置空并标记不可读。 */
    fun migrate(json: String, cipher: NoteCipher): Pair<String, Boolean> {
        if (json.isBlank() || json == "[]") return "[]" to false
        var anyUnreadable = false
        val arr = runCatching { JSONArray(json) }.getOrNull() ?: return "[]" to true
        val out = JSONArray()
        val enabled = true
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val stored = obj.optString("text", "")
            val migrated = if (!cipher.isEncrypted(stored)) {
                try {
                    cipher.encrypt(cipher.decrypt(stored), enabled)
                } catch (_: Exception) {
                    anyUnreadable = true
                    cipher.encrypt("", enabled)
                }
            } else {
                stored
            }
            out.put(
                obj.put("text", migrated),
            )
        }
        return out.toString() to anyUnreadable
    }
}