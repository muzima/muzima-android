package com.muzima.view;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.db.Html5FormDataSource;
import com.muzima.domain.Html5Form;
import com.muzima.utils.Fonts;

import java.util.List;

import static com.muzima.db.Html5FormDataSource.DataChangeListener;

public class Html5FormsAdapter extends ArrayAdapter<Html5Form> implements DataChangeListener {

    private Html5FormDataSource html5FormDataSource;

    public Html5FormsAdapter(Context context, int textViewResourceId, Html5FormDataSource html5FormDataSource) {
        super(context, textViewResourceId);
        this.html5FormDataSource = html5FormDataSource;
        html5FormDataSource.setDataChangeListener(this);
        new BackgroundQueryTask().execute();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.form_list_item, parent, false);
            holder = new ViewHolder();
            holder.name = (TextView) convertView
                    .findViewById(R.id.form_name);
            holder.description = (TextView) convertView
                    .findViewById(R.id.form_description);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Html5Form form = getItem(position);

        holder.name.setText(form.getName());
        holder.name.setTypeface(Fonts.roboto_bold(getContext()));

        String description = form.getDescription();
        if(description.equals("")){
            description = "No description available";
        }
        holder.description.setText(description);
        holder.name.setTypeface(Fonts.roboto_medium(getContext()));

        return convertView;
    }

    @Override
    public void onInsert() {
        new BackgroundQueryTask().execute();
    }

    private static class ViewHolder {
        TextView name;
        TextView description;
    }

    public class BackgroundQueryTask extends AsyncTask<Void, Void, List<Html5Form>> {

        @Override
        protected List<Html5Form> doInBackground(Void... voids) {
            List<Html5Form> allForms = html5FormDataSource.getAllForms();
            return allForms;
        }

        @Override
        protected void onPostExecute(List<Html5Form> html5Forms) {
            clear();
            for (Html5Form html5Form : html5Forms) {
                add(html5Form);
            }
            notifyDataSetChanged();
        }
    }
}
