package com.contextualmusicplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.contextualmusicplayer.com.contextualmusicplayer.model.Rule;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ManageRuleActivity extends AppCompatActivity {

    private SharedPreferences mPrefs = null;
    public static final String PREFS_NAME = "MyRules";
    private List<Rule> ruleList = null;
    private RuleListAdapter adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_rule);
        mPrefs = getSharedPreferences(PREFS_NAME,MODE_PRIVATE);
        String rulesJson = mPrefs.getString("rules",null);
        if (rulesJson != null) {
            Gson gson = new Gson();
            Type collectionType = new TypeToken<Collection<Rule>>() {
            }.getType();
            Collection<Rule> rules = gson.fromJson(rulesJson, collectionType);
            ruleList = new ArrayList<Rule>(rules);
            adapter = new RuleListAdapter(this, R.layout.rule_list,ruleList);
            ListView ruleListView = (ListView)findViewById(R.id.ruleNameList);
            ruleListView.setAdapter(adapter);
        }
    }
    public void removeRuleOnClickHandler(View v){
        Rule toBedeleted = (Rule)v.getTag();
        if (ruleList != null && adapter != null){
            adapter.remove(toBedeleted);
            ruleList.remove(toBedeleted);
            Gson gson = new Gson();
            String json = gson.toJson(ruleList);
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString("rules",json);
            editor.commit();
        }
    }

    public void viewRuleOnClickHandler(View v){
        Rule ruleToBeViewed = (Rule)v.getTag();
        Gson gson = new Gson();
        String json = gson.toJson(ruleToBeViewed);
        Intent intent = new Intent(this, ViewRule.class);
        intent.putExtra("RULE_JSON",json);
        startActivity(intent);
    }
}
