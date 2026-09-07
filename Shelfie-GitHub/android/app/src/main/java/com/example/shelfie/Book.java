package com.example.shelfie;

//model for one book row returned from get_books.php

public class Book {

    int id;
    String title, author, category, status, genres;
    int totalPages, currentPage, isFavorite;
    float rating;
    int isRecommended;

    //copies fields from JSON into a Book for the RecyclerView adapter
    public Book(int id, String title, String author, String category, String status,
                int totalPages, int currentPage, int isFavorite, float rating, String genres, int isRecommended) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.status = status;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.isFavorite = isFavorite;
        this.rating = rating;
        this.genres = genres;
        this.isRecommended = isRecommended;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public String getStatus() {
        return status;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getIsFavorite() {
        return isFavorite;
    }

    public float getRating() {
        return rating;
    }

    public String getGenres() {
        return genres;
    }

    public int getIsRecommended() {
        return isRecommended;
    }
}
