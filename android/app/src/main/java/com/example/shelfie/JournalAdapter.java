package com.example.shelfie;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

// RecyclerView adapter for journal entries with edit, delete, and export actions

public class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.JournalHolder> {

    // Declare all variables
    Context c;
    ArrayList<Journal> data;
    JournalClickListener listener;

    public interface JournalClickListener{
        void onEditClick(Journal j);
        void onDeleteClick(Journal j);
        void onExportClick(Journal j);
    }

    //keeps the journal list and listener for row interactions
    public JournalAdapter(Context c, ArrayList<Journal> data, JournalClickListener listener) {
        this.c = c;
        this.data = data;
        this.listener = listener;
    }

    public class JournalHolder extends RecyclerView.ViewHolder{
        // Declare all variables
        TextView tvTitle, tvMood, tvDate, tvContent;
        ImageButton btnEdit, btnDelete, btnExport;

        public JournalHolder(@NonNull View itemView) {
            super(itemView);

            // Reference all variables
            tvTitle = (TextView) itemView.findViewById(R.id.tvJournalTitle);
            tvMood = (TextView) itemView.findViewById(R.id.tvJournalMood);
            tvDate = (TextView) itemView.findViewById(R.id.tvJournalDate);
            tvContent = (TextView) itemView.findViewById(R.id.tvJournalContent);

            btnEdit = (ImageButton) itemView.findViewById(R.id.btnEditJournal);
            btnDelete = (ImageButton) itemView.findViewById(R.id.btnDeleteJournal);
            btnExport = (ImageButton) itemView.findViewById(R.id.btnExportJournal);
        }
    }

    // inflates item_journal into a new JournalHolder
    @NonNull
    @Override
    public JournalHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(c).inflate(R.layout.item_journal, parent, false);
        return new JournalHolder(v);
    }

    //Fills text fields for one Journal and attaches edit, delete, and export callbacks

    @Override
    public void onBindViewHolder(@NonNull JournalHolder holder, int position) {
        Journal j = data.get(position);

        holder.tvTitle.setText(j.getTitle());
        holder.tvMood.setText(j.getMood());
        holder.tvDate.setText(j.getEntryDate());
        holder.tvContent.setText(j.getContent());

        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(j));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(j));
        holder.btnExport.setOnClickListener(v -> listener.onExportClick(j));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
