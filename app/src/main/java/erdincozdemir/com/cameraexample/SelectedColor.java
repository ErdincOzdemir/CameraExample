package erdincozdemir.com.cameraexample;

import android.content.Context;
import android.text.Editable;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by erdinc.ozdemir on 02.10.2015.
 */
public class SelectedColor extends LinearLayout {

    private Context context;
    private LinearLayout color;
    private TextView colorValue;

    public SelectedColor(Context context) {
        super(context);

        this.context = context;

        inflate(context, R.layout.selected_color, this);

        this.color = (LinearLayout) findViewById(R.id.color);
        this.colorValue = (TextView) findViewById(R.id.colorValue);
    }

    public void setColor(int color) {
        if(this.color != null) {
            this.color.setBackgroundColor(Integer.parseInt(String.format("%06X", (0xFFFFFF & color)), 16) + 0xFF000000);
        }
    }

    public void setColorValue(String colorValue) {
        if(this.colorValue != null) {
            this.colorValue.setText("You have selected " + colorValue);
        }
    }
}
