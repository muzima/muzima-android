package com.muzima.adapters.forms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.muzima.R;
import com.muzima.model.SingleObsForm;
import com.muzima.model.events.CloseSingleFormEvent;
import com.muzima.utils.DateUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.Locale;

public class ClientDynamicObsFormsAdapter extends RecyclerView.Adapter<ClientDynamicObsFormsAdapter.ViewHolder> {

    private Context context;
    private List<SingleObsForm> singleObsFormList;
    private DatePickerClickedListener datePickerClickedListener;

    public ClientDynamicObsFormsAdapter(Context context, List<SingleObsForm> singleObsFormList, DatePickerClickedListener datePickerClickedListener) {
        this.context = context;
        this.singleObsFormList = singleObsFormList;
        this.datePickerClickedListener = datePickerClickedListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_single_obs_form_layout, parent, false), datePickerClickedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ClientDynamicObsFormsAdapter.ViewHolder holder, int position) {
        SingleObsForm form = singleObsFormList.get(position);
        holder.readingCountTextView.setText(String.format(Locale.getDefault(), "%s %d", context.getResources().getString(R.string.general_reading), form.getReadingCount()));
        holder.valueEditText.setHint(String.format(Locale.getDefault(), "%s %s", form.getConcept().getName(), form.getConcept().getConceptType().getName()));
        holder.dateEditText.setText(DateUtils.convertDateToStdString(form.getDate()));
    }

    @Override
    public int getItemCount() {
        return singleObsFormList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private DatePickerClickedListener datePickerClickedListener;
        private View dateSelectorView;
        private TextInputEditText dateEditText;
        private TextInputEditText valueEditText;
        private TextView readingCountTextView;
        private View closeEntryView;

        public ViewHolder(@NonNull View itemView, DatePickerClickedListener datePickerClickedListener) {
            super(itemView);
            this.dateSelectorView = itemView.findViewById(R.id.item_single_obs_form_date_selector_container);
            this.readingCountTextView = itemView.findViewById(R.id.item_single_obs_form_reading_count_view);
            this.closeEntryView = itemView.findViewById(R.id.item_single_obs_form_close_view);
            this.dateEditText = itemView.findViewById(R.id.item_single_obs_form_date_edit_text);
            this.valueEditText = itemView.findViewById(R.id.item_single_obs_form_value_edit_text);
            this.datePickerClickedListener = datePickerClickedListener;

            this.dateSelectorView.setOnClickListener(this);

            this.closeEntryView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EventBus.getDefault().post(new CloseSingleFormEvent(getAdapterPosition()));
                }
            });
        }

        @Override
        public void onClick(View view) {
            datePickerClickedListener.onDatePickerClicked(getAdapterPosition(), dateEditText);
        }
    }

    public interface DatePickerClickedListener {
        void onDatePickerClicked(int position, EditText dateEditText);
    }
}
