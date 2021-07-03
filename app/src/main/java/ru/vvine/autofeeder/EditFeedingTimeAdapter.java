package ru.vvine.autofeeder;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EditFeedingTimeAdapter extends RecyclerView.Adapter<EditFeedingTimeAdapter.ViewHolder> {

    interface OnFeedingTimeClickListener{
        void onFeedingTimeClick(String feedingTime, int position);
        void onDeleteFeedingTimeClick(String feedingTime, int position);
    }

    private final OnFeedingTimeClickListener onClickListener;

    private LayoutInflater inflater;
    private List<String> feedingTimes;

    EditFeedingTimeAdapter (Context context, List<String> feedingTimes, OnFeedingTimeClickListener onClickListener) {
        this.onClickListener = onClickListener;
        this.feedingTimes = feedingTimes;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public EditFeedingTimeAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.edit_feeding_time, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final String feedingTime = feedingTimes.get(position);
        holder.editFeedingTime.setText(feedingTime);

        holder.itemView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                onClickListener.onFeedingTimeClick(feedingTime, position);
            }
        });

        holder.deleteFeedingTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickListener.onDeleteFeedingTimeClick(feedingTime, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return feedingTimes.size();
    }

    public List<String> getFeedingTimes() {
        return feedingTimes;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView editFeedingTime;
        final ImageButton deleteFeedingTime;
        ViewHolder (View view) {
            super(view);
            editFeedingTime = (TextView) view.findViewById(R.id.editFeedingTime);
            deleteFeedingTime = (ImageButton) view.findViewById(R.id.deleteFeedingTime);
        }
    }

}
