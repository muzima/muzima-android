package com.muzima.view.forms;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.api.model.Form;
import com.muzima.api.service.FormService;
import com.muzima.listeners.EmptyListListener;
import com.muzima.utils.Fonts;
import com.muzima.utils.StringUtils;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.List;

public class NewFormsAdapter extends FormsListAdapter<Form> {
    private static final String TAG = "NewFormsAdapter";
    private FormService formService;

    public NewFormsAdapter(Context context, int textViewResourceId, FormService formService) {
        super(context, textViewResourceId);
        this.formService = formService;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.item_forms_list, parent, false);
            holder = new ViewHolder();
            holder.name = (TextView) convertView
                    .findViewById(R.id.form_name);
            holder.description = (TextView) convertView
                    .findViewById(R.id.form_description);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Form form = getItem(position);

        holder.name.setText(form.getName());
        holder.name.setTypeface(Fonts.roboto_bold(getContext()));

        String description = form.getDescription();
        if (StringUtils.isEmpty(description)) {
            description = "No description available";
        }
        holder.description.setText(description);
        holder.name.setTypeface(Fonts.roboto_medium(getContext()));

        return convertView;
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute();
    }

    private static class ViewHolder {
        TextView name;
        TextView description;
    }

    public class BackgroundQueryTask extends AsyncTask<Void, Void, List<Form>> {

        @Override
        protected List<Form> doInBackground(Void... voids) {
            List<Form> allForms = null;
            try {
                allForms = formService.getAllForms();
                Log.i(TAG, "#Forms: " + allForms.size());
            } catch (IOException e) {
                Log.w(TAG, "Exception occurred while fetching local forms " + e);
            } catch (ParseException e) {
                Log.w(TAG, "Exception occurred while fetching local forms " + e);
            }
            return allForms;
        }

        @Override
        protected void onPostExecute(List<Form> forms) {
            NewFormsAdapter.this.clear();
            for (Form form : forms) {
                add(form);
            }
            notifyDataSetChanged();
            notifyEmptyDataListener(forms.size() == 0);
        }
    }
}
