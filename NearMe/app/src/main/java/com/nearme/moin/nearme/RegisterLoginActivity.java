package com.nearme.moin.nearme;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class RegisterLoginActivity extends ActionBarActivity implements OnRegisterLoginButtonListener {

    private FragmentManager fragmentManager;
    private FragmentTransaction transaction;
    private Fragment fragment;

    // Local storage user info db instance
    private UserDbAdapter mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDbHelper = new UserDbAdapter(this);
        mDbHelper.open();

        Cursor c = mDbHelper.fetchAllUsers();
        if(c.getCount() > 0)
        {
            c.moveToFirst();
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra("ID", c.getInt(1));
            this.finish();
            startActivity(i);
        }
        mDbHelper.close();

        setContentView(R.layout.activity_launch);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new RegisterLoginFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_launch, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onButtonClicked(int type) {
        fragmentManager = getFragmentManager();
        transaction = fragmentManager.beginTransaction();
        fragment = fragmentManager.findFragmentById(R.id.container);

        if(type == 1)
        {
            if(fragment == null)
            {
                transaction.add(R.id.container, new LoginFragment(), "loginFragment");
            }else {
                transaction.replace(R.id.container, new LoginFragment(), "loginFragment");
            }
        }
        else
        {
            if(fragment == null)
            {
                transaction.add(R.id.container, new RegisterFragment(), "registerFragment");
            }else {
                transaction.replace(R.id.container, new RegisterFragment(), "registerFragment");
            }
        }

        transaction.addToBackStack(null);
        transaction.commit();
    }
}
