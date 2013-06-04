package com.muzima;

import android.widget.TextView;


import com.muzima.testSupport.CustomTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(CustomTestRunner.class)
public class HomeActivityTest {
    private HomeActivity activity;
    private TextView helloWorldTextView;

    @Before
    public void setUp() throws Exception {
        activity = new HomeActivity();
        activity.onCreate(null);
        helloWorldTextView = (TextView) activity.findViewById(R.id.hello);
    }

    @Test
    public void shouldHaveATextViewThatSaysHelloWorld() throws Exception {
        assertThat((String) helloWorldTextView.getText(), equalTo("Hello World!"));
    }
}
