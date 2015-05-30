package thm.eu.gesturemonkeyexporter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;

import thm.eu.gesturemonkey.Gesture;

/**
 * Created by Tobi on 13.01.2015.
 */
public class GestureListAdapter extends ArrayAdapter<Gesture> {
    private Activity context;
    private Gesture[] gestures;
    //keeps track which CheckBox is in which state (important due to the view recycling)
    private boolean[] selectionList;

    static class ViewHolder {
        public TextView tvCaption;
        public CheckBox chkEdit;
    }

    public GestureListAdapter(Activity context, Gesture[] gestures){
        super(context, R.layout.row_gesturelist, gestures);
        this.context = context;
        this.gestures = gestures;

        selectionList = new boolean[gestures.length];
        for(int i=0; i < selectionList.length; i++){
            selectionList[i] = false;
        }
    }

    public void selectAll(){
        for(int i=0; i < selectionList.length; i++){
            selectionList[i] = true;
        }
    }

    public ArrayList<String> getAllGestureNames(){
        ArrayList<String> gestureNames = new ArrayList<String>();

        for(Gesture g : gestures){
            gestureNames.add(g.name);
        }

        return gestureNames;
    }

    @Override
    public int getCount(){
        return gestures.length;
    }

    @Override
    public long getItemId(int position){
        return position;
    }

    @Override
    public Gesture getItem(int position){
        return gestures[position];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View view = convertView;

        ViewHolder viewHolder;

        if(view == null){
            LayoutInflater inflater = context.getLayoutInflater();
            view = inflater.inflate(R.layout.row_gesturelist, null);

            viewHolder = new ViewHolder();
            viewHolder.tvCaption = (TextView)view.findViewById(R.id.tvGestureTitle);
            viewHolder.chkEdit = (CheckBox)view.findViewById(R.id.chkGesture);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)view.getTag();
        }

        viewHolder.tvCaption.setText(gestures[position].name);

        viewHolder.chkEdit.setTag(position);
        viewHolder.chkEdit.setChecked(selectionList[position]);
        viewHolder.chkEdit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int getPosition = (Integer)buttonView.getTag();//get the position of the related item
                selectionList[getPosition] = buttonView.isChecked();//set the value of the checkbox to maintain its state
            }
        });

        return view;
    }
}
