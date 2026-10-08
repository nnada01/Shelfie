package com.example.shelfie;

//model for one journal entry from get_journals.php

public class Journal {
    int id;
    String title, content, mood, entryDate;

    //builds a journal row for the list and edit screen
    public Journal(int id, String title, String content, String mood, String entryDate) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.mood = mood;
        this.entryDate = entryDate;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getMood() {
        return mood;
    }

    public String getEntryDate() {
        return entryDate;
    }
}