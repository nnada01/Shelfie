package com.example.shelfie;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Turns export_shelf_journal JSON into CSV for sharing or backup.
 */
public final class ExportHelper {

    private ExportHelper() {
    }

    public static String jsonToCsv(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONArray books = root.optJSONArray("books");
        JSONArray journals = root.optJSONArray("journals");
        StringBuilder sb = new StringBuilder();

        sb.append("# Shelfie export ").append(root.optString("exported_at", "")).append("\n");

        sb.append("\n## Books\n");
        sb.append("id,title,author,category,total_pages,current_page,status,is_favorite,rating,genres,is_recommended,completed_at\n");
        if (books != null) {
            for (int i = 0; i < books.length(); i++) {
                JSONObject b = books.getJSONObject(i);
                sb.append(csvEscape(b.optString("id")))
                        .append(',')
                        .append(csvEscape(b.optString("title")))
                        .append(',')
                        .append(csvEscape(b.optString("author")))
                        .append(',')
                        .append(csvEscape(b.optString("category")))
                        .append(',')
                        .append(csvEscape(b.optString("total_pages")))
                        .append(',')
                        .append(csvEscape(b.optString("current_page")))
                        .append(',')
                        .append(csvEscape(b.optString("status")))
                        .append(',')
                        .append(csvEscape(b.optString("is_favorite")))
                        .append(',')
                        .append(csvEscape(b.optString("rating")))
                        .append(',')
                        .append(csvEscape(b.optString("genres")))
                        .append(',')
                        .append(csvEscape(b.optString("is_recommended")))
                        .append(',')
                        .append(csvEscape(b.optString("completed_at")))
                        .append('\n');
            }
        }

        sb.append("\n## Journal\n");
        sb.append("id,title,content,mood,entry_date\n");
        if (journals != null) {
            for (int i = 0; i < journals.length(); i++) {
                JSONObject j = journals.getJSONObject(i);
                sb.append(csvEscape(j.optString("id")))
                        .append(',')
                        .append(csvEscape(j.optString("title")))
                        .append(',')
                        .append(csvEscape(j.optString("content")))
                        .append(',')
                        .append(csvEscape(j.optString("mood")))
                        .append(',')
                        .append(csvEscape(j.optString("entry_date")))
                        .append('\n');
            }
        }

        return sb.toString();
    }

    private static String csvEscape(String s) {
        if (s == null) {
            return "";
        }
        boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String t = s.replace("\"", "\"\"");
        if (needQuote) {
            return "\"" + t + "\"";
        }
        return t;
    }
}
