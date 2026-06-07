package com.example.projektmobpravi.util

import java.util.Locale

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
        "ukupno eur",          // dvokolonski računi: labela u lijevom, iznos u desnom stupcu
        "grand total", "amount due", "total due"
    )

    private val totalKeywordsMid = listOf(
        "ukupno", "jkupno",  // "jkupno" = česta OCR greška za "ukupno" (J izgleda kao U)
        "total amount", "total", "svega",
        "prodaja"  // terminal kartica: "PRODAJA" prethodi naplaćenom iznosu na svim HR fiskalnim računima
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
        "konzum"           to "Konzum",
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
        // Pokušaj po prioritetu:
        //  1. HIGH keywords  — najspecifičniji labeli (za platiti, sveukupno…)
        //  2. EUR iznos      — "16.30 EUR" ili "EUR 18.93"; universalni signal
        //                      za fiskalne račune RH, radi i na dvokolonskim
        //                      računima gdje label i iznos nisu susjedni u OCR
        //  3. Gotovina math  — ukupno + vraćeno = gotovina (Spar gotovina)
        //  4. MID / LOW keywords
        //  5. Fallback: najveći iznos prije sekcije plaćanja/PDV-a
        return findAmountByKeywords(lines, totalKeywordsHigh)
            ?: extractEurAmount(lines)
            ?: extractFromCashPayment(lines)
            ?: findAmountByKeywords(lines, totalKeywordsMid)
            ?: findAmountByKeywords(lines, totalKeywordsLow)
            ?: findTotalByWideScan(lines)
    }

    /**
     * Traži iznose u formatu "X,XX EUR" ili "EUR X,XX".
     * Na fiskalnim računima RH konačni ukupni iznos uvijek ima EUR oznaku,
     * a stavke je nemaju — pa NAJVEĆI EUR iznos = ukupni iznos računa.
     *
     * Posebno korisno za dvokolonske račune (McDonald's, frizerski saloni…)
     * gdje je labela ("IN Ukupno") u lijevom stupcu a iznos ("16.30 EUR")
     * u desnom — OCR ih čita daleko jedan od drugog u tekstu.
     */
    private fun extractEurAmount(lines: List<String>): String? {
        val beforeEur = Regex("""([\d.,]+)\s+EUR\b""", RegexOption.IGNORE_CASE)
        val afterEur  = Regex("""\bEUR\s+([\d.,]+)""",  RegexOption.IGNORE_CASE)

        val amounts = mutableListOf<Double>()
        for (line in lines) {
            listOf(beforeEur, afterEur).forEach { pat ->
                pat.find(line)?.groupValues?.get(1)
                    ?.let { normalizeNumber(it)?.toDoubleOrNull() }
                    ?.let { if (it >= 0.50) amounts.add(it) }
            }
        }
        return amounts.maxOrNull()?.let { String.format(Locale.US, "%.2f", it) }
    }

    /**
     * Za gotovinska plaćanja detektira trojku (ukupno, gotovina, vraćeno) gdje
     *   ukupno + vraćeno = gotovina
     * i vraća ukupno. Ovo radi čak i kad OCR krivo pročita "UKUPNO" (npr. "JKUPNO").
     *
     * Tražimo trojku u sekciji plaćanja jer su tamo te tri vrijednosti blisko jedna
     * uz drugu u OCR tekstu.
     */
    private fun extractFromCashPayment(lines: List<String>): String? {
        val paymentStart = lines.indexOfFirst { line ->
            val lower = line.lowercase()
            listOf("gotovina", "placanje", "plaćanje").any { lower.contains(it) }
        }.takeIf { it >= 0 } ?: return null

        val amounts = lines.subList(paymentStart, lines.size)
            .mapNotNull { extractNumberFromLine(it)?.toDoubleOrNull() }
            .filter { it >= 0.01 }

        for (i in 0 until amounts.size - 2) {
            val total  = amounts[i]
            val cash   = amounts[i + 1]
            val change = amounts[i + 2]
            // Uvjet: gotovina > ukupno, vraćeno > 0, i ukupno + vraćeno = gotovina
            if (total >= 0.50 && cash > total && change > 0.0 &&
                Math.abs(total + change - cash) < 0.005) {
                return String.format(Locale.US, "%.2f", total)
            }
        }
        return null
    }

    /**
     * Za svaki match ključne riječi gleda istu liniju i sljedeće 2 linije,
     * skuplja sve pronađene iznose i vraća NAJVEĆI.
     *
     * Pretraga se zaustavlja na prvoj liniji plaćanja (gotovina, kartica…) ili
     * PDV rekapitulacije — pravi ukupni iznos uvijek dolazi PRIJE tih sekcija.
     *
     * Uspoređivanje radi i na verziji bez razmaka — zbog OCR greške "U K U P N O".
     */
    private fun findAmountByKeywords(lines: List<String>, keywords: List<String>): String? {
        val candidates = mutableListOf<Double>()
        val cutoff = findSearchCutoff(lines)

        for ((index, line) in lines.withIndex()) {
            if (cutoff != null && index >= cutoff) break
            if (!lineMatchesAnyKeyword(line, keywords)) continue

            for (offset in 0..2) {
                val candidate = lines.getOrNull(index + offset) ?: break
                // Preskoči PDV kategorizacijske retke: "5,69 D", "8,70 A" itd.
                // Oni se pojavljuju ispod "ukupno" u PDV tablici i nisu pravi ukupni iznos.
                if (isPdvCategoryLine(candidate)) continue
                val amount = extractNumberFromLine(candidate)
                if (amount != null) {
                    amount.toDoubleOrNull()?.let { candidates.add(it) }
                    break
                }
            }
        }

        return candidates.maxOrNull()?.let { String.format(Locale.US, "%.2f", it) }
    }

    /**
     * Vraća indeks prve linije koja označava sekciju plaćanja ili PDV tablice.
     * Pravi ukupni iznos uvijek se nalazi PRIJE te linije.
     *
     * Markeri moraju biti DOVOLJNO DUGI ili specifični da ne matchiraju nazive
     * proizvoda. Primjeri loših markera: "pin" → "spinat", "povrat" → "povratna
     * naknada" (ambalaža, pojavljuje se PRIJE ukupnog iznosa na računu).
     */
    private fun findSearchCutoff(lines: List<String>): Int? {
        val paymentMarkers = listOf(
            // Sekcijski headeri — najsigurniji markeri
            "plaćanje gotovinom", "placanje gotovinom",
            "plaćanje karticom",  "placanje karticom",
            "način plaćanja",     "nacin placanja",
            "ostatak novca",      // vraćeni kusur (Spar)
            // Oznake načina plaćanja — dovoljno specifične
            "gotovina",           // oznaka gotovine na računu
            "kartica",            // oznaka kartice
            "mastercard",
            "maestro",
            "amex",
            "contactless",
            "beskontaktno",
            // Poruke terminala — uvijek iza ukupnog iznosa
            "odobreno",           // odobrenje kartice
            "approved",
            "vraćeno",            // vraćeni iznos (kusur)
            "vraceno",
            "kusur",
            "plaćeno",
            "placeno"
            // IZBAČENO: "pin"    → matchira "spinat", "kupina" itd.
            // IZBAČENO: "povrat" → matchira "povratna naknada" (ambalaža, PRIJE ukupnog)
            // IZBAČENO: "visa"   → matchira "aviza", eventualno nazive proizvoda
            // IZBAČENO: "cash"   → može matchirati strane nazive proizvoda
        )
        val taxMarkers = listOf(
            "rekapitulacija",
            "pdv tablica",
            "pregled pdv",
            "porezna tablica",
            "pdv po stop",
            "oznaka pdv",
            "pdv razred",
            "porezni pregled",
            "osnovica pdv",
            "porezna osnovica",
            "osnova za pdv"
        )

        fun firstMatch(markers: List<String>) = lines.indexOfFirst { line ->
            val lower = line.lowercase()
            markers.any { lower.contains(it) }
        }.takeIf { it >= 0 }

        // Oznaka plaćanja (kartica, gotovina…) triggerira cutoff SAMO ako su
        // novčani iznosi vidljivi u neposrednoj okolini te linije (±1-2 retka).
        // Ako je oznaka sama bez iznosa (npr. "KARTICA" kao kolumni header na NTL
        // računu koji ispisuje terminal podatke PRIJE stavki), nije to pravi
        // cutoff marker — ignoriramo ga da ne odsijekamo traženje prerano.
        val rawPaymentIdx = firstMatch(paymentMarkers)
        val paymentIdx = rawPaymentIdx?.takeIf { idx ->
            val from = maxOf(0, idx - 1)
            val to   = minOf(lines.size, idx + 3)
            lines.subList(from, to).any { extractNumberFromLine(it) != null }
        }

        return listOfNotNull(paymentIdx, firstMatch(taxMarkers)).minOrNull()
    }

    private fun lineMatchesAnyKeyword(line: String, keywords: List<String>): Boolean {
        val lower        = line.lowercase()
        val lowerNoSpace = lower.replace(" ", "")
        return keywords.any { kw ->
            lower.contains(kw) || lowerNoSpace.contains(kw.replace(" ", ""))
        }
    }

    // Vraća true za retke oblika "5,69 D" ili "8,70 A" — PDV kategorizacijski iznosi
    // koji se pojavljuju u PDV tablici ispod ključnih riječi kao što je "ukupno".
    // Format: broj odmah praćen razmakom i jednim slovom PDV kategorije (A–E).
    private val pdvCategoryPattern = Regex("""^\s*[\d.,]+\s+[A-E]\s*$""")
    private fun isPdvCategoryLine(line: String) = pdvCategoryPattern.matches(line.trim())

    /**
     * Posljednji pokušaj: pretraži cijeli dokument i vrati NAJVEĆI iznos koji
     * nije u sekciji plaćanja i nije postotak ili datum.
     *
     * Matematički argument: ukupni iznos uvijek je >= svakoj pojedinoj stavci,
     * a iznos gotovine/kartice filtriramo — pa max ostatka = ukupni iznos.
     *
     * Posebno korisno za dvokolonske račune (Konzum, Spar…) gdje OCR čita cijeli
     * lijevi stupac (labele) a zatim desni stupac (iznosi), zbog čega su "UKUPNO EUR"
     * i 2,50 razdvojeni desecima linija u OCR tekstu, a cutoff koji se triggerira
     * na "PLAĆENO" prereže pretraživanje prerano.
     */
    private fun findTotalByWideScan(lines: List<String>): String? {
        val paymentTerms = listOf(
            "gotovina", "kartica", "mastercard", "maestro", "visa",
            "amex", "contactless", "beskontaktno",
            "vraćeno", "vraceno", "kusur", "ostatak",
            "plaćeno", "placeno"
        )
        val yearPattern = Regex("""\b20\d\d\b""")

        fun isNearPaymentTerm(idx: Int) = (-1..1).any { offset ->
            val i = idx + offset
            i in lines.indices && paymentTerms.any { lines[i].lowercase().contains(it) }
        }

        return lines.indices
            .filterNot { lines[it].contains('%') }               // preskoči postotke (stope PDV)
            .filterNot { yearPattern.containsMatchIn(lines[it]) } // preskoči retke s datumom/godinom
            .filterNot { isNearPaymentTerm(it) }                 // preskoči iznose uz oznaku plaćanja
            .filterNot { isPdvCategoryLine(lines[it]) }          // preskoči PDV kategorizacijske iznose
            .mapNotNull { extractNumberFromLine(lines[it])?.toDoubleOrNull() }
            .filter { it >= 0.50 }
            .maxOrNull()
            ?.let { String.format(Locale.US, "%.2f", it) }
    }

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
