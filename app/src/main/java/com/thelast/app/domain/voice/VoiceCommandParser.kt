package com.thelast.app.domain.voice

class VoiceCommandParser {
    fun parse(text: String): VoiceCommand {
        val rawText = text.trim()
        if (rawText.isBlank()) return VoiceCommand.Unknown(rawText)

        val normalized = rawText.normalizedForVoice()
        val createName = extractCreateEventName(normalized)
        if (createName != null) {
            return VoiceCommand.CreateEvent(
                eventName = createName,
                rawText = rawText,
            )
        }

        val recordName = extractKnownEventName(normalized) ?: extractRecordEventName(normalized)
        return if (recordName.isNullOrBlank()) {
            VoiceCommand.Unknown(rawText)
        } else {
            VoiceCommand.RecordEvent(
                eventName = recordName,
                note = rawText,
                rawText = rawText,
            )
        }
    }

    private fun extractCreateEventName(text: String): String? {
        val markers = listOf("新建卡片", "新增卡片", "创建卡片", "新建一个", "新增一个", "创建一个", "添加卡片", "添加一个")
        val marker = markers.firstOrNull { text.contains(it) } ?: return null
        return cleanEventName(text.substringAfter(marker))
    }

    private fun extractRecordEventName(text: String): String? {
        val knownPrefix = listOf("记录一下", "记一下", "记录", "记")
            .firstOrNull { text.contains(it) }
        val candidate = knownPrefix?.let { text.substringAfter(it) } ?: text
        return cleanEventName(candidate)
    }

    private fun extractKnownEventName(text: String): String? {
        return KnownVoiceEventAliases.firstOrNull { alias ->
            alias.keywords.any { text.contains(it) }
        }?.eventName
    }

    private fun cleanEventName(value: String): String? {
        val cleaned = value
            .replace("thelast", "")
            .replace("the last", "")
            .replace("帮我", "")
            .replace("给我", "")
            .replace("现在", "")
            .replace("刚刚", "")
            .replace("一下", "")
            .replace("一次", "")
            .replace("一个", "")
            .replace("我", "")
            .replace("卡片", "")
            .replace("事件", "")
            .replace("事项", "")
            .replace("小事", "")
            .replace("新增", "")
            .replace("新建", "")
            .replace("创建", "")
            .replace("添加", "")
            .replace("记录", "")
            .replace("记", "")
            .trim()
            .trim('，', '。', '、', ',', '.', ' ')
        return cleaned.ifBlank { null }
    }
}

private data class VoiceEventAlias(
    val eventName: String,
    val keywords: List<String>,
)

private val KnownVoiceEventAliases = listOf(
    VoiceEventAlias("喝水", listOf("喝水", "饮水", "喝了水", "喝了杯水", "喝一杯水")),
    VoiceEventAlias("加油", listOf("加油", "加了油", "油箱")),
    VoiceEventAlias("洗车", listOf("洗车", "洗了车")),
    VoiceEventAlias("剪发", listOf("剪发", "理发", "剪头发", "剪了头发")),
    VoiceEventAlias("换床单", listOf("换床单", "换了床单", "床单")),
    VoiceEventAlias("保养", listOf("保养", "做保养", "维护")),
)

private fun String.normalizedForVoice(): String {
    return lowercase()
        .replace(Regex("[\\s:：;；!！?？\"“”'‘’]"), "")
}

