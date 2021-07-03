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

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FeedingTimeAdapter extends RecyclerView.Adapter<FeedingTimeAdapter.ViewHolder> {

    private LayoutInflater inflater;
    private List<String> feedingTimes;

    FeedingTimeAdapter (Context context, List<String> feedingTimes) {
        this.feedingTimes = feedingTimes;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public FeedingTimeAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.feeding_time, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final String feedingTime = feedingTimes.get(position);
        holder.editFeedingTime.setText(feedingTime);
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
        ViewHolder (View view) {
            super(view);
            editFeedingTime = (TextView) view.findViewById(R.id.feedingTime);
        }
    }

}