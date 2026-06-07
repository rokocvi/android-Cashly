package com.example.projektmobpravi.util

import com.example.projektmobpravi.domain.model.Category
import com.example.projektmobpravi.domain.model.CustomCategory

data class VoiceParseResult(
    val amount: Double,
    val categoryName: String,
    val categoryEmoji: String,
    val rawText: String
)

object VoiceParser {

    private val currencyWords = setOf(
        "eura", "euro", "eur", "€", "kuna", "kune", "kn", "hrk",
        "dolara", "dolar", "dollar", "dollars", "usd", "$",
        "funta", "funte", "pound", "pounds", "gbp", "£",
        "franka", "frank", "francs", "chf"
    )

    private val ones = mapOf(
        "nula" to 0, "zero" to 0,
        "jedan" to 1, "jedna" to 1, "jednu" to 1, "one" to 1,
        "dva" to 2, "dvije" to 2, "two" to 2,
        "tri" to 3, "three" to 3,
        "četiri" to 4, "four" to 4,
        "pet" to 5, "five" to 5,
        "šest" to 6, "six" to 6,
        "sedam" to 7, "seven" to 7,
        "osam" to 8, "eight" to 8,
        "devet" to 9, "nine" to 9
    )

    private val teens = mapOf(
        "deset" to 10, "ten" to 10,
        "jedanaest" to 11, "eleven" to 11,
        "dvanaest" to 12, "twelve" to 12,
        "trinaest" to 13, "thirteen" to 13,
        "četrnaest" to 14, "fourteen" to 14,
        "petnaest" to 15, "fifteen" to 15,
        "šesnaest" to 16, "sixteen" to 16,
        "sedamnaest" to 17, "seventeen" to 17,
        "osamnaest" to 18, "eighteen" to 18,
        "devetnaest" to 19, "nineteen" to 19
    )

    private val tensMap = mapOf(
        "dvadeset" to 20, "twenty" to 20,
        "trideset" to 30, "thirty" to 30,
        "četrdeset" to 40, "forty" to 40,
        "pedeset" to 50, "fifty" to 50,
        "šezdeset" to 60, "sixty" to 60,
        "sedamdeset" to 70, "seventy" to 70,
        "osamdeset" to 80, "eighty" to 80,
        "devedeset" to 90, "ninety" to 90
    )

    private val hundredsMap = mapOf(
        "sto" to 100, "hundred" to 100,
        "dvjesto" to 200, "dvesto" to 200,
        "tristo" to 300,
        "četiristo" to 400,
        "petsto" to 500,
        "šesto" to 600,
        "sedamsto" to 700,
        "osamsto" to 800,
        "devetsto" to 900
    )

    private val multipliers = mapOf(
        "tisuću" to 1000, "tisuća" to 1000, "tisuće" to 1000,
        "thousand" to 1000
    )

    private val categoryKeywords: Map<String, Pair<String, String>> = buildMap {
        listOf("hrana","hranu","hrani","jelo","jelu","jela","restoran","kafić","kafic","ručak","rucak","večera","vecera")
            .forEach { put(it, "Hrana" to "🍔") }
        listOf("food","groceries","grocery","restaurant","eating","lunch","dinner","breakfast","cafe","coffee","snack")
            .forEach { put(it, "Hrana" to "🍔") }

        listOf("prijevoz","prijevoza","prijevozu","benzin","gorivo","autobus","tramvaj","vlak","taksi","taxi")
            .forEach { put(it, "Prijevoz" to "🚗") }
        listOf("transport","transportation","travel","gas","fuel","bus","train","car","uber","lyft","metro","parking")
            .forEach { put(it, "Prijevoz" to "🚗") }

        listOf("zabava","zabave","zabavi","kino","film","koncert","igre","izlazak","noćni","nocni")
            .forEach { put(it, "Zabava" to "🎬") }
        listOf("entertainment","fun","cinema","movie","movies","concert","gaming","games","game","bar","club")
            .forEach { put(it, "Zabava" to "🎬") }

        listOf("kuća","kuca","kuci","kući","dom","doma","stan","stana","najam","rezija","struja","voda")
            .forEach { put(it, "Kuća" to "🏠") }
        listOf("house","home","housing","rent","utilities","electricity","water","internet","mortgage")
            .forEach { put(it, "Kuća" to "🏠") }

        listOf("zdravlje","zdravlju","lijek","lijekovi","doktor","liječnik","ljekarna","bolnica","teretana","trening")
            .forEach { put(it, "Zdravlje" to "💊") }
        listOf("health","medicine","healthcare","doctor","pharmacy","hospital","gym","fitness","workout","dentist")
            .forEach { put(it, "Zdravlje" to "💊") }

        listOf("odijevanje","odjevanje","odijelo","odjeća","odjeca","cipele","majica","hlače","hlace")
            .forEach { put(it, "Odijevanje" to "👕") }
        listOf("clothing","clothes","fashion","shopping","shoes","shirt","dress","jacket","pants","outfit")
            .forEach { put(it, "Odijevanje" to "👕") }

        listOf("ostalo","ostali","ostalom","razno")
            .forEach { put(it, "Ostalo" to "📦") }
        listOf("other","misc","miscellaneous","various","stuff")
            .forEach { put(it, "Ostalo" to "📦") }
    }

    fun parse(rawText: String, customCategories: List<CustomCategory>): VoiceParseResult? {
        val normalized = rawText.lowercase().trim()
        val amount = parseAmount(normalized) ?: return null
        val (catName, catEmoji) = parseCategory(normalized, customCategories)
        return VoiceParseResult(
            amount        = amount,
            categoryName  = catName,
            categoryEmoji = catEmoji,
            rawText       = rawText
        )
    }

    private fun parseAmount(text: String): Double? {
        // Thousands separator — exactly 3 digits after . or ,
        // HR: "2.000" or "1.500,50"  |  EN: "2,000" or "1,500.50"
        Regex("""(\d{1,3}(?:\.\d{3})+)(?:,(\d{1,2}))?""").find(text)?.let { m ->
            val int = m.groupValues[1].replace(".", "")
            val dec = m.groupValues[2]
            return if (dec.isEmpty()) int.toDoubleOrNull() else "$int.$dec".toDoubleOrNull()
        }
        Regex("""(\d{1,3}(?:,\d{3})+)(?:\.(\d{1,2}))?""").find(text)?.let { m ->
            val int = m.groupValues[1].replace(",", "")
            val dec = m.groupValues[2]
            return if (dec.isEmpty()) int.toDoubleOrNull() else "$int.$dec".toDoubleOrNull()
        }
        // Decimal — 1 or 2 digits after separator
        Regex("""(\d+)[.,](\d{1,2})""").find(text)?.let { m ->
            return m.value.replace(",", ".").toDoubleOrNull()
        }
        // Plain integer
        Regex("""\d+""").find(text)?.let { m ->
            return m.value.toDoubleOrNull()
        }
        // Word numbers
        return parseWordNumber(text)
    }

    private fun parseWordNumber(text: String): Double? {
        val tokens = text.split(Regex("""\s+|[,.]"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() && it !in setOf("i", "and", "a") && it !in currencyWords }

        if (tokens.isEmpty()) return null

        // total  — running sum of completed groups (e.g. after "thousand")
        // current — accumulator for the current group
        // curTens — pending tens value within current group
        var total   = 0
        var current = 0
        var curTens = 0

        for (token in tokens) {
            when {
                token == "hundred" -> {
                    // EN: "two hundred" → multiply preceding sub-group by 100
                    val sub = curTens + current
                    current = (if (sub == 0) 1 else sub) * 100
                    curTens = 0
                }
                token == "sto" -> {
                    // HR: "sto" = standalone 100, not a multiplier
                    current += curTens + 100
                    curTens  = 0
                }
                hundredsMap.containsKey(token) -> {
                    // HR compound hundreds: dvjesto=200, tristo=300, etc.
                    current += curTens + (hundredsMap[token] ?: 0)
                    curTens  = 0
                }
                multipliers.containsKey(token) -> {
                    // "tisuću"/"thousand" — flush current group into total
                    val sub  = current + curTens
                    total   += (if (sub == 0) 1 else sub) * (multipliers[token] ?: 1)
                    current  = 0
                    curTens  = 0
                }
                teens.containsKey(token) -> {
                    current += curTens + (teens[token] ?: 0)
                    curTens  = 0
                }
                tensMap.containsKey(token) -> {
                    current += curTens
                    curTens  = tensMap[token] ?: 0
                }
                ones.containsKey(token) -> {
                    current += curTens + (ones[token] ?: 0)
                    curTens  = 0
                }
            }
        }
        total += current + curTens
        return if (total > 0) total.toDouble() else null
    }

    private fun parseCategory(text: String, customCategories: List<CustomCategory>): Pair<String, String> {
        val tokens = text.split(Regex("""\s+|[,.]"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        for (token in tokens) {
            categoryKeywords[token]?.let { return it }
        }
        for (token in tokens) {
            customCategories.firstOrNull { it.name.lowercase() == token }
                ?.let { return it.name to it.emoji }
        }
        for (token in tokens) {
            customCategories.firstOrNull {
                token.contains(it.name.lowercase()) || it.name.lowercase().contains(token)
            }?.let { return it.name to it.emoji }
        }

        return Category.OSTALO.displayName to Category.OSTALO.emoji
    }
}
