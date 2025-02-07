import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

fun main() {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://streamcloud.my/streamcloud/")
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val document = Jsoup.parse(response.body?.string())
        val movies = document.select("div.movie-item")

        for (movie in movies) {
            val title = movie.select("h2.title").text()
            val description = movie.select("p.description").text()
            val imageUrl = movie.select("img.poster").attr("src")
            val detailPageUrl = movie.select("a").attr("href")

            // Detailseite abrufen, um den Videolink zu extrahieren
            val detailRequest = Request.Builder()
                .url(detailPageUrl)
                .build()

            client.newCall(detailRequest).execute().use { detailResponse ->
                if (!detailResponse.isSuccessful) throw IOException("Unexpected code $detailResponse")

                val detailDocument = Jsoup.parse(detailResponse.body?.string())
                val videoUrl = detailDocument.select("a.download-button").attr("href")

                // Daten speichern oder weiterverarbeiten
                println("Titel: $title")
                println("Beschreibung: $description")
                println("Bild-URL: $imageUrl")
                println("Video-URL: $videoUrl")
            }
        }
    }
}
