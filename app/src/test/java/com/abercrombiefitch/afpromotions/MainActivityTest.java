package com.abercrombiefitch.afpromotions;

import android.test.ActivityInstrumentationTestCase2;

import org.junit.Test;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private MainActivity _mainActivity;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        _mainActivity = getActivity();
    }

    @Test
    public void testPreconditions() {
        assertNotNull("_mainActivity is null", _mainActivity);
    }

    @Test
    public void testDoMath() {
        assertEquals(_mainActivity.doMath(), 5); // should fail
        assertEquals(_mainActivity.doMath(), 6); // should pass
    }
}