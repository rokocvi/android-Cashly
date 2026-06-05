package com.example.projektmobpravi.util

object OcrParser {

    // ── Keyword prioriteti ────────────────────────────────────────────────────
    //
    //  HIGH → specifični izrazi koji gotovo uvijek znače ukupni iznos računa
    //  MID  → opći "total" izrazi, pouzdani ali manje specifični
    //  LOW  → slabi signali, provjeravaju se samo ako HIGH/MID ne nađu ništa

    private val totalKeywordsHigh = listOf(
        "ukupno za platiti", "ukupan iznos", "sveukupno",
        "za platiti", "za naplatu",
        "za plaćanje eur", "za plaćanje",
        "in ukupno(s porezom)", "in ukupno (s porezom)",
        "iznos eur", "iznos:",
        "grand total", "amount due", "total due"
    )

    private val totalKeywordsMid = listOf(
        "ukupno", "total amount", "total", "svega"
    )

    private val totalKeywordsLow = listOf(
        "iznos", "vrijednost"
    )

    // ── Mapa trgovina ─────────────────────────────────────────────────────────
    //
    //  VAŽNO: duži (specifičniji) ključevi moraju biti ISPRED kraćih.
    //  Npr. "autoservis" mora biti prije "servis", "interspar" prije "spar".
    //  Razlog: iteriramo redom i vraćamo prvi match.

    private val storeKeywords = linkedMapOf(
        "narodni trgova"   to "NTL",
        "konzum"           to "NTL",
        "ntl"              to "NTL",
        "Konzum"           to "Konzum",
        // Supermarketi — duži ispred
        "interspar"        to "Interspar",
        "dm drogerie"      to "DM",
        "kaufland"         to "Kaufland",
        "studenac"         to "Studenac",
        "plodine"          to "Plodine",
        "trgocentar"       to "Trgocentar",
        "mercator"         to "Mercator",
        "eurospin"         to "Eurospin",
        "k-plus"           to "K-plus",
        "tommy"            to "Tommy",
        "billa"            to "Billa",
        "lidl"             to "Lidl",
        "spar"             to "Spar",
        "müller"           to "Müller",
        "muller"           to "Müller",
        " dm "             to "DM",
        // Brza hrana — duži ispred
        "mcdonald"         to "McDonald's",
        "burger king"      to "Burger King",
        "subway"           to "Subway",
        "kfc"              to "KFC",
        // Ugostiteljstvo — duži ispred
        "slastičarna"      to "Slastičarna",
        "slasticarna"      to "Slastičarna",
        "pekarnica"        to "Pekarnica",
        "pizzeria"         to "Pizzeria",
        "restoran"         to "Restoran",
        "restaurant"       to "Restoran",
        "pekara"           to "Pekara",
        "konoba"           to "Konoba",
        "bistro"           to "Bistro",
        "mesnica"          to "Mesnica",
        "pizza"            to "Pizza",
        "caffee"           to "Kafić",
        "caffe"            to "Kafić",
        "kafić"            to "Kafić",
        "kafic"            to "Kafić",
        "cafe"             to "Kafić",
        // Usluge — duži ispred
        "autoservis"       to "Autoservis",
        "auto servis"      to "Autoservis",
        "kozmetički"       to "Kozmetički salon",
        "kozmeticki"       to "Kozmetički salon",
        "frizerski"        to "Frizerski salon",
        "frizer"           to "Frizerski salon",
        "cvjećarna"        to "Cvjećarna",
        "cvjecarna"        to "Cvjećarna",
        "knjižara"         to "Knjižara",
        "knjizara"         to "Knjižara",
        "ljekarna"         to "Ljekarna",
        "pharmacy"         to "Ljekarna",
        "praonica"         to "Praonica",
        "tiskara"          to "Tiskara",
        "brijač"           to "Brijač",
        "brijac"           to "Brijač",
        "parking"          to "Parking",
        "optika"           to "Optika",
        "servis"           to "Servis",
        // Gorivo
        "benzinska"        to "Benzinska postaja",
        "tifon"            to "Tifon",
        "petrol"           to "Petrol",
        "orlen"            to "Orlen",
        // Telekomunikacije — duži ispred
        "hrvatski telekom" to "Hrvatski Telekom",
        "telemach"         to "Telemach",
        "bonbon"           to "Bonbon",
        "ht "              to "HT",
        "a1"               to "A1",
        // Sport / Kultura / Ostalo
        "teretana"         to "Teretana",
        "fitness"          to "Fitness",
        "gym"              to "Teretana",
        "cinema"           to "Kino",
        "kino"             to "Kino",
        "book"             to "Knjižara"
    )

    // ── Javna API ─────────────────────────────────────────────────────────────

    fun parse(text: String): OcrResult {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return OcrResult(
            amount    = extractAmount(lines),
            storeName = extractStoreName(lines),
            rawText   = text
        )
    }

    // ── Izvlačenje iznosa ─────────────────────────────────────────────────────

    private fun extractAmount(lines: List<String>): String? {
        // Pokušaj po prioritetu: HIGH → MID → LOW → fallback najveći na računu.
        // Za svaki tier: skupi sve iznose nađene uz keyword matcheve, vrati NAJVEĆI.
        return findAmountByKeywords(lines, totalKeywordsHigh)
            ?: findAmountByKeywords(lines, totalKeywordsMid)
            ?: findAmountByKeywords(lines, totalKeywordsLow)
            ?: fallbackLargestAmount(lines)
    }

    /**
     * Za svaki match ključne riječi gleda istu liniju i sljedeće 2 linije,
     * skuplja sve pronađene iznose i vraća NAJVEĆI.
     *
     * Npr. na NTL računu: "UKUPNO" → 18,93 ; "Total" (PDV tablica) → 1,58
     * → najveći = 18,93 ✓
     *
     * Uspoređivanje radi i na verziji bez razmaka — zbog OCR greške "U K U P N O".
     */
    private fun findAmountByKeywords(lines: List<String>, keywords: List<String>): String? {
        val candidates = mutableListOf<Double>()

        for ((index, line) in lines.withIndex()) {
            if (!lineMatchesAnyKeyword(line, keywords)) continue

            for (offset in 0..2) {
                val candidate = lines.getOrNull(index + offset) ?: break
                val amount = extractNumberFromLine(candidate)
                if (amount != null) {
                    amount.toDoubleOrNull()?.let { candidates.add(it) }
                    break
                }
            }
        }

        return candidates.maxOrNull()?.let { "%.2f".format(it) }
    }

    private fun lineMatchesAnyKeyword(line: String, keywords: List<String>): Boolean {
        val lower        = line.lowercase()
        val lowerNoSpace = lower.replace(" ", "")
        return keywords.any { kw ->
            lower.contains(kw) || lowerNoSpace.contains(kw.replace(" ", ""))
        }
    }

    /**
     * Fallback: ako niti jedna ključna riječ nije pronađena, uzimamo najveći iznos
     * na cijelom računu. Filtriramo iznose < 0.50 € da izbjegnemo sitnice (PDV,
     * vrećica, kešbek...).
     */
    private fun fallbackLargestAmount(lines: List<String>): String? =
        lines
            .mapNotNull { extractNumberFromLine(it) }
            .mapNotNull { it.toDoubleOrNull() }
            .filter { it >= 0.50 }
            .maxOrNull()
            ?.let { "%.2f".format(it) }

    // ── Parsiranje broja ─────────────────────────────────────────────────────

    /**
     * Iz jedne linije teksta izvlači zadnji broj koji izgleda kao novčani iznos
     * (uvijek s točno 2 decimale).
     *
     * Zadnji broj se uzima jer na linijama poput "2 ko X   0,79   1,58"
     * zadnji broj (1,58) je ukupni iznos stavke, a ne jedinična cijena.
     */
    private fun extractNumberFromLine(line: String): String? {
        // Matchira sve standardne novčane formate s točno 2 decimale:
        //   12,00 | 16.30 | 1.234,56 | 1,234.56
        val regex = Regex("""\d{1,3}(?:[.,]\d{3})*[.,]\d{2}""")
        val matches = regex.findAll(line).toList()
        if (matches.isEmpty()) return null
        return normalizeNumber(matches.last().value)
    }

    /**
     * Pretvara broj iz bilo kojeg formata u standardni "1234.56".
     *
     * Ključna logika — koji separator dolazi ZADNJI, taj je DECIMALNI:
     *
     *   "1.234,56"  zarez je zadnji  →  europski format  →  "1234.56"
     *   "1,234.56"  točka je zadnja  →  engleski format  →  "1234.56"
     *   "18,93"     samo zarez       →  decimalni zarez  →  "18.93"
     *   "16.30"     samo točka + 2 znamenke iza  →  decimalna točka  →  "16.30"
     *   "1.000"     samo točka + 3 znamenke iza  →  separator tisuća →  "1000"
     */
    private fun normalizeNumber(raw: String): String? {
        val lastComma = raw.lastIndexOf(',')
        val lastDot   = raw.lastIndexOf('.')

        val normalized = when {
            // Ima i zareza i točke — zadnji je decimalni separator
            lastComma >= 0 && lastDot >= 0 -> when {
                lastComma > lastDot -> raw.replace(".", "").replace(",", ".") // 1.234,56
                else                -> raw.replace(",", "")                   // 1,234.56
            }
            // Samo zarez → decimalni zarez
            lastComma >= 0 -> raw.replace(",", ".")
            // Samo točka → ovisi o broju znamenki iza
            lastDot >= 0 -> {
                val afterDot = raw.substring(lastDot + 1)
                if (afterDot.length == 3) raw.replace(".", "") // 1.000 → tisuće
                else raw                                        // 16.30 → decimala
            }
            else -> raw
        }

        // Vraćamo null ako normalizacija ne daje valjani broj (sigurnosna mreža)
        return normalized.takeIf { it.toDoubleOrNull() != null }
    }

    // ── Izvlačenje naziva trgovine ────────────────────────────────────────────

    /**
     * Naziv trgovine je gotovo uvijek u prvih nekoliko linija (zaglavlje računa).
     * Pretraživanje radi u 2 prolaza: prvih 6 linija, pa ostatak.
     */
    private fun extractStoreName(lines: List<String>): String? =
        searchStoreInLines(lines.take(6))
            ?: searchStoreInLines(lines.drop(6))

    private fun searchStoreInLines(lines: List<String>): String? {
        for (line in lines) {
            val lower = line.lowercase()
            for ((keyword, storeName) in storeKeywords) {
                if (lower.contains(keyword)) return storeName
            }
        }
        return null
    }
}

// ── Result model ──────────────────────────────────────────────────────────────

data class OcrResult(
    val amount: String?,
    val storeName: String?,
    val rawText: String
)
