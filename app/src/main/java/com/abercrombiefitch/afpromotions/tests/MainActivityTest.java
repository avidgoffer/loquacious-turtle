package com.abercrombiefitch.afpromotions.tests;

import android.test.ActivityInstrumentationTestCase2;

import com.abercrombiefitch.afpromotions.MainActivity;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private MainActivity _mainActivity;

    public MainActivityTest(Class<MainActivity> activityClass) {
        super(activityClass);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        _mainActivity = getActivity();
    }

    public void testPreconditions() {
        assertNotNull("_mainActivity is null", _mainActivity);
    }

    public void testDoMath() {
        assertEquals(_mainActivity.doMath(), 6); // should pass
    }


    public void testDoMath2() {
        assertEquals(_mainActivity.doMath(), 5); // should fail
    }
}