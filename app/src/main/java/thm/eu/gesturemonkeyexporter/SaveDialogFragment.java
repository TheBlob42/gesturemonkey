package thm.eu.gesturemonkeyexporter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by Tobi on 14.01.2015.
 */
public class SaveDialogFragment extends DialogFragment {

    public interface SaveDialogListener {
        public void onDialogPositiveClick();
        public void onDialogNegativeClick();
    }

    private SaveDialogListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        try{
            mListener = (SaveDialogListener)getTargetFragment();
        } catch(ClassCastException e){
            throw new ClassCastException("Calling fragment must implement interface SaveDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage("Do you want to save the gesture?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onDialogPositiveClick();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onDialogNegativeClick();
                    }
                });

        return builder.create();
    }
}
