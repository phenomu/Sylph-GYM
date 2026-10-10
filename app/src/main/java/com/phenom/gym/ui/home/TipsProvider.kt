package com.phenom.gym.ui.home

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/**
 * Tips of the day — 30 fitness/motivation tips in Bahasa Indonesia.
 * Click to cycle to the next tip. Persists current index in memory.
 */
object TipsProvider {
    private val tips = listOf(
        "Konsistensi lebih penting dari intensitas. Lebih baik 20 menit setiap hari daripada 2 jam sekali seminggu.",
        "Progresif overload adalah kunci pertumbuhan otot. Tambah beban, reps, atau sets secara bertahap setiap minggu.",
        "Istirahat sama pentingnya dengan latihan. Otot tumbuh saat kamu tidur, bukan saat kamu angkat beban.",
        "Jangan pernah skip warm-up. 5-10 menit pemanasan mencegah cedera yang bisa menahan progres berminggu-minggu.",
        "Nutrisi menentukan 70% hasil gym. Kamu tidak bisa out-train a bad diet.",
        "Tidur 7-9 jam per malam adalah suplemen terbaik dan gratis untuk recovery otot.",
        "Form yang benar dengan beban ringan > form yang salah dengan beban berat. Ego lifting hanya bikin cedera.",
        "Minum 2-3 liter air sehari. Dehidrasi menurunkan performa gym sampai 20%.",
        "Protein adalah bahan baku otot. Target 1.6-2.2 gram per kg berat badan per hari.",
        "Jangan bandingkan progress kamu dengan orang lain. Bandingkan dengan diri kamu bulan lalu.",
        "Track progress kamu — berat, reps, foto, measurements. Yang terukur, tercapai.",
        "Latihan compound (squat, deadlift, bench press, pull-up) memberi hasil terbaik untuk massa otot.",
        "Mind-muscle connection: fokus rasakan otot yang bekerja, bukan sekadar gerakkan beban.",
        "Full range of motion > beban berat dengan ROM setengah. Otot tumbuh dari stretch, bukan dari ego.",
        "Deload week setiap 4-8 minggu penting untuk recovery sistem saraf dan mencegah overtraining.",
        "Karbohidrat sebelum latihan = bahan bakar. Protein setelah latihan = bahan baku otot.",
        "Jangan takut lemak sehat. Avocado, kacang, minyak zaitun penting untuk hormon testosteron.",
        "Pull day sebelum push day lebih baik untuk postur bahu jangka panjang.",
        "Core yang kuat = foundation semua gerakan. Plank, dead bug, bird dog dulu sebelum crunch.",
        "Mobility work 10 menit sehari mencegah 90% cedera yang terjadi di gym.",
        "Bukan berapa sering kamu gym, tapi berapa konsisten dalam 12 bulan. Marathon, bukan sprint.",
        "Log workout kamu. Melihat progres dari 20kg ke 60kg adalah motivasi terbaik.",
        "Stretching dinamis sebelum latihan, stretching statis setelah latihan.",
        "Jangan latihan otot yang sama 2 hari berturut. Beri waktu recovery 48 jam minimum.",
        "Suplemen hanya melengkapi. Protein whey dan creatine cukup, sisanya dari makanan asli.",
        "Sakit itu beda dengan pegal. Kalau sakit sendi, stop. Kalau pegal otot, lanjut.",
        "Gym bukan hukuman untuk makan enak. Gym adalah investasi untuk tubuh yang kuat seumur hidup.",
        "Mulai dari yang ringan dulu. Ego di gym = cedera di rumah.",
        "Bersedialah gagal di set terakhir. Kalau kamu bisa complete semua reps dengan mudah, beban kurang berat.",
        "Hari ini lebih baik dari kemarin, besok lebih baik dari hari ini. Itu satu-satunya tujuan."
    )

    private var currentIndex = (0 until tips.size).random()
    private var _tipState = mutableStateOf(tips[currentIndex])
    val currentTip get() = _tipState.value
    val tipIndex get() = currentIndex
    val totalTips get() = tips.size

    /** Get a specific tip by index. */
    fun getTip(index: Int): String = tips.getOrElse(index) { tips[0] }

    /** Advance to the next tip (wraps around). */
    fun nextTip() {
        currentIndex = (currentIndex + 1) % tips.size
        _tipState.value = tips[currentIndex]
    }
}
