package thm.eu.gesturemonkeyexporter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.io.File;
import java.util.ArrayList;

import thm.eu.gesturemonkey.GestureMonkey;

/**
 * Created by Tobi on 18.01.2015.
 */
public class ImportDialogFragment extends DialogFragment {

    private Spinner spiFiles;

    private ArrayList<String> files;

    public interface ImportDialogListener {
        public void onImportPositiveClick();
    }
    private ImportDialogListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        try{
            mListener = (ImportDialogListener)getTargetFragment();
        } catch(ClassCastException e){
            throw new ClassCastException("Calling fragment must implement interface ImportDialogListener");
        }

        File dir = new File(ExporterConstants.FOLDER_NAME + "/");
        if(dir.exists() && dir.isDirectory()){
            files = new ArrayList<String>();

            File[] filesInDir = dir.listFiles();
            for(File file : filesInDir){
                if(!file.isDirectory()){
                    files.add(file.getName());
                }
            }
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_import, null);
        spiFiles = (Spinner)view.findViewById(R.id.spiFile);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, files);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spiFiles.setAdapter(adapter);

        builder.setView(view)
                .setPositiveButton("Import", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String fileName = spiFiles.getSelectedItem().toString();
                        GestureMonkey.getInstance().importGesturesFromJSON(ExporterConstants.FOLDER_NAME, fileName);
                        mListener.onImportPositiveClick();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                });

        return builder.create();
    }
}
