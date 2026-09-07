package com.example.shelfie;

import android.net.Uri;
import android.text.SpannableStringBuilder;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

//Builds Google Books volumes URLs and formats JSON for the home screen
//Requests omit cover/image fields; the summary uses title, author, description, preview link,
//viewability, and whether the volume is listed as an e-book
 
public final class GoogleBooksApi {

    //Plain text for the quote plus an optional preview URL for an “Open preview” action
    public static final class VolumeSummary {
        public final CharSequence text;
        @Nullable public final String previewUrl;

        VolumeSummary(CharSequence text, @Nullable String previewUrl) {
            this.text = text;
            this.previewUrl = previewUrl;
        }
    }

    private GoogleBooksApi() {
    }

    //Narrow fields so we do not pull cover/image payloads—only metadata needed for the UI
    private static final String VOLUMES_LIST_FIELDS =
            "items(id,volumeInfo(title,authors,description,previewLink,infoLink),"
                    + "accessInfo(viewability),saleInfo(isEbook))";

    public static String buildVolumesUrl(String query, int maxResults) {
        return Uri.parse("https://www.googleapis.com/books/v1/volumes").buildUpon()
                .appendQueryParameter("q", query)
                .appendQueryParameter("maxResults", String.valueOf(maxResults))
                .appendQueryParameter("fields", VOLUMES_LIST_FIELDS)
                .appendQueryParameter("key", ApiConfig.GOOGLE_BOOKS_API_KEY)
                .build()
                .toString();
    }

    //return summary for the first volume, or null if there are no items
    @Nullable
    public static VolumeSummary summarizeFirstVolume(String responseJson) throws Exception {
        JSONObject root = new JSONObject(responseJson);
        if (!root.has("items")) {
            return null;
        }
        JSONArray items = root.getJSONArray("items");
        if (items.length() == 0) {
            return null;
        }
        JSONObject firstBook = items.getJSONObject(0);
        JSONObject volumeInfo = firstBook.getJSONObject("volumeInfo");

        String title = volumeInfo.optString("title", "No title");
        String author = "Unknown author";
        if (volumeInfo.has("authors")) {
            JSONArray authors = volumeInfo.getJSONArray("authors");
            if (authors.length() > 0) {
                author = authors.getString(0);
                if (authors.length() > 1) {
                    author += " et al.";
                }
            }
        }

        String description = volumeInfo.optString("description", "No description");
        if (description.length() > 500) {
            description = description.substring(0, 500) + "…";
        }

        String previewLink = volumeInfo.optString("previewLink", "");
        if (previewLink.isEmpty()) {
            previewLink = volumeInfo.optString("infoLink", "");
        }
        final String previewUrl = previewLink.isEmpty() ? null : previewLink;

        JSONObject accessInfo = firstBook.optJSONObject("accessInfo");
        String viewability = accessInfo != null ? accessInfo.optString("viewability", "") : "";

        JSONObject saleInfo = firstBook.optJSONObject("saleInfo");
        boolean isEbook = saleInfo != null && saleInfo.optBoolean("isEbook", false);

        SpannableStringBuilder out = new SpannableStringBuilder();
        out.append("Title: ").append(title).append('\n');
        out.append("Author: ").append(author).append("\n\n");
        out.append("Description:\n").append(description).append('\n');
        out.append("\nViewability: ").append(viewability.isEmpty() ? "n/a" : viewability);
        out.append("\nE-book listed: ").append(isEbook ? "yes" : "no");
        if (previewUrl == null) {
            out.append("\n\nPreview: not available for this volume.");
        }
        return new VolumeSummary(out, previewUrl);
    }
}
