package org.mstevens.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class TextEntryActivity extends Activity {
	    private EditText et;
	    /*
	     * (non-Javadoc)
	     * @see android.app.Activity#onCreate(android.os.Bundle)
	     */
	    @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);

	        setContentView(R.layout.activity_text_entry);
	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
	                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
	        // callback
//	        try {
//	            String s = getIntent().getExtras().getString("callback");
//	            this.callback=s;
//	        } catch (Exception e) {
//	        }
	        
	        // title
	        try {
	            String s = getIntent().getExtras().getString("title");
	            if (s.length() > 0) {
	                this.setTitle(s);
	            }
	        } catch (Exception e) {
	        }
	        // value

	        try {
	            et = ((EditText) findViewById(R.id.TextEntryText));
	            et.setText(getIntent().getExtras().getString("value"));
	        } catch (Exception e) {
	        }
	        // button
	        ((Button) findViewById(R.id.TextEntryOk)).setOnClickListener(new OnClickListener() {

	            @Override
	            public void onClick(View v) {
	                executeDone(true);
	            }
	        });
	        ((Button) findViewById(R.id.TextEntryCancel)).setOnClickListener(new OnClickListener() {

	            @Override
	            public void onClick(View v) {
	                executeDone(false);
	            }
	        });
	    }
	    @Override
	    public void onBackPressed() {
	        executeDone(false);
	        super.onBackPressed();
	    }
	    private void executeDone(boolean success) {
	        Intent resultIntent = new Intent();
	        resultIntent.putExtra("value", TextEntryActivity.this.et.getText().toString());
//	        resultIntent.putExtra("callback", this.callback);
	        if (success) {
	        	setResult(Activity.RESULT_OK, resultIntent);
	        } else {
	        	setResult(Activity.RESULT_CANCELED, resultIntent);
	        }
	        finish();
	    }
}
