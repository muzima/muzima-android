/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.forms;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
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
    private DateValuePickerClickedListener dateValuePickerClickedListener;

    public ClientDynamicObsFormsAdapter(Context context, List<SingleObsForm> singleObsFormList, DatePickerClickedListener datePickerClickedListener, DateValuePickerClickedListener dateValuePickerClickedListener) {
        this.context = context;
        this.singleObsFormList = singleObsFormList;
        this.datePickerClickedListener = datePickerClickedListener;
        this.dateValuePickerClickedListener = dateValuePickerClickedListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_single_obs_form_layout, parent, false), datePickerClickedListener, dateValuePickerClickedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull final ClientDynamicObsFormsAdapter.ViewHolder holder, int position) {
        final SingleObsForm form = singleObsFormList.get(position);
        holder.readingCountTextView.setText(String.format(Locale.getDefault(), "%s %d", context.getResources().getString(R.string.general_reading), form.getReadingCount()));
        holder.valueEditText.setHint(String.format(Locale.getDefault(), "%s %s", form.getConcept().getName(), form.getConcept().getConceptType().getName()));
        if(form.getConcept().isNumeric()) {
            holder.valueEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
            holder.valueEditText.setVisibility(View.VISIBLE);
            holder.valueDateText.setVisibility(View.GONE);
        }else if(form.getConcept().isDatetime()){
            holder.valueEditText.setVisibility(View.GONE);
            holder.valueDateText.setVisibility(View.VISIBLE);
            holder.valueDateText.setHint(String.format(Locale.getDefault(), "%s %s", form.getConcept().getName(), form.getConcept().getConceptType().getName()));
            holder.valueDateText.setText(form.getInputDateValue());
            holder.valueEditText.setText(form.getInputDateValue());
        } else{
            holder.valueEditText.setInputType(InputType.TYPE_CLASS_TEXT);
            holder.valueEditText.setVisibility(View.VISIBLE);
            holder.valueDateText.setVisibility(View.GONE);
        }
        holder.valueEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                form.setInputValue(text.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        holder.dateEditText.setText(DateUtils.convertDateToStdString(form.getDate()));
    }

    @Override
    public int getItemCount() {
        return singleObsFormList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private DatePickerClickedListener datePickerClickedListener;
        private View dateSelectorView;
        private TextInputEditText dateEditText;
        private TextInputEditText valueEditText;
        private TextView readingCountTextView;
        private View closeEntryView;
        private MaterialTextView valueDateText;
        private DateValuePickerClickedListener dateValuePickerClickedListener;

        public ViewHolder(@NonNull View itemView, DatePickerClickedListener datePickerClickedListener, DateValuePickerClickedListener dateValuePickerClickedListener) {
            super(itemView);
            this.dateSelectorView = itemView.findViewById(R.id.item_single_obs_form_date_selector_container);
            this.valueDateText = itemView.findViewById(R.id.item_single_obs_form_value_date_text);
            this.readingCountTextView = itemView.findViewById(R.id.item_single_obs_form_reading_count_view);
            this.closeEntryView = itemView.findViewById(R.id.item_single_obs_form_close_view);
            this.dateEditText = itemView.findViewById(R.id.item_single_obs_form_date_edit_text);
            this.valueEditText = itemView.findViewById(R.id.item_single_obs_form_value_edit_text);
            this.datePickerClickedListener = datePickerClickedListener;
            this.dateValuePickerClickedListener = dateValuePickerClickedListener;
            this.dateSelectorView.setOnClickListener(this);

            this.closeEntryView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EventBus.getDefault().post(new CloseSingleFormEvent(getAdapterPosition()));
                }
            });

            this.valueDateText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dateValuePickerClickedListener.onDateValuePickerClicked(getAdapterPosition(), valueDateText);
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

    public interface DateValuePickerClickedListener{
        void onDateValuePickerClicked(int position, MaterialTextView dateEditText);
    }
}
