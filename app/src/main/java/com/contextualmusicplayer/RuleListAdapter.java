package com.contextualmusicplayer;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.contextualmusicplayer.com.contextualmusicplayer.model.Rule;

import java.util.List;

/**
 * Created by tarunchhabra on 11/13/16.
 */

public class RuleListAdapter extends ArrayAdapter<Rule>{
    private List<Rule> items;
    private int layoutResourceId;
    private Context context;

    public RuleListAdapter(Context context, int layoutResourceId, List<Rule> items){
        super(context, layoutResourceId, items);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View row = convertView;
        RuleNameHolder holder = null;

        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        row = inflater.inflate(layoutResourceId, parent, false);

        holder = new RuleNameHolder();
        holder.rule = items.get(position);
        holder.removeRuleButton = (ImageButton)row.findViewById(R.id.delete_rule);
        holder.removeRuleButton.setTag(holder.rule);
        holder.viewRuleButton = (ImageButton)row.findViewById(R.id.view_rule);
        holder.viewRuleButton.setTag(holder.rule);
        holder.name = (TextView)row.findViewById(R.id.rule_name);
        row.setTag(holder);
        setupItem(holder);
        return row;
    }

    private void setupItem(RuleNameHolder holder){
        holder.name.setText(holder.rule.getRulename());
    }

    public static class RuleNameHolder{
        Rule rule;
        TextView name;
        ImageButton removeRuleButton;
        ImageButton viewRuleButton;
    }
}
