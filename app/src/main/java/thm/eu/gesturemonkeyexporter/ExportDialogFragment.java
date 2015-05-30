package thm.eu.gesturemonkeyexporter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import thm.eu.gesturemonkey.GestureMonkey;

/**
* Created by Tobi on 16.01.2015.
*/
public class ExportDialogFragment extends DialogFragment {

    private EditText etFileName;
    private Button btnExport, btnCancel;
    private ProgressBar pgbExport;

    private String[] selectedGestures;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        selectedGestures = arguments.getStringArray("SelectedGestures");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_export, null);
        etFileName = (EditText)view.findViewById(R.id.etFileName);
        pgbExport = (ProgressBar)view.findViewById(R.id.pgbExport);
        btnExport = (Button)view.findViewById(R.id.btnExport);
        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!etFileName.getText().toString().equals("")){
                    etFileName.setText(etFileName.getText().toString() + ".json");
                    etFileName.setEnabled(false);
                    pgbExport.setVisibility(View.VISIBLE);

                    GestureMonkey.getInstance().exportGesturesToJSON(getActivity(), ExporterConstants.FOLDER_NAME, etFileName.getText().toString(), selectedGestures);

                    Toast.makeText(getActivity(), "Export successful", Toast.LENGTH_SHORT).show();
                    dismiss();
                } else {
                    etFileName.setHintTextColor(Color.RED);
                    etFileName.setHint("Enter filename");
                }
            }
        });
        btnCancel = (Button)view.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        builder.setView(view);
        return builder.create();
    }
}
