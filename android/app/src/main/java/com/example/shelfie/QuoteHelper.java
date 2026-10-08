package com.example.shelfie;

import java.util.Calendar;

public class QuoteHelper {
    //picks a short reading quote based on the day of year so the home screen shows a stable quote per day
    public static String getQuoteOfDay()
    {
        String[] quotes = {
                "A reader lives a thousand lives before he dies.",
                "Books are a uniquely portable magic.",
                "Reading is dreaming with open eyes.",
                "Small reading progress is still progress.",
                "Every page counts.",
                "Today a reader, tomorrow a leader."
        };

        int index = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % quotes.length;
        return quotes[index];
    }
}
