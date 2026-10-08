package com.example.shelfie;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

//RecyclerView adapter: one row per Book with edit, delete, and search actions forwarded to the activity

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookHolder> {

    // Declare all variables
    Context c;
    ArrayList<Book> data;
    BookClickListener listener;

    public interface BookClickListener{
        void onEditClick(Book b);
        void onDeleteClick(Book b);
        void onWebsiteClick(Book b);
    }

    //stores the list reference and who to notify on row actions
    public BookAdapter(Context c, ArrayList<Book> data, BookClickListener listener) {
        this.c = c;
        this.data = data;
        this.listener = listener;
    }

    public class BookHolder extends RecyclerView.ViewHolder{
        // Declare all variables
        TextView tvTitle, tvAuthor, tvStatus, tvProgress;
        ImageView ivFavorite;
        ProgressBar progressBook;
        ImageButton btnEdit, btnDelete, btnWebsite;

        //finds all row views and will receive bind calls for each position
        public BookHolder(@NonNull View itemView) {
            super(itemView);

            // Reference all variables
            tvTitle = (TextView) itemView.findViewById(R.id.tvBookTitle);
            tvAuthor = (TextView) itemView.findViewById(R.id.tvBookAuthor);
            tvStatus = (TextView) itemView.findViewById(R.id.tvBookStatus);
            tvProgress = (TextView) itemView.findViewById(R.id.tvBookProgress);
            ivFavorite = (ImageView) itemView.findViewById(R.id.ivBookFavorite);
            progressBook = (ProgressBar) itemView.findViewById(R.id.progressBookItem);

            btnEdit = (ImageButton) itemView.findViewById(R.id.btnEditBook);
            btnDelete = (ImageButton) itemView.findViewById(R.id.btnDeleteBook);
            btnWebsite = (ImageButton) itemView.findViewById(R.id.btnWebsiteBook);
        }
    }

    //inflates item_book and wraps it in a BookHolder

    @NonNull
    @Override
    public BookHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(c).inflate(R.layout.item_book, parent, false);
        return new BookHolder(v);
    }

    //binds one Book to the row: text, progress bar range, favorite icon visibility, and click listeners
    @Override
    public void onBindViewHolder(@NonNull BookHolder holder, int position) {
        Book b = data.get(position);

        holder.tvTitle.setText(b.getTitle());
        holder.tvAuthor.setText(b.getAuthor());
        holder.tvStatus.setText(b.getStatus());
        holder.tvProgress.setText(
                c.getString(R.string.book_progress_fmt, b.getCurrentPage(), b.getTotalPages()));

        int total = Math.max(b.getTotalPages(), 1);
        int cur = Math.min(Math.max(b.getCurrentPage(), 0), total);
        holder.progressBook.setMax(total);
        holder.progressBook.setProgress(cur);
        holder.ivFavorite.setVisibility(b.getIsFavorite() == 1 ? View.VISIBLE : View.GONE);
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(b));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(b));
        holder.btnWebsite.setOnClickListener(v -> listener.onWebsiteClick(b));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
