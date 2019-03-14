package com.junfengxiong.moto2;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btSai)
    Button btSai;
    @BindView(R.id.btBosi)
    Button btBosi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.btSai, R.id.btBosi})
    public void onViewClicked(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.btSai:
                intent = null;
//                intent = new Intent(getApplicationContext(), BlueSPPActivity.class);
                intent = new Intent(getApplicationContext(), PrimaryActivity2.class);
                startActivity(intent);
                break;
            case R.id.btBosi:
                intent = null;
                intent = new Intent(getApplicationContext(), PrimaryActivity.class);
                startActivity(intent);
                break;
        }
    }
}
